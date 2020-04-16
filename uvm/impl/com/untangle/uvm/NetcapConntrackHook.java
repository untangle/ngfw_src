/**
 * $Id: NetcapConntrackHook.java 38299 2014-08-06 03:03:17Z dmorris $
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapCallback;
import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.app.SessionEvent;
import com.untangle.uvm.app.SessionStatsEvent;
import com.untangle.uvm.app.SessionTuple;

/**
 * NetcapConntrackHook is the global conntrack hook for netcap
 */
public class NetcapConntrackHook implements NetcapCallback
{
    private static final int BYPASS_MARK = 0x01000000;

    private static final int CONNTRACK_TYPE_NEW = 1;
    private static final int CONNTRACK_TYPE_END = 4;
    
    private static NetcapConntrackHook INSTANCE;

    private final Logger logger = Logger.getLogger(getClass());

    private HashMap<SessionTuple,Long> conntrackSessionIdMap = new HashMap<>();
    
    /**
     * getInstance gets the singleton instance
     * @return singleton
     */
    public static NetcapConntrackHook getInstance()
    {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /**
     * Singleton
     */
    private NetcapConntrackHook() {}

    /**
     * init creates the instancen
     */
    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new NetcapConntrackHook();
    }

    /**
     * event
     * @param sessionId
     */
    public void event( long sessionId ) {}

    /**
     * lookupSessionId - lookup the session ID in the conntrack map
     * @param tuple 
     * @return Session ID
     */
    public Long lookupSessionId( SessionTuple tuple )
    {
        return conntrackSessionIdMap.get( tuple );
    }
    
