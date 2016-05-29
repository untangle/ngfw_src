/**
 * $Id: NetcapConntrackHook.java 38299 2014-08-06 03:03:17Z dmorris $
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapCallback;
import com.untangle.jnetcap.Conntrack;
import com.untangle.uvm.node.SessionEvent;
import com.untangle.uvm.node.SessionStatsEvent;

public class NetcapConntrackHook implements NetcapCallback
{
    private static final int BYPASS_MARK = 0x01000000;

    private static final int CONNTRACK_TYPE_NEW = 1;
    private static final int CONNTRACK_TYPE_END = 4;
    
    private static NetcapConntrackHook INSTANCE;

    private final Logger logger = Logger.getLogger(getClass());

    private HashMap<Long,Long> conntrackIdToSessionIdMap = new HashMap<Long, Long>();
    
    public static NetcapConntrackHook getInstance()
    {
        if ( INSTANCE == null )
            init();

        return INSTANCE;
    }

    /* Singleton */
    private NetcapConntrackHook() {}

    private static synchronized void init()
    {
        if ( INSTANCE == null )
            INSTANCE = new NetcapConntrackHook();
    }

    public void event( long sessionId ) {}

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

            long sessionId = ct.getSessionId();
            long conntrackId = ct.getConntrackId();
            
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( cClientAddr );
            String username = null;
            String hostname = null;
            if ( entry != null ) {
                username = entry.getUsername();
                hostname = entry.getHostname();
            }

            if ((hostname == null || hostname.length() == 0))
                hostname = cClientAddr.getHostAddress();
        
            if ( type == CONNTRACK_TYPE_NEW ) { /* New Session */
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
                    UvmContextFactory.context().logEvent( sessionEvent );
                }
                
                // remember the session Id so we know it when the session ends
                conntrackIdToSessionIdMap.put( conntrackId, sessionId );
            }

            if ( type == CONNTRACK_TYPE_END ) { /* End Session */
                // fetch the session Id
                Long sess_id = conntrackIdToSessionIdMap.remove( conntrackId );
                if ( sess_id != null && sessionId != 0 && sess_id != sessionId ) {
                    logger.warn("Mismatched ID: " + sessionId + " != " + sess_id );
                }

                /**
                 * UDP supports bypassing the session mid-session
                 * As such, the session ID might not be in the table
                 * because we only store the bypassed session IDs in that table
                 * If we couldn't find it, check the session table to see
                 * if this is a session we bypassed mid-session.
                 */
                if ( sess_id == null && protocol == 17 ) {
                    SessionGlobalState session = SessionTable.getInstance().remove( (short)protocol, clientIntf, serverIntf, cClientAddr, sServerAddr, cClientPort, sServerPort );
                    if ( session != null ) {
                        sess_id = session.id();
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
                    logger.debug("End Session: [" + sess_id + "] " +
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
                if ( sess_id == null )
                    return;
                
                if ( logEvent ) {
                    SessionStatsEvent statEvent = new SessionStatsEvent( sess_id );
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
