/**
 * $Id: ApplicationControlEventHandler.java 38041 2014-07-08 06:57:29Z dmorris $
 */

package com.untangle.app.application_control;

import java.util.List;
import java.net.ConnectException;
import java.nio.ByteBuffer;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.vnet.AbstractEventHandler;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import org.apache.log4j.Logger;

/**
 * This is the main event handler where we pass traffic to the daemon for
 * classification and allow or block the traffic as required.
 * 
 * @author mahotz
 * 
 */
public class ApplicationControlEventHandler extends AbstractEventHandler
{
    private static final int MAX_CHUNK_COUNT = 15;

    public static final int NAVL_STATE_TERMINATED = 0; // Indicates the connection has been terminated
    public static final int NAVL_STATE_INSPECTING = 1; // Indicates the connection is under inspection
    public static final int NAVL_STATE_MONITORING = 2; // Indicates the connection is under monitoring
    public static final int NAVL_STATE_CLASSIFIED = 3; // Indicates the connection is fully classified

    public static final int SELECT_TIMEOUT = 100;

    private final Logger logger = Logger.getLogger(getClass());
    private ApplicationControlApp app;
    private int networkPort = 0;

    public enum TrafficAction
    {
        ALLOW, BLOCK, RELEASE, TARPIT
    }

    /**
     * Constructor
     * 
     * @param app
     *        The application that created us
     * @param networkPort
     *        The specific network port to match, or zero for all
     */
    public ApplicationControlEventHandler(ApplicationControlApp app, int networkPort)
    {
        super(app);
        this.app = app;
        this.networkPort = networkPort;
    }

    /**
     * This is the handler for all new TCP session requests. We use this
     * function to setup everything we need to analyze the traffic.
     * 
     * @param sessionRequest
     *        The session request
     */
    @Override
    public void handleTCPNewSessionRequest(TCPNewSessionRequest sessionRequest)
    {
        // if a specific port was passed to the constructor we check the
        // server port of new sessions and release stuff we don't want
        if (networkPort != 0) {
            if (sessionRequest.getNewServerPort() != networkPort) {
                sessionRequest.release();
                return;
            }
        }

        this.app.incrementMetric(ApplicationControlApp.STAT_SCAN);
        app.statistics.IncrementSessionCount();
        processNewSession(sessionRequest);
    }

    /**
     * This is the handler for finished TCP sessions. We use this function to
     * cleanup everything we created when the session was created.
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleTCPFinalized(AppTCPSession session)
    {
        cleanupActiveSession(session, true);
        super.handleTCPFinalized(session);
    }

    /**
     * This function handles processing raw session traffic from the client
     * 
     * @param sess
     *        The session
     * @param data
     *        The raw client data
     */
    @Override
    public void handleTCPClientChunk(AppTCPSession sess, ByteBuffer data)
    {
        ApplicationControlStatus status = (ApplicationControlStatus) sess.attachment();

        // before we do anything else see if this session is already set
        // for tarpit and if so drop the traffic on the floor
        if (status.tarpit == true) return;

        TrafficAction action = processTraffic(true, status, sess, data);

        // set the tarpit flag and block the traffic
        if (action == TrafficAction.TARPIT) {
            status.tarpit = true;
            return;
        }

        // block traffic and clean up the session
        if (action == TrafficAction.BLOCK) {
            sess.resetClient();
            sess.resetServer();
            cleanupActiveSession(sess, false);
            return;
        }

        // allow traffic and clean up the session
        if (action == TrafficAction.RELEASE) {
            cleanupActiveSession(sess, false);
        }

        super.handleTCPClientChunk(sess, data);
    }

    /**
     * This function handles processing raw session data from the server
     * 
     * @param sess
     *        The session
     * @param data
     *        The raw server data
     */
    @Override
    public void handleTCPServerChunk(AppTCPSession sess, ByteBuffer data)
    {
        ApplicationControlStatus status = (ApplicationControlStatus) sess.attachment();

        // before we do anything else see if this session is already set
        // for tarpit and if so drop the traffic on the floor
        if (status.tarpit == true) return;

        TrafficAction action = processTraffic(false, status, sess, data);

        // set the tarpit flag and block the traffic
        if (action == TrafficAction.TARPIT) {
            status.tarpit = true;
            return;
        }

        // block traffic and clean up the session
        if (action == TrafficAction.BLOCK) {
            sess.resetClient();
            sess.resetServer();
            cleanupActiveSession(sess, false);
            return;
        }

        // allow traffic and clean up the session
        if (action == TrafficAction.RELEASE) {
            cleanupActiveSession(sess, false);
        }

        super.handleTCPServerChunk(sess, data);
    }

