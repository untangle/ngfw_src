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

    public void event( int type, long mark, long conntrack_id, long session_id, 
                       int l3_proto, int l4_proto, int icmp_type,
                       long c_client_addr, long c_server_addr,
                       int  c_client_port, int c_server_port,
                       long s_client_addr, long s_server_addr,
                       int  s_client_port, int s_server_port,
                       int c2s_packets, int c2s_bytes,
                       int s2c_packets, int s2c_bytes,
                       long timestamp_start, long timestamp_stop )
    {
        try {
            int srcIntf = (int)mark & 0xff;
            int dstIntf = (int)(mark & 0xff00)>>8;
            boolean logEvent = UvmContextFactory.context().networkManager().getNetworkSettings().getLogBypassedSessions();
            
            // if its TCP and not bypassed, the event will be logged elsewhere
            if( l4_proto == 6 && (mark & BYPASS_MARK) != BYPASS_MARK )
                return;
            // if its UDP and not bypassed, the event will be logged elsewhere
            if( l4_proto == 17 && (mark & BYPASS_MARK) != BYPASS_MARK )
                return;
        
            // srcIntf == 0 means its from the local server
            // must check srcIntf first, because outbound traffic is 0->0
            if ( srcIntf == 0 ) {
                if ( ! UvmContextFactory.context().networkManager().getNetworkSettings().getLogLocalOutboundSessions() ) {
                    return;
                }
            }
            // dstIntf == 0 means its to the local server
            else if ( dstIntf == 0 ) {
                if ( ! UvmContextFactory.context().networkManager().getNetworkSettings().getLogLocalInboundSessions() ) {
                    return;
                }
            }
        
            InetAddress cClientAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( c_client_addr );
            InetAddress cServerAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( c_server_addr );
            InetAddress sClientAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( s_client_addr );
            InetAddress sServerAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( s_server_addr );

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
                        Date startDate = new Date(timestamp_start*1000l);
                        logger.debug("New Session: [" + session_id + "]" +
                                     " [protocol " + l4_proto + "] " +
                                     "[" + srcIntf + "->" + dstIntf + "] " +
                                     cClientAddr.getHostAddress() + ":" + c_client_port +
                                     " -> " +
                                     sServerAddr.getHostAddress() + ":" + s_server_port +
                                     " [" + startDate + "]"); 
                    }
                    sessionEvent.setSessionId( session_id );
                    sessionEvent.setBypassed( true );
                    sessionEvent.setProtocol( (short)l4_proto ); 
                    if ( l4_proto == 1 ) sessionEvent.setIcmpType( (short)icmp_type );
                    sessionEvent.setClientIntf( srcIntf ); 
                    sessionEvent.setServerIntf( dstIntf ); 
                    sessionEvent.setUsername( username ); 
                    sessionEvent.setHostname( hostname ); 
                    sessionEvent.setPolicyId( 0 ); 
                    sessionEvent.setCClientAddr( cClientAddr ); 
                    sessionEvent.setCClientPort( c_client_port ); 
                    sessionEvent.setCServerAddr( cServerAddr ); 
                    sessionEvent.setCServerPort( c_server_port ); 
                    sessionEvent.setSClientAddr( sClientAddr );
                    sessionEvent.setSClientPort( s_client_port );
                    sessionEvent.setSServerAddr( sServerAddr );
                    sessionEvent.setSServerPort( s_server_port );
                    UvmContextFactory.context().logEvent( sessionEvent );
                }
                
                // remember the session Id so we know it when the session ends
                conntrackIdToSessionIdMap.put( conntrack_id, session_id );
            }

            if ( type == CONNTRACK_TYPE_END ) { /* End Session */
                // fetch the session Id
                Long sess_id = conntrackIdToSessionIdMap.remove( conntrack_id );

                /**
                 * UDP supports bypassing the session mid-session
                 * As such, the session ID might not be in the table
                 * because we only store the bypassed session IDs in that table
                 * If we couldn't find it, check the session table to see
                 * if this is a session we bypassed mid-session.
                 */
                if ( sess_id == null && l4_proto == 17 ) {
                    SessionGlobalState session = SessionTable.getInstance().remove( (short)l4_proto, srcIntf, dstIntf, cClientAddr, sServerAddr, c_client_port, s_server_port );
                    if ( session != null ) {
                        sess_id = session.id();
                        // we set this to true, because we always want to log the end of this session
                        // even if "log bypassed" is not enabled.
                        // this session was not originally bypassed
                        logEvent = true;
                    }
                }
                
                if ( logger.isDebugEnabled() ) {
                    Date endDate = new Date(timestamp_stop*1000l);
                    logger.debug("End Session: [" + sess_id + "] " +
                                 "[protocol " + l4_proto + "] " +
                                 "[" + srcIntf + "->" + dstIntf + "] " +
                                 cClientAddr.getHostAddress() + ":" + c_client_port +
                                 " -> " +
                                 sServerAddr.getHostAddress() + ":" + s_server_port +
                                 " [" + endDate + "]" +
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
        }
    }

}
