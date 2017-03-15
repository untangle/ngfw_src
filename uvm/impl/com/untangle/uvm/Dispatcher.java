/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.ByteBuffer;
import java.net.InetAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.AppProperties;
import com.untangle.uvm.node.AppManager;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeUDPSession;
import com.untangle.uvm.vnet.IPPacketHeader;
import com.untangle.uvm.vnet.SessionEventHandler;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.uvm.vnet.UDPNewSessionRequest;

/**
 * One dispatcher per PipelineConnector.  This where all the new session logic
 * lives, and the event dispatching.
 */
public class Dispatcher
{
    public static final String SESSION_ID_MDC_KEY = "SessionID";

    private static final int TCP_READ_BUFFER_SIZE = 8192;
    private static final int UDP_MAX_PACKET_SIZE  = 16384;
    
    protected static final Logger logger = Logger.getLogger( Dispatcher.class );

    private final PipelineConnectorImpl pipelineConnector;
    private final NodeBase node;

    private static final String STAT_LIVE_SESSIONS = "live-sessions";
    private static final String STAT_TCP_LIVE_SESSIONS = "tcp-live-sessions";
    private static final String STAT_UDP_LIVE_SESSIONS = "udp-live-sessions";
    private static final String STAT_SESSIONS     = "sessions";
    private static final String STAT_TCP_SESSIONS = "tcp-sessions";
    private static final String STAT_UDP_SESSIONS = "udp-sessions";
    private static final String STAT_SESSION_REQUESTS     = "session-requests";
    private static final String STAT_TCP_SESSION_REQUESTS = "tcp-session-requests";
    private static final String STAT_UDP_SESSION_REQUESTS = "udp-session-requests";
    
    private SessionEventHandler sessionEventListener;

    /**
     * We need a single global <code>releasedHandler</code> for all
     * sessions that have been release(); ed after session request
     * time.  We use the default abstract event handler implmentation
     * to become a transparent proxy.
     */
    private SessionEventHandler releasedHandler;

    /**
     * The set of active sessions (both TCP and UDP), kept as weak
     * references to SessionState object (both TCP/UDP)
     */
    private ConcurrentHashMap<NodeSessionImpl,NodeSession> liveSessions;

    /**
     * dispatcher is created PipelineConnector.start() when user decides this
     * dispatcher should begin handling a newly connected PipelineConnector.
     *
     * Note that order of initialization is important in here, since
     * the "inner" classes access stuff from us.
     */
    public Dispatcher(PipelineConnectorImpl pipelineConnector) 
    {
        this.pipelineConnector = pipelineConnector;
        this.node = (NodeBase)pipelineConnector.node();
        this.sessionEventListener = null;
        this.releasedHandler = new ReleasedEventHandler(node);
        this.liveSessions = new ConcurrentHashMap<NodeSessionImpl,NodeSession>();

        this.node.addMetric(new NodeMetric(STAT_LIVE_SESSIONS, I18nUtil.marktr("Current Sessions")));
        this.node.addMetric(new NodeMetric(STAT_TCP_LIVE_SESSIONS, I18nUtil.marktr("Current TCP Sessions")));
        this.node.addMetric(new NodeMetric(STAT_UDP_LIVE_SESSIONS, I18nUtil.marktr("Current UDP Sessions")));
        this.node.addMetric(new NodeMetric(STAT_SESSIONS, I18nUtil.marktr("Sessions")));
        this.node.addMetric(new NodeMetric(STAT_TCP_SESSIONS, I18nUtil.marktr("TCP Sessions")));
        this.node.addMetric(new NodeMetric(STAT_UDP_SESSIONS, I18nUtil.marktr("UDP Sessions")));
        this.node.addMetric(new NodeMetric(STAT_SESSION_REQUESTS, I18nUtil.marktr("Session Requests")));
        this.node.addMetric(new NodeMetric(STAT_TCP_SESSION_REQUESTS, I18nUtil.marktr("TCP NodeSession Requests")));
        this.node.addMetric(new NodeMetric(STAT_UDP_SESSION_REQUESTS, I18nUtil.marktr("UDP NodeSession Requests")));
    }

    public void killAllSessions()
    {
        UvmContextFactory.context().netcapManager().shutdownMatches( new SessionMatcher() {
                public boolean isMatch( Integer policyId,
                                        short protocol,
                                        int clientIntf, int serverIntf,
                                        InetAddress clientAddr, InetAddress serverAddr,
                                        int clientPort, int serverPort,
                                        Map<String,Object> attachments )
                {
                    return true;
                }
            }, pipelineConnector );
    }
    