    /**
     * event - process an conntrack event
     * @param conntrackPtr
     * @param type
     */
    public void event( long conntrackPtr, int type )
    {
        if ( conntrackPtr == 0 ) {
            logger.warn("Invalid arguments: conntrackPtr is NULL");
            return;
        }

        Conntrack ct = new Conntrack(conntrackPtr);

        try {
            int mark = ct.getMark();
            int clientIntf = ct.getClientIntf();
            int serverIntf = ct.getServerIntf();
            int protocol = ct.getProtocol();
            SessionTuple tuple = new SessionTuple( ct.getProtocol(),
                                                           ct.getPreNatClient(),
                                                           ct.getPreNatServer(),
                                                           ct.getPreNatClientPort(),
                                                           ct.getPreNatServerPort(),
                                                           clientIntf,
                                                           serverIntf );

            boolean logEvent = UvmContextFactory.context().networkManager().getNetworkSettings().getLogBypassedSessions();
            
            // if its TCP and not bypassed, the event will be logged elsewhere
            if( protocol == 6 && (mark & BYPASS_MARK) != BYPASS_MARK )
                return;
            // if its UDP and not bypassed, the event will be logged elsewhere
            if( protocol == 17 && (mark & BYPASS_MARK) != BYPASS_MARK )
                return;

            // clientIntf == 0 means its from the local server
            // must check clientIntf first, because outbound traffic is 0->0
            if ( clientIntf == 0 ) {
                if ( ! UvmContextFactory.context().networkManager().getNetworkSettings().getLogLocalOutboundSessions() ) {
                    return;
                }
            }
            // serverIntf == 0 means its to the local server
            else if ( serverIntf == 0 ) {
                if ( ! UvmContextFactory.context().networkManager().getNetworkSettings().getLogLocalInboundSessions() ) {
                    return;
                }
            }
        
            InetAddress cClientAddr = ct.getPreNatClient();
            InetAddress cServerAddr = ct.getPreNatServer();
            InetAddress sClientAddr = ct.getPostNatClient();
            InetAddress sServerAddr = ct.getPostNatServer();

            int cClientPort = ct.getPreNatClientPort();
            int cServerPort = ct.getPreNatServerPort();
            int sClientPort = ct.getPostNatClientPort();
            int sServerPort = ct.getPostNatServerPort();

            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( cClientAddr );
            String username = null;
            String hostname = null;
            if ( entry != null ) {
                username = entry.getUsername();
                hostname = entry.getHostname();
            }

            if ( hostname == null || hostname.length() == 0 ) {
                hostname = SessionEvent.determineBestHostname( cClientAddr, clientIntf, sServerAddr, serverIntf );
            }
        
            if ( type == CONNTRACK_TYPE_NEW ) { /* New Session */
                long sessionId = com.untangle.jnetcap.Netcap.nextSessionId(); /* create new session ID */
                if ( logEvent ) {
                    SessionEvent sessionEvent =  new SessionEvent( );
                    if ( logger.isDebugEnabled() ) {
                        logger.debug("New Session: [" + sessionId + "]" +
                                     " [protocol " + protocol + "] " +
                                     "[" + clientIntf + "->" + serverIntf + "] " +
                                     cClientAddr.getHostAddress() + ":" + cClientPort +
                                     " -> " +
                                     sServerAddr.getHostAddress() + ":" + sServerPort);
                    }
                    sessionEvent.setSessionId( sessionId );
                    sessionEvent.setBypassed( true );
                    sessionEvent.setProtocol( (short)protocol ); 
                    if ( protocol == 1 )
                        sessionEvent.setIcmpType( (short)ct.getIcmpType() );
                    sessionEvent.setClientIntf( clientIntf ); 
                    sessionEvent.setServerIntf( serverIntf ); 
                    sessionEvent.setUsername( username ); 
                    sessionEvent.setHostname( hostname ); 
                    sessionEvent.setPolicyId( 0 ); 
                    sessionEvent.setCClientAddr( cClientAddr ); 
                    sessionEvent.setCClientPort( cClientPort ); 
                    sessionEvent.setCServerAddr( cServerAddr ); 
                    sessionEvent.setCServerPort( cServerPort ); 
                    sessionEvent.setSClientAddr( sClientAddr );
                    sessionEvent.setSClientPort( sClientPort );
                    sessionEvent.setSServerAddr( sServerAddr );
                    sessionEvent.setSServerPort( sServerPort );

                    /**
                     * If the client is on a WAN, the the local address is the server address
                     * If not, then the local address is the client
                     */
                    if ( clientIntf != 0 && UvmContextFactory.context().networkManager().isWanInterface( clientIntf ) ) {
                        sessionEvent.setLocalAddr( sServerAddr );
                        sessionEvent.setRemoteAddr( cClientAddr );
                    } else {
                        sessionEvent.setLocalAddr( cClientAddr );
                        sessionEvent.setRemoteAddr( sServerAddr );
                    }

                    UvmContextFactory.context().logEvent( sessionEvent );
                }
                
                // remember the session Id so we know it when the session ends
                conntrackSessionIdMap.put( tuple, sessionId );
            }

            if ( type == CONNTRACK_TYPE_END ) { /* End Session */
                // fetch the session Id
                Long sessionId = conntrackSessionIdMap.remove( tuple );

                /**
                 * UDP supports bypassing the session mid-session
                 * As such, the session ID might not be in the table
                 * because we only store the bypassed session IDs in that table
                 * If we couldn't find it, check the session table to see
                 * if this is a session we bypassed mid-session.
                 */
                if ( sessionId == null && protocol == 17 ) {
                    SessionGlobalState session = SessionTableImpl.getInstance().remove( (short)protocol, clientIntf, serverIntf, cClientAddr, cServerAddr, cClientPort, cServerPort );
                    if ( session != null ) {
                        sessionId = session.id();
                        // we set this to true, because we always want to log the end of this session
                        // even if "log bypassed" is not enabled.
                        // this session was not originally bypassed
                        logEvent = true;
                    } 
                }
                
                long c2s_bytes = ct.getOriginalCounterBytes();
                long s2c_bytes = ct.getReplyCounterBytes();
                long c2s_packets = ct.getOriginalCounterPackets();
                long s2c_packets = ct.getReplyCounterPackets();
                
                if ( logger.isDebugEnabled() ) {
                    logger.debug("End Session: [" + sessionId + "] " +
                                 "[protocol " + protocol + "] " +
                                 "[" + clientIntf + "->" + serverIntf + "] " +
                                 cClientAddr.getHostAddress() + ":" + cClientPort +
                                 " -> " +
                                 sServerAddr.getHostAddress() + ":" + sServerPort +
                                 " c2s_bytes: " + c2s_bytes +
                                 " s2c_bytes: " + s2c_bytes +
                                 " c2s_packets: " + c2s_packets +
                                 " s2c_packets: " + s2c_packets); 
                }

                // dont know session id (session probably started before hook was installed)
                // nothing to log
                if ( sessionId == null )
                    return;
                
                if ( logEvent ) {
                    SessionStatsEvent statEvent = new SessionStatsEvent( sessionId, ct.getTimeStampStartMillis() );
                    statEvent.setC2pBytes( c2s_bytes ); 
                    statEvent.setP2cBytes( s2c_bytes );
                    //statEvent.setC2pChunks( c2s_packets );
                    //statEvent.setP2cChunks( s2c_packets );
                    statEvent.setS2pBytes( s2c_bytes );
                    statEvent.setP2sBytes( c2s_bytes );
                    //statEvent.setS2pChunks( s2c_packets );
                    //statEvent.setP2sChunks( c2s_packets );
                    UvmContextFactory.context().logEvent( statEvent );
                }
            }
        }
        catch (Exception e) {
            logger.warn("Exception in Conntrack Hook.",e);
        } finally {
            ct.raze(); /* free the conntrack entry */
        }
    }

}
