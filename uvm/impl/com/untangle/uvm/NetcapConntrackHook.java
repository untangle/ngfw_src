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

    public void event( long session_id, int type, long mark, long conntrack_id,
                       int l3_proto, int l4_proto,
                       long c_client_addr, long c_server_addr,
                       int  c_client_port, int c_server_port,
                       long s_client_addr, long s_server_addr,
                       int  s_client_port, int s_server_port,
                       int c2s_packets, int c2s_bytes,
                       int s2c_packets, int s2c_bytes,
                       long timestamp_start, long timestamp_stop )
    {

        int sourceIntf = (int)mark & 0xff;
        int destIntf = (int)(mark & 0xff00)>>8;
        InetAddress cClientAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( c_client_addr );
        InetAddress cServerAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( c_server_addr );
        InetAddress sClientAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( s_client_addr );
        InetAddress sServerAddr = com.untangle.jnetcap.Inet4AddressConverter.toAddress( s_server_addr );
        
        // if ( type == 1 ) {
        //     Date startDate = new Date(timestamp_start*1000l);
        //     logger.warn("New Session: " +
        //                 " [protocol " + l4_proto + "] " +
        //                 "[" + sourceIntf + "->" + destIntf + "] " +
        //                 cClientAddr.getHostAddress() + ":" + c_client_port +
        //                 " -> " +
        //                 sServerAddr.getHostAddress() + ":" + s_server_port +
        //                 " [" + startDate + "]"); 
        // }
        // if ( type == 4 ) {
        //     Date endDate = new Date(timestamp_stop*1000l);
        //     logger.warn("End Session: " + "[protocol " + l4_proto + "] " +
        //                 "[" + sourceIntf + "->" + destIntf + "] " +
        //                 cClientAddr.getHostAddress() + ":" + c_client_port +
        //                 " -> " +
        //                 sServerAddr.getHostAddress() + ":" + s_server_port +
        //                 " [" + endDate + "]" +
        //                 " c2s_bytes: " + c2s_bytes +
        //                 " s2c_bytes: " + s2c_bytes +
        //                 " c2s_packets: " + c2s_packets +
        //                 " s2c_packets: " + s2c_packets); 
        // }

        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry( cClientAddr );
        String username = null;
        String hostname = null;
        if ( entry != null ) {
            username = entry.getUsername();
            hostname = entry.getHostname();
        }

        if ((hostname == null || hostname.length() == 0))
            hostname = cClientAddr.getHostAddress();
        
        if ( type == 1 ) { /* New Session */
            SessionEvent sessionEvent =  new SessionEvent( );
            sessionEvent.setSessionId( session_id );
            sessionEvent.setBypassed( true );
            sessionEvent.setProtocol( (short)l4_proto ); 
            sessionEvent.setClientIntf( sourceIntf ); 
            sessionEvent.setServerIntf( destIntf ); 
            sessionEvent.setUsername( username ); 
            sessionEvent.setHostname( hostname ); 
            sessionEvent.setPolicyId( 0l ); 
            sessionEvent.setCClientAddr( cClientAddr ); 
            sessionEvent.setCClientPort( c_client_port ); 
            sessionEvent.setCServerAddr( cServerAddr ); 
            sessionEvent.setCServerPort( c_server_port ); 
            sessionEvent.setSClientAddr( sClientAddr );
            sessionEvent.setSClientPort( s_client_port );
            sessionEvent.setSServerAddr( sServerAddr );
            sessionEvent.setSServerPort( s_server_port );
            UvmContextFactory.context().logEvent( sessionEvent );

            // remember the session Id so we know it when the session ends
            conntrackIdToSessionIdMap.put( conntrack_id, session_id );
        }

        if ( type == 4 ) { /* End Session */
            // fetch the session Id
            session_id = conntrackIdToSessionIdMap.remove( conntrack_id );

            SessionStatsEvent statEvent = new SessionStatsEvent( session_id );
            statEvent.setC2pBytes( c2s_bytes ); 
            statEvent.setP2cBytes( s2c_bytes );
            statEvent.setC2pChunks( c2s_packets );
            statEvent.setP2cChunks( s2c_packets );
            statEvent.setS2pBytes( s2c_bytes );
            statEvent.setP2sBytes( c2s_bytes );
            statEvent.setS2pChunks( s2c_packets );
            statEvent.setP2sChunks( c2s_packets );
            UvmContextFactory.context().logEvent( statEvent );
        }
    }

}
