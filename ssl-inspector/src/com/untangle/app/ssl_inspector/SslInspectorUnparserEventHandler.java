/**
 * $Id: SslInspectorUnparserEventHandler.java,v 1.00 2017/03/03 19:29:18 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */

package com.untangle.app.ssl_inspector;

import java.nio.ByteBuffer;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;

import org.apache.log4j.Logger;

/**
 * The unparser handles converting plaint-text SSL traffic back to SSL traffic,
 * and sending the encrypted data to the client or server as required. During
 * the SSL handshake, our casing needs to pass raw SSL traffic back and forth
 * with both the client and server, and the parser is the best place to handle
 * that which means less work to do here. For the server side, we only need to
 * do the initial wrap to start the handshake. On the client side, we have to
 * unwrap the original message, and then handle enough wrap and task calls until
 * we have something to return to the client, after which the parser will finish
 * the handshake.
 */
public class SslInspectorUnparserEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());

    private final SslInspectorApp app;
    private final boolean clientSide;

    /**
     * Constructor
     * 
     * @param clientSide
     *        True for client side, false for server
     * @param app
     *        The application that created us
     */
    protected SslInspectorUnparserEventHandler(boolean clientSide, SslInspectorApp app)
    {
        this.clientSide = clientSide;
        this.app = app;
    }

    /**
     * Main handler for data receive from the client
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPClientChunk(AppTCPSession session, ByteBuffer data)
    {
        if (clientSide) {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamUnparse(session, data, false);
        }
    }

    /**
     * Main handler for data received from the server
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPServerChunk(AppTCPSession session, ByteBuffer data)
    {
        if (clientSide) {
            streamUnparse(session, data, true);
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        }
    }

    /**
     * Handler for end of client data
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPClientDataEnd(AppTCPSession session, ByteBuffer data)
    {
        if (clientSide) {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamUnparse(session, data, false);
        }
    }

    /**
     * Handler for end of server data
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     */
    @Override
    public void handleTCPServerDataEnd(AppTCPSession session, ByteBuffer data)
    {
        if (clientSide) {
            streamUnparse(session, data, true);
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        }
    }

    /**
     * This is the handler for all data received.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @param s2c
     *        True for server to client, false for client to server
     */
    private void streamUnparse(AppTCPSession session, ByteBuffer data, boolean s2c)
    {
        SslInspectorManager manager = getManager(session);
        boolean tlsFlag = (s2c ? manager.tlsFlagServer : manager.tlsFlagClient);

        // special handling for SMTP stream in plain text mode
        if ((session.getServerPort() == 25) && (tlsFlag == false)) {

            if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_RELEASE_MESSAGE) == true) {
                logger.debug("Received IPC_RELEASE message");
                session.release();
                return;
            }

            if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_DESTROY_MESSAGE) == true) {
                logger.debug("Received IPC_DESTROY message");
                session.killSession();
                return;
            }

            logger.debug("---------- " + (s2c ? "CLIENT" : "SERVER") + " unparse() received " + data.limit() + " bytes ----------");

            if (s2c == true) {
                session.sendDataToClient(data);
                if (manager.tlsFlagClient == true) {
                    if (manager.checkTlsServer(data) == true) {
                        logger.debug("UNPARSER SETTING SERVER TLS FLAG");
                        manager.tlsFlagServer = true;
                    } else {
                        logger.debug("UNPARSER RELEASING TLS LOGIC FAILURE");
                        manager.tlsFlagClient = manager.tlsFlagServer = false;
                        shutdownOtherSide(session, false);
                        session.release();
                    }
                }
            }

            if (s2c == false) {
                session.sendDataToServer(data);
                if (manager.checkTlsClient(data) == true) {
                    logger.debug("UNPARSER SETTING CLIENT TLS FLAG");
                    manager.tlsFlagClient = true;
                }
            }
            return;
        }

        try {
            unparse(session, data, manager);
        } catch (Exception exn) {
            logger.warn("Error during streamUnparse()", exn);
        }
    }

    /**
     * This is the main data unparser
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @param manager
     *        The SSL manager
     */
    public void unparse(AppTCPSession session, ByteBuffer data, SslInspectorManager manager)
    {
        String sslProblem = null;
        String logDetail = null;
        boolean success = false;

        logger.debug("---------- " + (manager.getClientSide() ? "CLIENT" : "SERVER") + " unparse() received " + data.limit() + " bytes ----------");

        // empty buffer indicates the session is terminating
        if (data.limit() == 0) return;

        try {
            // pass the data to the unparse worker function
            success = unparseWorker(session, data);
        }

        catch (SSLException ssl) {
            sslProblem = ssl.getMessage();
            if (sslProblem == null) sslProblem = "Unknown SSL exception";

            // for normal close we kill both sides and return  
            if (sslProblem.contains("close_notify")) {
                shutdownOtherSide(session, true);
                session.killSession();
                return;
            }
        }

        catch (Exception exn) {
            logger.debug("Exception calling unparseWorker", exn);
        }

        // no success result means something went haywire so we kill the session 
        if (success == false) {
            // put something in the event log starting with any ssl message we extracted above
            if (sslProblem != null) {
                logDetail = ((clientSide ? "Client" : "Server") + " SSL encrypt exception: " + sslProblem);
                if (manager.getPeerThumbprint() != null) logDetail += (" CERT: " + manager.getPeerThumbprint());
            }
            if (logDetail == null) logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            if (logDetail == null) logDetail = session.getServerAddr().getHostAddress();
            SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_ABANDONED, logDetail);
            app.logEvent(logevt);
            app.incrementMetric(SslInspectorApp.STAT_ABANDONED);

            // only log a warning if we didn't get an exception message for the event log            
            if (sslProblem == null) logger.warn("Session abandon on unparseWorker false return for " + logDetail);

            // kill the session on the other side and kill our session
            shutdownOtherSide(session, true);
            session.killSession();
        }

        return;
    }

    /**
     * This function is called to shut down the other side of the casing
     * 
     * @param session
     *        The TCP session
     * @param killSession
     *        True to kill session, false to release
     */
    private void shutdownOtherSide(AppTCPSession session, boolean killSession)
    {
        ByteBuffer message = ByteBuffer.allocate(256);

        if (killSession == true) message.put(SslInspectorManager.IPC_DESTROY_MESSAGE);
        else message.put(SslInspectorManager.IPC_RELEASE_MESSAGE);

        message.flip();

        SslInspectorManager manager = getManager(session);
        if (manager.getClientSide()) {
            SslInspectorManager server = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
            server.getSession().simulateClientData(message);
        } else {
            SslInspectorManager client = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
            client.getSession().simulateServerData(message);
        }
    }

    /**
     * This is the main unparse worker where we sit in a loop passing data to
     * the SSLEngine, reading the handshake status, and doing whatever it tells
     * us to do until there is nothing left to be done.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return True if everything is fine, false for any error or failure
     * @throws Exception
     */
    private boolean unparseWorker(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        ByteBuffer target = ByteBuffer.allocate(32768);
        boolean done = false;
        HandshakeStatus status;

        // if not in dataMode yet look for our special message that
        // lets us know when it's time to initialize our SSLEngine
        if ((manager.getSSLEngine() == null) && (manager.getDataMode() == false)) {
            if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_WAKEUP_MESSAGE) == true) {
                logger.debug("Received IPC_WAKEUP message");
                manager.initializeEngine();
            }
        }

        if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_RELEASE_MESSAGE) == true) {
            logger.debug("Received IPC_RELEASE message");
            session.release();
            return true;
        }

        if (manager.checkIPCMessage(data.array(), SslInspectorManager.IPC_DESTROY_MESSAGE) == true) {
            logger.debug("Received IPC_DESTROY message");
            session.killSession();
            return true;
        }

        logger.debug("CASING_BUFFER = " + manager.getCasingBuffer().toString());
        logger.debug("PARAM_BUFFER = " + data.toString());
        logger.debug("DATA_MODE = " + manager.getDataMode());

        while (done == false) {
            status = manager.getSSLEngine().getHandshakeStatus();
            logger.debug("STATUS = " + status);

            // for SMTP streams the client decides when to close the connection
            // so we watch for that and release the session when detected

            if (manager.getSSLEngine().isInboundDone()) {
                if (session.getServerPort() == 25) {
                    session.release();
                    return (true);
                }
                logger.debug("Unexpected isInboundDone() == TRUE");
                return false;
            }
            if (manager.getSSLEngine().isOutboundDone()) {
                if (session.getServerPort() == 25) {
                    session.release();
                    return (true);
                }
                logger.debug("Unexpected IsOutboundDone() == TRUE");
                return false;
            }

            switch (status)
            {
            // should never happen since this will only be returned from
            // a call to wrap or unwrap but we include it to be complete
            case FINISHED:
                logger.error("Unexpected FINISHED in unparseWorker loop");
                return false;

                // handle outstanding tasks during handshake
            case NEED_TASK:
                done = doNeedTask(session, data);
                break;

            // the parser handles most handshake stuff so we can ignore
            case NEED_UNWRAP:
                logger.error("Unexpected NEED_UNWRAP in unparseWorker loop");
                return false;

                // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap(session, data, target);
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking(session, data, target);
                break;

            // should never happen but we handle just to be safe
            default:
                logger.error("Unknown SSLEngine status in unparseWorker loop");
                return false;
            }
        }

        return done;
    }

    /**
     * Called when SSLEngine status = NEED_TASK. We call run for all outstanding
     * tasks and then return false to break out of the parser processing loop so
     * we can receive more data.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return False
     * @throws Exception
     */
    private boolean doNeedTask(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        Runnable runnable;

        // loop and run SSLEngine outstanding tasks
        while ((runnable = manager.getSSLEngine().getDelegatedTask()) != null) {
            logger.debug("EXEC_TASK " + runnable.toString());
            runnable.run();
        }
        return false;
    }

    /**
     * Called when SSLEngine status = NEED_WRAP
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @param target
     *        The buffer to store the encrypted data
     * @return True to continue the parser loop, false to break out
     * @throws Exception
     */
    private boolean doNeedWrap(AppTCPSession session, ByteBuffer data, ByteBuffer target) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        SSLEngineResult result;
        ByteBuffer empty = ByteBuffer.allocate(32);

        // during handshake the SSL engine doesn't do anything with
        // data we pass, so we just wrap an empty buffer here.
        result = manager.getSSLEngine().wrap(empty, target);
        logger.debug("EXEC_WRAP " + result.toString());
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // during the handshake we only expect to need a single wrap call
        // so if the status did not transition we have a big problem
        if (result.getHandshakeStatus() != HandshakeStatus.NEED_UNWRAP) throw new Exception("SSLEngine logic fault");

        // if the wrap call didn't produce any data return done=false
        if (result.bytesProduced() == 0) return false;

        // send the initial handshake data and let the parser handle the rest
        target.flip();

        if (manager.getClientSide()) {
            session.sendDataToClient(target);
            session.setServerBuffer(null);
        } else {
            session.sendDataToServer(target);
            session.setClientBuffer(null);
        }
        return true;
    }

    /**
     * Called when we receive data and dataMode is true, meaning we're done with
     * the handshake and we're now passing data back and forth between the two
     * sides.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @param target
     *        The buffer to store the encrypted data
     * @return True to continue the parser loop, false to break out
     * @throws Exception
     */
    private boolean doNotHandshaking(AppTCPSession session, ByteBuffer data, ByteBuffer target) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        SSLEngineResult result;

        // if we're not in dataMode yet we need to work on the handshake
        if (manager.getDataMode() == false) {
            // server side should not get here unless dataMode is active
            if (manager.getClientSide() == false) {
                throw new Exception("SSLEngine datamode fault");
            }

            // we're on the client side and dataMode is not active yet so we
            // need to unwrap the initial client data saved by the parser
            manager.getCasingBuffer().flip();
            result = manager.getSSLEngine().unwrap(manager.getCasingBuffer(), target);
            logger.debug("UNWRAP_TRANSITION " + result.toString());
            manager.getCasingBuffer().clear();
            return false;
        }

        // the unparser will always call wrap to convert plain to SSL
        result = manager.getSSLEngine().wrap(data, target);
        logger.debug("EXEC_HANDSHAKING " + result.toString());

        // if the engine reports closed clear the buffer and return done=true
        if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            if (manager.getClientSide()) {
                session.setClientBuffer(null);
            } else {
                session.setServerBuffer(null);
            }
            return true;
        }

        // any other result is very bad news
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // if we have gone back into handshake mode we clear dataMode and return done=false and
        // let the loop detect the problem since the SSLEngine doesn't support re-handshake
        if (result.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            manager.setDataMode(false);
            return false;
        }

        // if the wrap call didn't produce anything yet just return done=false
        // so we can call wrap again on the next pass through the loop
        if (result.bytesProduced() == 0) return false;

        // got some data from the wrap call so well prepare to send it along
        target.flip();

        // here in the unparser we're converting plain-text back to SSL so we
        // send client data to the client and server data to the server
        if (manager.getClientSide()) {
            session.sendDataToClient(target);
            session.setServerBuffer(null);
        } else {
            session.sendDataToServer(target);
            session.setClientBuffer(null);
        }
        return true;
    }

    /**
     * Gets the client or server side manager attached to the session
     * 
     * @param session
     *        The TCP session
     * @return The appropriate client or server side manager
     */
    private SslInspectorManager getManager(AppTCPSession session)
    {
        if (clientSide) return (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
        else return (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
    }
}