    /**
     * This is the handler for all new UDP session requests. We use this
     * function to setup everything we need to analyze the traffic.
     * 
     * @param sessionRequest
     *        The session request
     */
    @Override
    public void handleUDPNewSessionRequest(UDPNewSessionRequest sessionRequest)
    {
        this.app.incrementMetric(ApplicationControlApp.STAT_SCAN);
        app.statistics.IncrementSessionCount();
        processNewSession(sessionRequest);
    }

    /**
     * This is the handler for finished UDP sessions. We use this function to
     * cleanup everything we created when the session was created.
     * 
     * @param session
     *        The session
     */
    @Override
    public void handleUDPFinalized(AppUDPSession session)
    {
        cleanupActiveSession(session, true);
        super.handleUDPFinalized(session);
    }

    /**
     * This function handles processing raw session traffic from the client
     * 
     * @param sess
     *        The session
     * @param data
     *        The raw client data
     * @param header
     *        theh IP packet header
     */
    @Override
    public void handleUDPClientPacket(AppUDPSession sess, ByteBuffer data, IPPacketHeader header)
    {
        ApplicationControlStatus status = (ApplicationControlStatus) sess.attachment();

        // before we do anything else see if this session is already set
        // for tarpit and if so drop the traffic on the floor
        if (status.tarpit == true) return;

        TrafficAction action = processTraffic(true, status, sess, data);

        // set the tarpit flag and block the traffic
        if (action == TrafficAction.TARPIT) {
            status.tarpit = true;
            return;
        }

        // expire both sides and clean up the session
        if (action == TrafficAction.BLOCK) {
            sess.expireClient();
            sess.expireServer();
            cleanupActiveSession(sess, false);
            return;
        }

        // allow traffic and clean up the session
        if (action == TrafficAction.RELEASE) {
            cleanupActiveSession(sess, false);
        }

        super.handleUDPClientPacket(sess, data, header);
    }

    /**
     * This function handles processing raw session traffic from the server
     * 
     * @param sess
     *        The session
     * @param data
     *        The raw client data
     * @param header
     *        theh IP packet header
     */
    @Override
    public void handleUDPServerPacket(AppUDPSession sess, ByteBuffer data, IPPacketHeader header)
    {
        ApplicationControlStatus status = (ApplicationControlStatus) sess.attachment();

        // before we do anything else see if this session is already set
        // for tarpit and if so drop the traffic on the floor
        if (status.tarpit == true) return;

        TrafficAction action = processTraffic(false, status, sess, data);

        // set the tarpit flag and block the traffic
        if (action == TrafficAction.TARPIT) {
            status.tarpit = true;
            return;
        }

        // expire both sides and clean up the session
        if (action == TrafficAction.BLOCK) {
            sess.expireClient();
            sess.expireServer();
            cleanupActiveSession(sess, false);
            return;
        }

        // allow traffic and clean up the session
        if (action == TrafficAction.RELEASE) {
            cleanupActiveSession(sess, false);
        }

        super.handleUDPServerPacket(sess, data, header);
    }

    /**
     * This common function handles all new TCP and UDP sessions
     * 
     * @param ipr
     *        The IP session request
     */
    public void processNewSession(IPNewSessionRequest ipr)
    {
        // if the license is not valid we ignore all traffic
        if (app.isLicenseValid() != true) {
            ipr.release();
            return;
        }

        // create string to allocate the session in the daemon
        // string format = CMD|ID|PROTOCOL|C_ADDR|C_PORT|S_ADDR|S_PORT
        // string sample = CREATE|123456789|TCP|192.168.1.1|55555|4.3.2.1|80
        String sessionInfo = getIdString(ipr.id());
        if (ipr instanceof TCPNewSessionRequest) sessionInfo = (sessionInfo + "|TCP");
        if (ipr instanceof UDPNewSessionRequest) sessionInfo = (sessionInfo + "|UDP");
        sessionInfo = (sessionInfo + "|" + ipr.getOrigClientAddr().getHostAddress() + "|" + ipr.getOrigClientPort());
        sessionInfo = (sessionInfo + "|" + ipr.getNewServerAddr().getHostAddress() + "|" + ipr.getNewServerPort());

        // create new status object and attach to session
        String message = "CREATE|" + sessionInfo + "\r\n";
        ApplicationControlStatus status = new ApplicationControlStatus(sessionInfo, ipr);
        ipr.attach(status);

        // pass the session create string to the daemon and allow the traffic
        daemonCommand(message, null);
        return;
    }