    // Called by the new session handler thread.
    protected void addSession( NodeTCPSessionImpl sess ) 
    {
        this.node.incrementMetric(STAT_LIVE_SESSIONS);
        this.node.incrementMetric(STAT_TCP_LIVE_SESSIONS);

        this.node.incrementMetric(STAT_SESSIONS);
        this.node.incrementMetric(STAT_TCP_SESSIONS);
        
        liveSessions.put(sess, sess);
    }

    // Called by the new session handler thread.
    protected void addSession( NodeUDPSessionImpl sess ) 
    {
        this.node.incrementMetric(STAT_LIVE_SESSIONS);
        this.node.incrementMetric(STAT_UDP_LIVE_SESSIONS);

        this.node.incrementMetric(STAT_SESSIONS);
        this.node.incrementMetric(STAT_UDP_SESSIONS);

        liveSessions.put(sess, sess);
    }

    public NodeTCPSession newSession( TCPNewSessionRequestImpl request )
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(node.getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, "TCP_" + request.id());
            return newSessionInternal(request);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }

    public NodeUDPSession newSession( UDPNewSessionRequestImpl request )
    {
        try {
            UvmContextImpl.getInstance().loggingManager().setLoggingNode(node.getAppSettings().getId());
            MDC.put(SESSION_ID_MDC_KEY, "UDP_" + request.id());
            return newSessionInternal(request);
        } finally {
            UvmContextImpl.getInstance().loggingManager().setLoggingUvm();
            MDC.remove(SESSION_ID_MDC_KEY);
        }
    }
    
    // Called by NodeSessionImpl at closeFinal (raze) time.
    protected void removeSession( NodeSessionImpl sess )
    {
        this.node.decrementMetric(STAT_LIVE_SESSIONS);
        if (sess instanceof NodeUDPSession) {
            this.node.decrementMetric(STAT_UDP_LIVE_SESSIONS);
        } else if (sess instanceof NodeTCPSession) {
            this.node.decrementMetric(STAT_TCP_LIVE_SESSIONS);
        }

        liveSessions.remove(sess);

        if (pipelineConnector == null) {
            logger.warn("attempt to remove session " + sess.id() + " when already destroyed");
        } else {
            pipelineConnector.removeSession( sess );
        }
    }

    protected PipelineConnectorImpl pipelineConnector()
    {
        return pipelineConnector;
    }

    protected void setSessionEventHandler( SessionEventHandler listener )
    {
        sessionEventListener = listener;
    }

    protected long[] liveSessionIds()
    {
        int count = 0;
        int size = liveSessions.size();
        long[] idlist = new long[size];
        
        for (Iterator<NodeSessionImpl> i = liveSessions.keySet().iterator(); i.hasNext(); ) {
            if (count == size) /* just in case */
                break;
            NodeSession sess = i.next();
            idlist[count] = sess.id();
            count++;
        }

        return idlist;
    }

    protected List<NodeSession> liveSessions()
    {
        LinkedList<NodeSession> sessions = new LinkedList<NodeSession>();
        
        for (Iterator<NodeSessionImpl> i = liveSessions.keySet().iterator(); i.hasNext(); ) {
            sessions.add(i.next());
        }

        return sessions;
    }

    //////////////////////////////////////////////////////////////////

    void dispatchTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        elog(Level.DEBUG, "TCPNewSessionRequest", sessionRequest.id());
        if ( sessionEventListener == null ) {
            releasedHandler.handleTCPNewSessionRequest( sessionRequest );
        } else {
            sessionEventListener.handleTCPNewSessionRequest( sessionRequest );
        }
    }

    void dispatchUDPNewSessionRequest( UDPNewSessionRequest sessionRequest )
    {
        elog(Level.DEBUG, "UDPNewSessionRequest", sessionRequest.id());
        if ( sessionEventListener == null ) {
            releasedHandler.handleUDPNewSessionRequest( sessionRequest );
        } else {
            sessionEventListener.handleUDPNewSessionRequest( sessionRequest );
        }
    }

    void dispatchTCPNewSession( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPNewSession", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPNewSession( session );
        else
            sessionEventListener.handleTCPNewSession( session );
    }

    void dispatchUDPNewSession( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPNewSession", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPNewSession( session );
        else
            sessionEventListener.handleUDPNewSession( session );
    }

    void dispatchTCPClientChunk( NodeTCPSessionImpl session, ByteBuffer data )
    {
        elog(Level.DEBUG, "TCPClientChunk", session.id(), data.remaining());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPClientChunk( session, data );
        } else {
            sessionEventListener.handleTCPClientChunk( session, data );
        }
    }

    void dispatchTCPServerChunk( NodeTCPSessionImpl session, ByteBuffer data )
    {
        elog(Level.DEBUG, "TCPServerChunk", session.id(), data.remaining());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPServerChunk( session, data );
        } else {
            sessionEventListener.handleTCPServerChunk( session, data );
        }
    }
    
    void dispatchTCPClientObject( NodeTCPSessionImpl session, Object obj )
    {
        elog(Level.DEBUG, "TCPClientObject", session.id());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPClientObject( session, obj );
        } else {
            sessionEventListener.handleTCPClientObject( session, obj );
        }
    }
    
    void dispatchTCPServerObject( NodeTCPSessionImpl session, Object obj )
    {
        elog(Level.DEBUG, "TCPServerObject", session.id());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPServerObject( session, obj );
        } else {
            sessionEventListener.handleTCPServerObject( session, obj );
        }
    }

    void dispatchTCPClientWritable( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPClientWritable", session.id());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPClientWritable( session );
        } else {
            sessionEventListener.handleTCPClientWritable( session );
        }
    }

    void dispatchTCPServerWritable( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPServerWritable", session.id());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleTCPServerWritable( session );
        } else {
            sessionEventListener.handleTCPServerWritable( session );
        }
    }

    void dispatchUDPClientPacket( NodeUDPSessionImpl session, ByteBuffer data, IPPacketHeader header  )
    {
        elog(Level.DEBUG, "UDPClientPacket", session.id(), data.remaining());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleUDPClientPacket( session, data, header );
        } else {
            sessionEventListener.handleUDPClientPacket( session, data, header );
        }
    }

    void dispatchUDPServerPacket( NodeUDPSessionImpl session, ByteBuffer data, IPPacketHeader header  )
    {
        elog(Level.DEBUG, "UDPServerPacket", session.id(), data.remaining());
        if ( sessionEventListener == null || session.released() ) {
            releasedHandler.handleUDPServerPacket( session, data, header );
        } else {
            sessionEventListener.handleUDPServerPacket( session, data, header );
        }
    }

    void dispatchTCPClientDataEnd( NodeTCPSessionImpl session, ByteBuffer data )
    {
        elog(Level.DEBUG, "TCPClientDataEnd", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPClientDataEnd( session, data );
        else
            sessionEventListener.handleTCPClientDataEnd( session, data );
    }

    void dispatchTCPClientFIN( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPClientFIN", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPClientFIN( session );
        else
            sessionEventListener.handleTCPClientFIN( session );
    }

    void dispatchTCPServerDataEnd( NodeTCPSessionImpl session, ByteBuffer data )
    {
        elog(Level.DEBUG, "TCPServerDataEnd", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPServerDataEnd( session, data );
        else
            sessionEventListener.handleTCPServerDataEnd( session, data );
    }

    void dispatchTCPServerFIN( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPServerFIN", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPServerFIN( session );
        else
            sessionEventListener.handleTCPServerFIN( session );
    }

    void dispatchTCPClientRST( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPClientRST", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPClientRST( session );
        else
            sessionEventListener.handleTCPClientRST( session );
    }

    void dispatchTCPServerRST( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPServerRST", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPServerRST( session );
        else
            sessionEventListener.handleTCPServerRST( session );
    }

    void dispatchTCPFinalized( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPFinalized", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPFinalized( session );
        else
            sessionEventListener.handleTCPFinalized( session );
    }

    void dispatchTCPComplete( NodeTCPSessionImpl session )
    {
        elog(Level.DEBUG, "TCPComplete", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTCPComplete( session );
        else
            sessionEventListener.handleTCPComplete( session );
    }

    void dispatchUDPClientExpired( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPClientExpired", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPClientExpired( session );
        else
            sessionEventListener.handleUDPClientExpired( session );
    }

    void dispatchUDPServerExpired( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPServerExpired", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPServerExpired( session );
        else
            sessionEventListener.handleUDPServerExpired( session );
    }

    void dispatchUDPClientWritable( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPClientWritable", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPClientWritable( session );
        else
            sessionEventListener.handleUDPClientWritable( session );
    }

    void dispatchUDPServerWritable( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPServerWritable", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPServerWritable( session );
        else
            sessionEventListener.handleUDPServerWritable( session );
    }

    void dispatchUDPFinalized( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPFinalized", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPFinalized( session );
        else
            sessionEventListener.handleUDPFinalized( session );
    }

    void dispatchUDPComplete( NodeUDPSessionImpl session )
    {
        elog(Level.DEBUG, "UDPComplete", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleUDPComplete( session );
        else
            sessionEventListener.handleUDPComplete( session );
    }

    void dispatchTimer( NodeSessionImpl session )
    {
        elog(Level.DEBUG, "Timer", session.id());
        if ( sessionEventListener == null || session.released() )
            releasedHandler.handleTimer( session );
        else
            sessionEventListener.handleTimer( session );
    }

    private NodeTCPSession newSessionInternal( TCPNewSessionRequestImpl request )
    {
        long sessionId = -1L;

        try {
            sessionId = request.id();

            this.node.incrementMetric(STAT_SESSION_REQUESTS);
            this.node.incrementMetric(STAT_TCP_SESSION_REQUESTS);

            // Give the request event to the user, to give them a chance to reject the session.
            logger.debug("sending TCP new session request event");
            dispatchTCPNewSessionRequest( request );

            // Check the session only if it was not rejected.
            switch (request.state()) {
            case IPNewSessionRequestImpl.REJECTED:
            case IPNewSessionRequestImpl.REJECTED_SILENT:
                logger.debug("rejecting");
                return null;

            case IPNewSessionRequestImpl.RELEASED:
                logger.debug("releasing");
                return null;

            case IPNewSessionRequestImpl.REQUESTED:
            case IPNewSessionRequestImpl.ENDPOINTED:
            default:
                break;
            }

            // Create the session, client and server channels
            NodeTCPSessionImpl session = new NodeTCPSessionImpl(this, request.sessionEvent(), TCP_READ_BUFFER_SIZE, TCP_READ_BUFFER_SIZE, request);

            request.copyAttachments( session );
            //session.attach(request.attachment());

            // Send the new session event.  
            if (logger.isDebugEnabled())
                logger.debug("New TCP session " +
                             session.getOrigClientAddr().getHostAddress() + ":" + session.getOrigClientPort() + " -> " +
                             session.getNewServerAddr().getHostAddress() + ":" + session.getNewServerPort());
            if (request.state() == IPNewSessionRequestImpl.RELEASED) {
                session.release();
            } else {
                dispatchTCPNewSession( session );
            }

            // Finally add it to our set of owned sessions.
            addSession(session);

            return session;
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " building TCP session " + sessionId + " : ";
            logger.error(message, x);
            // kill the session by returning null
            return null;
        }
    }

    private NodeUDPSession newSessionInternal( UDPNewSessionRequestImpl request )
    {
        long sessionId = -1;

        try {
            sessionId = request.id();

            this.node.incrementMetric(STAT_SESSION_REQUESTS);
            this.node.incrementMetric(STAT_UDP_SESSION_REQUESTS);

            // Give the request event to the user, to give them a chance to reject the session.
            logger.debug("sending UDP new session request event");
            dispatchUDPNewSessionRequest( request );

            // Check the session only if it was not rejected.
            switch (request.state()) {
            case IPNewSessionRequestImpl.REJECTED:
            case IPNewSessionRequestImpl.REJECTED_SILENT:
                logger.debug("rejecting");
                return null;

            case IPNewSessionRequestImpl.RELEASED:
                logger.debug("releasing");
                return null;

            case IPNewSessionRequestImpl.REQUESTED:
            case IPNewSessionRequestImpl.ENDPOINTED:
            default:
                break;
            }

            // Create the session, client and server channels
            NodeUDPSessionImpl session = new NodeUDPSessionImpl(this, request.sessionEvent(), UDP_MAX_PACKET_SIZE, UDP_MAX_PACKET_SIZE, request);

            session.attach(request.attachment());

            // Send the new session event.  Maybe this should be done on the session handler
            // thread instead?  XXX
            if (logger.isDebugEnabled())
                logger.debug("New UDP session " +
                             session.getOrigClientAddr().getHostAddress() + ":" + session.getOrigClientPort() + " -> " +
                             session.getNewServerAddr().getHostAddress() + ":" + session.getNewServerPort());
            if (request.state() == IPNewSessionRequestImpl.RELEASED) {
                session.release();
            } else {
                dispatchUDPNewSession( session );
            }

            // Finally add it to our set of owned sessions.
            addSession(session);

            return session;
        } catch (Exception x) {
            String message = "" + x.getClass().getName() + " building UDP session " + sessionId + " : ";
            logger.error(message, x);
            // kill the session by returning null
            return null;
        }
    }

    private void elog( Level level, String eventName, long sessionId )
    {
        if ( logger.isEnabledFor(level) ) {
            StringBuilder message = new StringBuilder("EV[");
            message.append(sessionId);
            message.append(",");
            message.append(eventName);
            message.append("] T: ");
            message.append(Thread.currentThread().getName());

            logger.log(level, message.toString());
        }
    }

    private void elog( Level level, String eventName, long sessionId, long dataSize )
    {
        if ( logger.isEnabledFor(level) ) {
            StringBuilder message = new StringBuilder("EV[");
            message.append(sessionId);
            message.append(",");
            message.append(eventName);
            message.append("] S: ");
            message.append(dataSize);
            message.append(" T: ");
            message.append(Thread.currentThread().getName());

            logger.log(level, message.toString());
        }
    }
    
}
