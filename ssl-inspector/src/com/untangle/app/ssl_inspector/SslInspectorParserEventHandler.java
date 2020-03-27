/**
 * $Id: SslInspectorParserEventHandler.java,v 1.00 2017/03/03 19:29:12 dmorris Exp $
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

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.OAuthDomain;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.vnet.TCPNewSessionRequest;

/**
 * The parser handles converting the encrypted stream of SSL traffic to
 * plain-text traffic. On both the client and server side of the casing we have
 * to complete the SSL handshake before application data can flow. The process
 * begins when we receive the initial ClientHello message. We need the server
 * side handshake to complete first, so the initial ClientHello is saved in the
 * casing and a dummy message is passed to the server to initiate the handshake
 * on that side. Data goes back and forth between the casing and server until
 * the handshake is complete, at which point we pass a dummy message back
 * allowing the client side handshake to continue. Once the handshake is
 * complete on both sides, dataMode is set which allows end to end and traffic
 * to flow freely back and forth until the connection is terminated.
 * 
 * @author mahotz
 * 
 */
public class SslInspectorParserEventHandler extends AbstractEventHandler
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
    protected SslInspectorParserEventHandler(boolean clientSide, SslInspectorApp app)
    {
        super();
        this.clientSide = clientSide;
        this.app = app;
    }

    /**
     * We do a license check for all new session requests and release the
     * session if our license is not valid.
     * 
     * @param sessionRequest
     *        The new session request
     */

    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)
    {
        // if the license is not valid we ignore all traffic
        if (app.isLicenseValid() != true) {
            sessionRequest.release();
            return;
        }
    }

    /**
     * We handle new sessions by creating a manager and attaching to the
     * session. We also attach a special flag letting other apps know we're
     * doing inspection.
     * 
     * @param session
     *        The TCP session
     */
    @Override
    public void handleTCPNewSession(AppTCPSession session)
    {

        SslInspectorManager manager = new SslInspectorManager(session, clientSide, app);

        if (clientSide) session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER, manager);
        else session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER, manager);

        // attach something to let everyone else know we are working the session 
        session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT, Boolean.TRUE);

        // set the server read buffer size and limit really large to deal
        // with huge certs that contain lots of subject alt names
        // such as cert returned from google.co.nz
        session.serverReadBufferSize(32768);
        session.serverReadLimit(32768);
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
            streamParse(session, data, false);
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
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
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamParse(session, data, true);
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
            streamParse(session, data, false);
        } else {
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
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
            logger.warn("Received unexpected event");
            throw new RuntimeException("Received unexpected event");
        } else {
            streamParse(session, data, true);
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
    private void streamParse(AppTCPSession session, ByteBuffer data, boolean s2c)
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

            logger.debug("---------- " + (s2c ? "CLIENT" : "SERVER") + " parse() received " + data.limit() + " bytes ----------");

            if (s2c == true) {
                session.sendDataToClient(data);
                if (manager.tlsFlagClient == true) {
                    if (manager.checkTlsServer(data) == true) {
                        logger.debug("PARSER SETTING SERVER TLS FLAG");
                        manager.tlsFlagServer = true;
                    } else {
                        logger.debug("PARSER RELEASING TLS LOGIC FAILURE");
                        manager.tlsFlagClient = manager.tlsFlagServer = false;
                        shutdownOtherSide(session, false);
                        session.release();
                    }
                }
            }

            if (s2c == false) {
                session.sendDataToServer(data);
                if (manager.checkTlsClient(data) == true) {
                    logger.debug("PARSER SETTING CLIENT TLS FLAG");
                    manager.tlsFlagClient = true;
                }
            }
            return;
        }

        try {
            parse(session, data, manager);
        } catch (Exception exn) {
            logger.warn("Error during streamParse()", exn);
            return;
        }
    }

    /**
     * This is the main data parser
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @param manager
     *        The SSL manager
     */
    public void parse(AppTCPSession session, ByteBuffer data, SslInspectorManager manager)
    {
        String sslProblem = null;
        String logDetail = null;
        boolean success = false;

        logger.debug("---------- " + (manager.getClientSide() ? "CLIENT" : "SERVER") + " parse() received " + data.limit() + " bytes ----------");

        // empty buffer indicates the session is terminating
        if (data.limit() == 0) {
            return;
        }

        // pass the data to the parse worker function
        try {
            success = parseWorker(session, data);
        }

        catch (SSLException ssl) {
            sslProblem = ssl.getMessage();
            if (sslProblem == null) sslProblem = "Unknown SSL exception";

            if (sslProblem.contains("unrecognized_name")) {
                String targetName = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
                String targetHost = session.getServerAddr().getHostAddress().toString();
                if ((targetName != null) && (targetHost != null)) {
                    String brokenServer = (targetHost + " | " + targetName);
                    logger.warn("Adding broken SNI server: " + brokenServer);
                    app.addBrokenServer(brokenServer);
                }
            }

            // for normal close we kill both sides and return  
            if (sslProblem.contains("close_notify")) {
                shutdownOtherSide(session, true);
                session.killSession();
                return;
            }
        }

        catch (Exception exn) {
            logger.warn("Exception calling parseWorker", exn);
        }

        // no success result means something went haywire so we kill the session
        if (success == false) {
            // put something in the event log starting with any ssl message we extracted above
            if (sslProblem != null) {
                logDetail = ((clientSide ? "Client" : "Server") + " SSL decrypt exception: " + sslProblem);
                if (manager.getPeerThumbprint() != null) logDetail += (" CERT: " + manager.getPeerThumbprint());
            }
            if (logDetail == null) logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            if (logDetail == null) logDetail = session.getServerAddr().getHostAddress();
            SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_ABANDONED, logDetail);
            app.logEvent(logevt);
            app.incrementMetric(SslInspectorApp.STAT_ABANDONED);

            // only log a warning if we didn't get an exception message for the event log 
            if (sslProblem == null) logger.warn("Session abandon on parseWorker false return for " + logDetail);

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
     * This is the main parser worker where we sit in a loop passing data to the
     * SSLEngine, reading the handshake status, and doing whatever it tells us
     * to do until there is nothing left to be done.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return True if everything is fine, false for any error or failure
     * @throws Exception
     */
    private boolean parseWorker(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        boolean done = false;
        SslInspectorRule ruleMatch = null;
        HandshakeStatus status;

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

        // do special handling of the initial client hello message
        if ((manager.getSSLEngine() == null) && (manager.getClientSide() == true) && (manager.getDataMode() == false)) {
            handleClientHello(session, data);
            return true;
        }

        // if the server sends the first message then we are dealing with
        // something other than SSL so do special handling here
        if ((manager.getSSLEngine() == null) && (manager.getClientSide() == false) && (manager.getDataMode() == false)) {
            ruleMatch = new SslInspectorRule();
            ruleMatch.setRuleId(0);
            ruleMatch.setDescription("Missing ClientHello packet");

            // if invalid traffic is blocked we log and block the traffic
            if (app.getSettings().getBlockInvalidTraffic() == true) {
                ruleMatch.setAction(new SslInspectorRuleAction(SslInspectorRuleAction.ActionType.BLOCK, true));
                SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), ruleMatch.getRuleId(), SslInspectorApp.STAT_BLOCKED, ruleMatch.getDescription());
                app.logEvent(logevt);
                logger.debug("RULE MATCH EVENT = " + logevt.toString());
                app.incrementMetric(SslInspectorApp.STAT_BLOCKED);

                // kill the session on both sides
                shutdownOtherSide(session, true);
                session.killSession();
                return true;
            }

            // invalid traffic not blocked so we ignore the session
            ruleMatch.setAction(new SslInspectorRuleAction(SslInspectorRuleAction.ActionType.IGNORE, true));
            SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), ruleMatch.getRuleId(), SslInspectorApp.STAT_IGNORED, ruleMatch.getDescription());
            app.logEvent(logevt);
            logger.debug("RULE MATCH EVENT = " + logevt.toString());
            app.incrementMetric(SslInspectorApp.STAT_IGNORED);

            // let everyone else know that we are ignoring the session
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT, Boolean.FALSE);

            // release the session on both sides and send the original
            // message received from the server to the client
            shutdownOtherSide(session, false);
            session.sendObjectToClient(new ReleaseToken());
            session.sendDataToClient(data);
            session.release();
            return true;
        }

        while (done == false) {
            status = manager.getSSLEngine().getHandshakeStatus();
            logger.debug("STATUS = " + status);
            logger.debug("BUFFER = " + data.toString());

            // if isInboundDone() becomes true on the server side during the
            // handshake we likely have encountered an untrusted certificate
            if ((manager.getClientSide() == false) && (manager.getSSLEngine().isInboundDone() == true)) {

                // log the untrusted certificate event
                String logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
                if (logDetail == null) logDetail = session.getServerAddr().getHostAddress();

                SslInspectorLogEvent logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_UNTRUSTED, logDetail);
                app.logEvent(logevt);
                app.incrementMetric(SslInspectorApp.STAT_UNTRUSTED);

                logger.debug("UNTRUSTED SERVER = " + logDetail);

                // kill the session on both sides
                shutdownOtherSide(session, true);
                session.killSession();
                return true;
            }

            // problems with the external server cert seem to cause one
            // of these to become true during handshake so we just return
            if (manager.getSSLEngine().isInboundDone()) {
                logger.debug("Unexpected isInboundDone() == TRUE");
                return false;
            }
            if (manager.getSSLEngine().isOutboundDone()) {
                logger.debug("Unexpected isOutboundDone() == TRUE");
                return false;
            }

            // check the SSL handshake status we grabbed above
            switch (status)
            {
            // should never happen since this will only be returned from
            // a call to wrap or unwrap but we include it to be complete
            case FINISHED:
                logger.error("Unexpected FINISHED in parseWorker loop");
                return false;

                // handle outstanding tasks during handshake
            case NEED_TASK:
                done = doNeedTask(session, data);
                break;

            // handle unwrap during handshake
            case NEED_UNWRAP:
                done = doNeedUnwrap(session, data);
                break;

            // handle wrap during handshake
            case NEED_WRAP:
                done = doNeedWrap(session, data);
                break;

            // handle data when no handshake is in progress
            case NOT_HANDSHAKING:
                done = doNotHandshaking(session, data);
                break;

            // should never happen but we handle just to be safe
            default:
                logger.error("Unknown SSLEngine status in parseWorker loop");
                return false;
            }
        }

        return done;
    }

    /**
     * If we're on the client side and dataMode is not yet active, we have the
     * initial TLS ClientHello message. We first try to extract the SNI hostname
     * from the message. Next we check the session against the RuleCondition.
     * This is the only time we can decide to release the session without
     * messing up the SSL handshake, since once we kick off either side we can't
     * extricate ourselves from playing man-in-the-middle. See RFC-5746 for
     * details. If we match a rule configured for ignore then we release the
     * session and pass the initial client chunk directly to the server. If no
     * ignore rules match, the we setup to inspect the stream. First we need to
     * kick off the server handshake. We can't do the client handshake until the
     * server side is done since we need the server certificate to generate a
     * fake cert for the client. So on the first packet from the client we save
     * the initial chunk of data for later and pass a dummy message to the
     * server unparser to start the handshake between us and the external
     * server.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @throws Exception
     */
    private void handleClientHello(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        java.security.cert.X509Certificate serverCert = null;
        SslInspectorRule ruleMatch = null;
        SslInspectorLogEvent logevt = null;
        String sniHostname = null;
        String logDetail = null;
        boolean allowed = false;

        // fist clear the casing buffer since it could have been partially
        // filled by a previous underflow exception and then save the
        // ClientHello message so we can handle it later
        manager.getCasingBuffer().clear();
        manager.getCasingBuffer().put(data);
        data.flip();

        try {
            // pass a duplicate to the SNI extractor in case it tweaks anything
            sniHostname = manager.extractSNIhostname(data.duplicate());
        }

        // a buffer underflow exception from the extractor means it didn't
        // have enough data to parse the TLS message so pass the inbound
        // data buffer back to the uvm to wait for more client data
        catch (BufferUnderflowException exn) {
            logger.debug("CLIENT_HELLO_UNDERFLOW = " + data.toString());
            data.position(data.limit());
            data.limit(data.capacity());

            // keep data in buffer, wait for more
            session.setClientBuffer(data);
            return;
        }

        // any other extractor exception means the SSL packet was not valid
        // so we craft a special rule matcher to block or ignore
        catch (Exception exn) {
            ruleMatch = new SslInspectorRule();
            ruleMatch.setRuleId(0);
            ruleMatch.setDescription(exn.getMessage());

            if (app.getSettings().getBlockInvalidTraffic() == true) ruleMatch.setAction(new SslInspectorRuleAction(SslInspectorRuleAction.ActionType.BLOCK, true));
            else ruleMatch.setAction(new SslInspectorRuleAction(SslInspectorRuleAction.ActionType.IGNORE, true));

            // if the message was null this was unexpected so log a warning
            if (exn.getMessage() == null) logger.warn("Exception parsing SNI hostname", exn);
        }

        // get the captive portal session capture flag
        String captureFlag = (String) session.globalAttachment(AppSession.KEY_CAPTIVE_PORTAL_SESSION_CAPTURE);

        // When captive portal is using OAuth we have to ignore the same
        // sessions it allows for access to the external login page. If
        // we try to inspect these things go completely off the rails.
        if ((captureFlag != null) && (sniHostname != null)) {

            // check the SNI name against each item in the OAuthConfigList
            for (OAuthDomain item : app.oauthConfigList) {
                // check PROVIDER = all
                if ((item.provider.equals("all")) && ((captureFlag == "GOOGLE") || (captureFlag == "FACEBOOK") || (captureFlag == "MICROSOFT") || (captureFlag == "ANY_OAUTH"))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }

                // check PROVIDER = google
                if ((item.provider.equals("google")) && ((captureFlag == "GOOGLE") || (captureFlag == "ANY_OAUTH"))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }

                // check PROVIDER = facebook
                if ((item.provider.equals("facebook")) && ((captureFlag == "FACEBOOK") || (captureFlag == "ANY_OAUTH"))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }

                // check PROVIDER = microsoft
                if ((item.provider.equals("microsoft")) && ((captureFlag == "MICROSOFT") || (captureFlag == "ANY_OAUTH"))) {
                    if (item.match.equals("full") && sniHostname.toLowerCase().equals(item.name)) allowed = true;
                    if (item.match.equals("end") && sniHostname.toLowerCase().endsWith(item.name)) allowed = true;
                }
            }
        }

        if (allowed == true) {
            logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_IGNORED, "Captive Portal OAuth: " + sniHostname);
            app.logEvent(logevt);
            app.incrementMetric(SslInspectorApp.STAT_IGNORED);
            logger.debug("CAPTIVE PORTAL IGNORE(" + captureFlag + ") = " + logevt.toString());

            // let everyone else know that we are ignoring the session
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT, Boolean.FALSE);

            // release the session in the casing on the other side
            shutdownOtherSide(session, false);

            // release the session and send the original client hello to the server
            session.sendObjectToServer(new ReleaseToken());
            session.sendDataToServer(data);
            session.release();
            return;
        }

        // if the captive portal flag is set we must do inspection because it
        // expects to use our unencrypted stream to send the redirect with the
        // added benefit of a good MitM cert even on sites we'll later ignore 
        if ((captureFlag != null) && (captureFlag == "CAPTURE")) {

            // craft a wakeup message and send it directly to the server side
            // casing using simulateClientData inside the server side casing
            ByteBuffer wakeup = ByteBuffer.allocate(256);
            wakeup.put(SslInspectorManager.IPC_WAKEUP_MESSAGE);
            wakeup.flip();
            SslInspectorManager server = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
            server.getSession().simulateClientData(wakeup);
            return;
        }

        // wait until after the exception handlers to increment the counter
        // so we don't increment on a buffer underflow exception
        app.incrementMetric(SslInspectorApp.STAT_COUNTER);

        // if we found the SNI hostname attach it for the rule matcher
        if (sniHostname != null) {
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME, sniHostname);
            logger.debug("SSL_INSPECTOR_SNI_HOSTNAME = " + sniHostname);
            serverCert = UvmContextFactory.context().certCacheManager().fetchServerCertificate(sniHostname);
        }

        // grab the cached certificate for the server but only for non-SMTP traffic 
        if (session.getServerPort() != 25 && sniHostname == null) {
            serverCert = UvmContextFactory.context().certCacheManager().fetchServerCertificate(session.getServerAddr().getHostAddress().toString());
        }

        // attach the subject and issuer names for use by the rule matcher
        if (serverCert != null) {
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SUBJECT_DN, serverCert.getSubjectDN().toString());
            logger.debug("CERTCACHE FOUND SubjectDN = " + serverCert.getSubjectDN());
            session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_ISSUER_DN, serverCert.getIssuerDN().toString());
            logger.debug("CERTCACHE FOUND IssuerDN = " + serverCert.getIssuerDN());
        }

        // if we didn't create an invalid packet rule matcher above then
        // we walk through the list of rules and find the first match
        if (ruleMatch == null) {
            List<SslInspectorRule> ruleList = app.getSettings().getIgnoreRules();

            logger.debug("Checking Rules against AppTCPSession : " + session.getProtocol() + " " + session.getClientAddr().getHostAddress() + ":" + session.getClientPort() + " -> " + session.getServerAddr().getHostAddress() + ":" + session.getServerPort());

            for (SslInspectorRule rule : ruleList) {
                if (rule.getEnabled() == false) continue;
                if (rule.matches(session) == false) continue;
                ruleMatch = rule;
                logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
                break;
            }
        }

        // handle rule matches
        if (ruleMatch != null) {
            // if we created a block rule above we release and kill the session
            if (ruleMatch.getAction().getActionType() == SslInspectorRuleAction.ActionType.BLOCK) {
                // log the block event using any log detail set above but
                // falling back to the rule description if detail is empty
                if (logDetail == null) logDetail = ruleMatch.getDescription();
                logevt = new SslInspectorLogEvent(session.sessionEvent(), ruleMatch.getRuleId(), SslInspectorApp.STAT_BLOCKED, logDetail);
                app.logEvent(logevt);
                app.incrementMetric(SslInspectorApp.STAT_BLOCKED);
                logger.debug("RULE MATCH EVENT = " + logevt.toString());

                // kill the session on both sides
                shutdownOtherSide(session, true);
                session.killSession();
                return;
            }

            // if we found an ignore rule we release the session and return
            // the original client hello message for sending to the server
            else if (ruleMatch.getAction().getActionType() == SslInspectorRuleAction.ActionType.IGNORE) {
                // log the ignore event using any log detail set above but
                // falling back to the rule description if detail is empty
                if (logDetail == null) logDetail = ruleMatch.getDescription();
                logevt = new SslInspectorLogEvent(session.sessionEvent(), ruleMatch.getRuleId(), SslInspectorApp.STAT_IGNORED, logDetail);
                app.logEvent(logevt);
                app.incrementMetric(SslInspectorApp.STAT_IGNORED);
                logger.debug("RULE MATCH EVENT = " + logevt.toString());

                // let everyone else know that we are ignoring the session
                session.globalAttach(AppTCPSession.KEY_SSL_INSPECTOR_SESSION_INSPECT, Boolean.FALSE);

                // release the session in the casing on the other side
                shutdownOtherSide(session, false);

                // release the session and send the original client hello to the server
                session.sendObjectToServer(new ReleaseToken());
                session.sendDataToServer(data);
                session.release();
                return;
            }
        }

        if (ruleMatch != null) {
            // if we have a rule match and make it this far then it must have
            // been an inspect rule so we create an appropriate log event
            logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            if (logDetail == null) logDetail = ruleMatch.getDescription();
            logevt = new SslInspectorLogEvent(session.sessionEvent(), ruleMatch.getRuleId(), SslInspectorApp.STAT_INSPECTED, logDetail);
            logger.debug("RULE MATCH EVENT = " + logevt.toString());
        } else {
            // no rule match so create a default INSPECT log event
            logDetail = (String) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SNI_HOSTNAME);
            if (logDetail == null) logDetail = session.getServerAddr().getHostAddress();
            logevt = new SslInspectorLogEvent(session.sessionEvent(), 0, SslInspectorApp.STAT_INSPECTED, logDetail);
        }

        // either no rule match or we matched an inspect rule so log an event
        app.logEvent(logevt);
        app.incrementMetric(SslInspectorApp.STAT_INSPECTED);

        // craft a wakeup message and send it directly to the server side
        // casing using simulateClientData inside the server side casing
        ByteBuffer wakeup = ByteBuffer.allocate(256);
        wakeup.put(SslInspectorManager.IPC_WAKEUP_MESSAGE);
        wakeup.flip();
        SslInspectorManager server = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_SERVER_MANAGER);
        server.getSession().simulateClientData(wakeup);
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
     * Called when SSLEngine status = NEED_UNWRAP
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return True to continue the parser loop, false to break out
     * @throws Exception
     */
    private boolean doNeedUnwrap(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        ByteBuffer target = ByteBuffer.allocate(32768);
        SSLEngineResult result;

        // unwrap the argumented data into the engine buffer - we expect all 
        // all the data to be consumed internally with no bytes produced
        result = manager.getSSLEngine().unwrap(data, target);
        logger.debug("EXEC_UNWRAP " + result.toString());

        if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {

            // underflow during unwrap means the SSLEngine needs more data
            // but it's also possible it used some of the passed data so we
            // compact the receive buffer which leaves the position at the
            // end of the existing data and ready to receive more
            data.compact();
            logger.debug("UNDERFLOW_LEFTOVER = " + data.toString());

            if (manager.getClientSide()) session.setClientBuffer(data);
            else session.setServerBuffer(data);
            return true;
        }

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine unwrap fault");

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_UNWRAP) return false;

        // handle transition from handshaking to finished
        if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) {
            // set the datamode flag
            manager.setDataMode(true);

            // nothing to do on the client side when handshake is finished
            if (manager.getClientSide() == true) return false;

            // grab the server certificate and save in our mananger so we can
            // use on the client side to create our fake certificate
            java.security.cert.Certificate peerCert = manager.getSSLEngine().getSession().getPeerCertificates()[0];
            manager.setPeerCertificate(session.getServerAddr().getHostAddress(), (java.security.cert.X509Certificate) peerCert);
            logger.debug("CERTIFICATE = " + peerCert.toString());

            // if not SMTP we also save the certificate in the global
            // certificate cache so it will always be up to date
            if (session.getServerPort() != 25) {
                UvmContextFactory.context().certCacheManager().updateServerCertificate(session.getServerAddr().getHostAddress().toString(), (java.security.cert.X509Certificate) peerCert);
            }

            // Once the server side handshake is finished we need to handle the
            // client side handshake so we craft a wakeup message and send
            // it directly to the client side casing using simulateServerData
            // inside the client side casing
            ByteBuffer wakeup = ByteBuffer.allocate(256);
            wakeup.put(SslInspectorManager.IPC_WAKEUP_MESSAGE);
            wakeup.flip();
            SslInspectorManager client = (SslInspectorManager) session.globalAttachment(AppTCPSession.KEY_SSL_INSPECTOR_CLIENT_MANAGER);
            client.getSession().simulateServerData(wakeup);
            return true;
        }

        // the unwrap call shouldn't produce data during handshake and if that
        // is the case we return done=false here allowing the loop to continue
        if (result.bytesProduced() == 0) return false;

        // unwrap calls during handshake should never produce data
        throw new Exception("SSLEngine produced unexpected data during handshake unwrap");
    }

    /**
     * Called when SSLEngine status = NEED_WRAP
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return True to continue the parser loop, false to break out
     * @throws Exception
     */
    private boolean doNeedWrap(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        ByteBuffer target = ByteBuffer.allocate(32768);
        SSLEngineResult result;

        // wrap the argumented data into the engine buffer
        result = manager.getSSLEngine().wrap(data, target);
        logger.debug("EXEC_WRAP " + result.toString());

        // check for engine problems
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine wrap fault");

        // if the wrap call produced some data send it to the peer
        if (result.bytesProduced() != 0) {
            target.flip();
            // during handshake client data goes from client to client and server
            // data goes from server to server, because they are two separate handshakes
            // unlike how normally client flows from client to server and vice versa
            if (manager.getClientSide()) {
                session.sendDataToClient(target);
            } else {
                session.sendDataToServer(target);
            }
        }

        // if the engine result hasn't changed we need more processing
        if (result.getHandshakeStatus() == HandshakeStatus.NEED_WRAP) return false;

        // if the handshake completed set the dataMode flag
        if (result.getHandshakeStatus() == HandshakeStatus.FINISHED) manager.setDataMode(true);

        if (data.position() < data.limit()) return (false);

        return true;
    }

    // ------------------------------------------------------------------------

    /**
     * Called when we receive data and dataMode is true, meaning we're done with
     * the handshake and we're now passing data back and forth between the two
     * sides.
     * 
     * @param session
     *        The TCP session
     * @param data
     *        The data received
     * @return True to continue the parser loop, false to break out
     * @throws Exception
     */
    private boolean doNotHandshaking(AppTCPSession session, ByteBuffer data) throws Exception
    {
        SslInspectorManager manager = getManager(session);
        ByteBuffer target = ByteBuffer.allocate(32768);
        SSLEngineResult result;

        // we don't expect to get here unless dataMode is already active
        if (manager.getDataMode() == false) throw new Exception("SSLEngine datamode fault");

        // the parser will always call unwrap to convert SSL to plain
        result = manager.getSSLEngine().unwrap(data, target);
        logger.debug("EXEC_HANDSHAKING " + result.toString());

        if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            // if the target buffer is empty we are finished so return done=true 
            if (target.position() == 0) {
                return true;
            }

            // the target buffer has unwrapped data so pass it along
            target.flip();

            // when the handshake is complete and data is flowing we send
            // unparse data from the client to the server and vice versa
            if (manager.getClientSide()) {
                session.sendDataToServer(target);
            } else {
                session.sendDataToClient(target);
            }
            return true;
        }

        if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
            data.compact();
            logger.debug("OVERFLOW_LEFTOVER = " + data.toString());

            // unwrap did produce some data so we'll return it now along
            // with the partially filled data buffer so we can get more
            target.flip();

            // when the handshake is complete and data is flowing we send
            // unparse data from the client to the server and vice versa
            if (manager.getClientSide()) {
                session.sendDataToServer(target);
                session.setClientBuffer(data);
            } else {
                session.sendDataToClient(target);
                session.setServerBuffer(data);
            }
            return true;
        }

        if (result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            // if the data buffer is full allocate a new larger buffer
            // and copy the leftover data from the passed buffer
            if (data.limit() == data.capacity()) {
                ByteBuffer special = ByteBuffer.allocate(data.capacity() * 2);
                special.put(data);
                data = special;
            }

            // passed buffer has room to receive more data so just compact
            else {
                data.compact();
            }

            // the allocate or compact will both leave the position at the end
            // of any existing data in the buffer and ready to receive more 
            logger.debug("UNDERFLOW_LEFTOVER = " + data.toString());

            // use the partially filled buffer for the next receive
            if (target.position() == 0) {
                if (manager.getClientSide()) {
                    session.setClientBuffer(data);
                } else {
                    session.setServerBuffer(data);
                }
                return true;
            }

            // unwrap did produce some data so we'll return it now
            target.flip();

            // when the handshake is complete and data is flowing we send
            // unparse data from the client to the server and vice versa
            if (manager.getClientSide()) {
                session.sendDataToServer(target);
                session.setClientBuffer(data);
            } else {
                session.sendDataToClient(target);
                session.setServerBuffer(data);
            }

            return true;
        }

        // any other result is very bad news
        if (result.getStatus() != SSLEngineResult.Status.OK) throw new Exception("SSLEngine unwrap fault");

        // if we have gone back into handshake mode we return done=false so the caller
        // can detect and abandon the session since the SSLEngine doesn't support rehandshake
        if (result.getHandshakeStatus() != HandshakeStatus.NOT_HANDSHAKING) {
            manager.setDataMode(false);
            return false;
        }

        // if the unwrap call doesn't produce any data we just return
        // done=false and let the processing continue
        if (result.bytesProduced() == 0) return false;

        // The SSLEngine gave us some data so pass it along
        target.flip();

        // when the handshake is complete and data is flowing we send
        // unparse data from the client to the server and vice versa
        if (manager.getClientSide()) {
            session.sendDataToServer(target);
        } else {
            session.sendDataToClient(target);
        }

        // if there is still data in the buffer we return done=false
        // to let the processing continue
        if (data.position() < data.limit()) {
            logger.debug("PARTIAL_LEFTOVER = " + data.toString());
            return (false);
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