    /**
     * This function passes the raw traffic to the classd daemon, tracks the
     * classification for the session, looks at rules and settings, and returns
     * an appropriate action code.
     * 
     * @param isClient
     *        True for client data, false for server data
     * @param status
     *        Our status object attached to the session
     * @param sess
     *        The session object
     * @param data
     *        The raw traffic
     * @return The action to be taken for the traffic
     */
    private TrafficAction processTraffic(boolean isClient, ApplicationControlStatus status, AppSession sess, ByteBuffer data)
    {
        // create string to pass session data to the classd daemon
        // string format = CMD|ID|DATALEN
        // string sample = CLIENT|123456789|437
        String message = (isClient ? "CLIENT|" : "SERVER|") + getIdString(status.sessionId) + "|" + Integer.toString(data.limit()) + "\r\n";

        // add 1 to the chunk count
        // if we've scanned more chunks than allowed, just give up on categorization
        // go ahead and evaluate the logic rules with what we have
        status.chunkCount++;
        if (status.chunkCount >= MAX_CHUNK_COUNT) {
            logger.debug("Max chunk count reached. Giving up and releasing session. " + status.toString());
            return processLogicRules(isClient, status, sess, data);
        }

        // pass the session data to the daemon and get the status in return
        // null response means the daemon is still scanning so we must allow
        String traffic = daemonCommand(message, data.duplicate());
        if (traffic == null) return (TrafficAction.ALLOW);

        // update the status object with the daemon result
        ApplicationControlStatus.StatusCode check = status.updateStatus(traffic);

        // this call gets and clears the number of status members that were
        // just updated which we use to make the debug log less noisy
        if (status.getChangeCount() != 0) logger.debug("STATUS = " + status.toString());

        // if we detect a failure parsing the daemon response then
        // something is really screwed up so log an event and release
        if (check == ApplicationControlStatus.StatusCode.FAILURE) {
            ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, null, null, false, false);
            app.logStatusEvent(evt, "FAILURE");
            logger.warn("Error processing daemon response - " + traffic);
            return (TrafficAction.RELEASE);
        }

        // if we didn't get any classification data from the daemon allow
        // the traffic and hold onto the stream for further inspection.
        if (check == ApplicationControlStatus.StatusCode.EMPTY) {
            return (TrafficAction.ALLOW);
        }

        // update the global attachments
        sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_APPLICATION, status.application);
        sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_PROTOCHAIN, status.protochain);
        sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_DETAIL, status.detail);
        sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_CONFIDENCE, status.confidence);

        // search for the application in the rules hashtable
        ApplicationControlProtoRule protoRule = app.settings.searchProtoRules(status.application);

        // match found so see if we need to block or flag
        if (protoRule != null) {
            sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_CATEGORY, protoRule.getCategory());
            sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_PRODUCTIVITY, protoRule.getProductivity());
            sess.globalAttach(AppSession.KEY_APPLICATION_CONTROL_RISK, protoRule.getRisk());

            // first we handle protocols set for block
            if (protoRule.getBlock() == true) {
                this.app.incrementMetric(ApplicationControlApp.STAT_BLOCK);
                app.statistics.IncrementBlockedCount();
                if (protoRule.getFlag() == true) {
                    this.app.incrementMetric(ApplicationControlApp.STAT_FLAG);
                    app.statistics.IncrementFlaggedCount();
                }
                logger.debug("BLOCK ProtoRule " + status.toString());
                ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, protoRule);
                app.logStatusEvent(evt, "ProtoBlock");
                return (TrafficAction.BLOCK);
            }

            // next we handle protocols set for tarpit
            if (protoRule.getTarpit() == true) {
                this.app.incrementMetric(ApplicationControlApp.STAT_BLOCK);
                app.statistics.IncrementBlockedCount();
                if (protoRule.getFlag() == true) {
                    this.app.incrementMetric(ApplicationControlApp.STAT_FLAG);
                    app.statistics.IncrementFlaggedCount();
                }
                logger.debug("TARPIT ProtoRule " + status.toString());
                ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, protoRule);
                app.logStatusEvent(evt, "ProtoTarpit");
                return (TrafficAction.TARPIT);
            }

            // if only flag is set we assume they want to know about the
            // traffic but not interfere so we log the event and release
            if (protoRule.getFlag() == true) {
                this.app.incrementMetric(ApplicationControlApp.STAT_FLAG);
                app.statistics.IncrementFlaggedCount();
                ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, protoRule);
                app.logStatusEvent(evt, "ProtoFlag");
                return (TrafficAction.RELEASE);
            }
        }

        // if classification is still in progress we allow the traffic
        // but hold onto the stream for further inspection.
        if ((status.state != NAVL_STATE_CLASSIFIED) && (status.state != NAVL_STATE_TERMINATED)) {
            return (TrafficAction.ALLOW);
        }

        // At this point we have a packet that is not blocked or flagged and
        // classification is complete so we look for a logic rule match
        return processLogicRules(isClient, status, sess, data);
    }

    /**
     * This function applies our logic rules to fully classified sessions
     * 
     * @param isClient
     *        True for client data, false for server data
     * @param status
     *        Our status object attached to the session
     * @param sess
     *        The session object
     * @param data
     *        The raw traffic
     * @return The action to be taken for the traffic
     */
    private TrafficAction processLogicRules(boolean isClient, ApplicationControlStatus status, AppSession sess, ByteBuffer data)
    {
        ApplicationControlLogicRule logicRule = findLogicRule(sess);
        TrafficAction action = null;
        boolean flag = false;
        Integer ruleid = null;

        // if no logic rule matches then flag the session for release
        if (logicRule == null) {
            action = TrafficAction.RELEASE;
        }

        // otherwise use the action for the first matching rule
        else {
            flag = logicRule.getAction().getFlag();
            ruleid = logicRule.getId();

            switch (logicRule.getAction().getActionType())
            {
            case ALLOW:
                action = TrafficAction.RELEASE;
                break;
            case BLOCK:
                action = TrafficAction.BLOCK;
                break;
            case TARPIT:
                action = TrafficAction.TARPIT;
                break;
            default:
                logger.warn("Unknown action: " + logicRule.getAction().getActionType());
                action = TrafficAction.RELEASE;
                break;
            }
        }

        // use the application to grab the category from the protocol rules
        String category = null;
        ApplicationControlProtoRule categoryRule = app.settings.searchProtoRules(status.application);
        if (categoryRule != null) category = categoryRule.getCategory();

        switch (action)
        {
        case RELEASE:
            this.app.incrementMetric(ApplicationControlApp.STAT_PASS);
            app.statistics.IncrementAllowedCount();
            ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, category, ruleid, flag, false);
            app.logStatusEvent(evt, "RulePass");
            return (TrafficAction.RELEASE);
        case BLOCK:
            this.app.incrementMetric(ApplicationControlApp.STAT_BLOCK);
            app.statistics.IncrementBlockedCount();
            evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, category, ruleid, flag, true);
            app.logStatusEvent(evt, "RuleBlock");
            return (TrafficAction.BLOCK);
        case TARPIT:
            this.app.incrementMetric(ApplicationControlApp.STAT_BLOCK);
            app.statistics.IncrementBlockedCount();
            evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, category, ruleid, flag, true);
            app.logStatusEvent(evt, "RuleTarpit");
            return (TrafficAction.TARPIT);
        default:
            logger.warn("Unknown action: " + action);
            this.app.incrementMetric(ApplicationControlApp.STAT_PASS);
            app.statistics.IncrementAllowedCount();
            evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, category, ruleid, flag, false);
            app.logStatusEvent(evt, "RuleUnknown");
            return (TrafficAction.RELEASE);
        }
    }

    /**
     * Called to clean up sessions that have been blocked, released, or
     * finalized
     * 
     * @param sess
     *        The session object
     * @param isFinalized
     *        True if were called from the TCP or UDP finalize handler
     */
    public void cleanupActiveSession(AppSession sess, boolean isFinalized)
    {
        ApplicationControlStatus status = (ApplicationControlStatus) sess.attachment();

        // if status object is empty we can bail out now
        if (status == null) return;

        // status object is valid so we have to do cleanup
        sess.attach(null);
        sess.release();

        // create string to remove the session in the daemon
        // string format = CMD|ID
        // string sample = REMOVE|123456789
        String message = "REMOVE|" + getIdString(sess.id()) + "\r\n";

        // pass the session remove string to the daemon
        daemonCommand(message, null);

        // If the isFinalized flag is true then we are being called from one
        // of the handleXXXFinalized functions. This normally indicates
        // a session that was never flagged or blocked and never reached
        // the fully classified state, so we count and log those here.
        // However, sessions we tarpit will also show up this way since we
        // never release them, but they were already logged and counted so
        // we have to look at the tarpit flag also.
        if ((isFinalized == true) && (status.tarpit == false)) {
            this.app.incrementMetric(ApplicationControlApp.STAT_PASS);
            app.statistics.IncrementAllowedCount();
            ApplicationControlLogEvent evt = new ApplicationControlLogEvent(sess.sessionEvent(), status, null, null, false, false);
            app.logStatusEvent(evt, "FINALIZE");
        }
    }

    /**
     * Public function to send commands to the classd daemon. It takes care of
     * logging and syncronization, and will reconnect to the daemon if the
     * socket is disconnected.
     * 
     * @param message
     *        The message to transmit to the daemon
     * @param buffer
     *        The raw network data if command is to classify traffic
     * @return The response from the daemon
     */
    public String daemonCommand(String message, ByteBuffer buffer)
    {
        String result = null;

        if (app.settings.getDaemonDebug()) {
            logger.debug("DAEMON COMMAND = " + message);
            if (buffer != null) logger.debug("DAEMON BUFFER = " + buffer.toString());
        }

        /*
         * We synchronize on the address object to prevent mulitple threads from
         * stepping on each other when passing commands and raw data to the
         * daemon. We use the address rather than the socket since the socket
         * object gets destroyed and recreated during socket recycle.
         */

        synchronized (app.daemonAddress) {
            /*
             * If the daemon socket is null we need to call the startup
             * function. This would only happen if an exception is thrown during
             * a previous call to socketStartup, and it should never happen but
             * we check and handle just in case.
             */
            if (app.daemonSocket == null) app.socketStartup();

            // if we have a good daemon socket object we handle the command
            if (app.daemonSocket != null) result = privateCommand(message, buffer);
        }

        return (result);
    }

    /**
     * Private function to actually transmit commands to the classd daemon and
     * receive the response. For session CREATE and REMOVE commands, we send a
     * single command string. When passing client or server data, we first send
     * the CLIENT or SERVER command, which includes the length of the data,
     * followed by the actual data for classification. The daemon looks for the
     * trailing <CR><LF> and extracts the raw data starting from that location.
     * The daemon reply to all of these commands will be terminated with
     * <CR><LF><CR><LF> so we know to keep reading until we get the entire
     * response.
     * 
     * @param message
     *        The message to transmit to the daemon
     * @param buffer
     *        The raw network data if command is to classify traffic
     * @return The response from the daemon
     */
    /*
     */
    private String privateCommand(String message, ByteBuffer buffer)
    {
        ByteBuffer rxbuffer = ByteBuffer.allocate(1024);
        ByteBuffer txbuffer = ByteBuffer.wrap(message.getBytes());

        try {
            // first we see if the connect request has finished but only
            // if a connection is pending since the connect could have
            // completed and returned true in the initial call
            if (app.daemonSocket.isConnectionPending() == true) {
                if (app.daemonSocket.finishConnect() == false) {
                    logger.warn("The daemon socket has not finished connecting");
                    return (null);
                }
                return (null);
            }

            // next make sure the socket is actually connected
            if (app.daemonSocket.isConnected() == false) {
                socketRecycle("isConnected() returned false", false);
                return (null);
            }

            // loop on the select/write until all the data is transmitted
            do {
                // wait until the socket is ready to transmit
                app.writeSelector.select(SELECT_TIMEOUT);
                if (app.writeKey.isWritable() == false) {
                    socketRecycle("isWritable(message) returned false", false);
                    return (null);
                }

                // transmit the command to the daemon
                app.daemonSocket.write(txbuffer);
            } while (txbuffer.hasRemaining() == true);

            // if the buffer argument is valid we have a chunk of raw
            // client or server traffic that we have to send also so
            // we loop on the select/write until all has been sent
            if (buffer != null) {
                do {
                    // wait until the socket is ready to transmit
                    app.writeSelector.select(SELECT_TIMEOUT);
                    if (app.writeKey.isWritable() == false) {
                        socketRecycle("isWritable(buffer) returned false", false);
                        return (null);
                    }

                    // send the buffer to the daemon
                    app.daemonSocket.write(buffer);
                } while (buffer.hasRemaining() == true);
            }

            // loop on the select/read until we find <CR><LF><CR><LF>
            do {
                // wait until the socket is ready to receive
                app.readSelector.select(SELECT_TIMEOUT);
                if (app.readKey.isReadable() == false) {
                    socketRecycle("isReadable() returned false", false);
                    return (null);
                }

                // read the reply from the daemon
                int size = app.daemonSocket.read(rxbuffer);

                // negative value means the socket disconnected
                if (size == -1) {
                    socketRecycle("read() indicated disconnect", false);
                    return (null);
                }

            } while (markerSearch(rxbuffer) == false);
        }

        catch (ConnectException con) {
            socketRecycle(con.getMessage(), true);
            return (null);
        }

        catch (Exception exn) {
            logger.warn("Exception transmitting daemon command -" + message, exn);
            socketRecycle("exception detected", true);
            return (null);
        }

        // return the result as a string
        String result = new String(rxbuffer.array(), 0, rxbuffer.position());

        // check the that string contains only ascii
        // NGFW-9482
        String result2 = result.replaceAll("[^\\x01-\\x7F]", "");
        if (!result.equals(result2)) {
            logger.warn("classd daemon return non-ascii bytes[" + rxbuffer.position() + "]: " + result);
        }

        return result;
    }

    /**
     * Locates the first matching logic rule for a session
     * 
     * @param sess
     *        The session to use for matching
     * @return The first matching rule or null if no match is found
     */
    private ApplicationControlLogicRule findLogicRule(AppSession sess)
    {
        List<ApplicationControlLogicRule> logicList = this.app.getSettings().getLogicRules();

        logger.debug("Checking Rules against AppSession : " + sess.getProtocol() + " " + sess.getOrigClientAddr().getHostAddress() + ":" + sess.getOrigClientPort() + " -> " + sess.getNewServerAddr().getHostAddress() + ":" + sess.getNewServerPort());

        if (logicList == null) return null;

        for (ApplicationControlLogicRule logicRule : logicList) {
            Boolean result;

            if (!logicRule.getEnabled()) continue;
            result = logicRule.matches(sess);

            if (result == true) {
                logger.debug("MATCHED LogicRule \"" + logicRule.getDescription() + "\"");
                return logicRule;
            }

            else {
                logger.debug("Checked LogicRule \"" + logicRule.getDescription() + "\"");
            }
        }

        return null;
    }

    /**
     * Function to get a session ID string for a session ID value. If no network
     * port value was passed when we were created, we simply use the argumented
     * value. If a specific port was set when we were created, we're likely
     * scanning another stream of session traffic such as HTTP that has been
     * decrypted by the https casing so we add a value in the highest 16 bits of
     * the session id so the daemon can track the flow separately.
     * 
     * @param argValue
     *        The session ID value for the traffic
     * @return A String with the session ID to be passed to the classd daemon
     */
    private String getIdString(long argValue)
    {
        long marker = 2222200000000000000L;

        // if not targeting a specific port then use session id as is
        if (networkPort == 0) return (Long.toString(argValue));

        // specific port is set so add a magic value in the highest 16 bits 
        return (Long.toString(argValue + marker));
    }

    /**
     * Search a buffer for the <CR><LF><CR><LF> marker used to indicating the
     * end of a classd daemon response
     * 
     * @param buffer
     *        The buffer to search
     * @return True if the marker is found in the buffer, otherwise false
     */
    private boolean markerSearch(ByteBuffer buffer)
    {
        byte rawdata[] = buffer.array();
        int max = buffer.position();
        int x;

        for (x = 0; x < buffer.position(); x++) {
            if (rawdata[x + 0] != '\r') continue;

            if ((x + 1) >= max) return (false);
            if (rawdata[x + 1] != '\n') continue;

            if ((x + 2) >= max) return (false);
            if (rawdata[x + 2] != '\r') continue;

            if ((x + 3) >= max) return (false);
            if (rawdata[x + 3] != '\n') continue;
            return (true);
        }

        return (false);
    }

    /**
     * Recycles our socket connection to the classd daemon
     * 
     * @param message
     *        The message to be logged
     * @param force
     *        True if we should ignore a pending connection and force the socket
     *        to be recycled
     */
    private void socketRecycle(String message, boolean force)
    {
        // if connection is already pending we just return
        if ((app.daemonSocket.isConnectionPending() == true) && (force == false)) return;

        // not connecting so destroy the socket and start it back up
        logger.warn("Recycling daemon socket connection: " + message);
        app.socketDestroy();
        app.socketStartup();
    }
}
