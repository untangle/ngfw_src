/**
 * $Id: NetcapConntrackHook.java 38299 2014-08-06 03:03:17Z dmorris $
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.untangle.jnetcap.NetcapCallback;
import com.untangle.uvm.node.SessionEvent;

public class NetcapConntrackHook implements NetcapCallback
{
    private static NetcapConntrackHook INSTANCE;
    private final Logger logger = Logger.getLogger(getClass());

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

    public void event( long sessionID )
    {
        // if ( isNewSession ) {
        //     sessionEvent =  new SessionEvent( );
        //     sessionEvent.setSessionId( /* FIXME */ 0 );
        //     sessionEvent.setProtocol( protocol ); // FIXME pass in arguments
        //     sessionEvent.setClientIntf( 1 ); // FIXME pass mark in arguments, extract from mark
        //     sessionEvent.setServerIntf( 2 ); // FIXME pass mark in arguments, extract from mark
        //     sessionEvent.setUsername( username ); // FIXME lookup
        //     sessionEvent.setHostname( hostname ); // FIXME lookup
        //     sessionEvent.setPolicyId( 0 ); 
        //     sessionEvent.setCClientAddr( cClientAddr ); // FIXME pass in arguments
        //     sessionEvent.setCClientPort( cClientPort ); // FIXME pass in arguments
        //     sessionEvent.setCServerAddr( cServerAddr ); // FIXME pass in arguments
        //     sessionEvent.setCServerPort( cServerPort ); // FIXME pass in arguments
        //     sessionEvent.setSClientAddr( sClientAddr ); // FIXME pass in arguments
        //     sessionEvent.setSClientPort( sClientPort ); // FIXME pass in arguments
        //     sessionEvent.setSServerAddr( sServerAddr ); // FIXME pass in arguments
        //     sessionEvent.setSServerPort( sServerPort ); // FIXME pass in arguments
        //     UvmContextFactory.context().logEvent( sessionEvent );
        // }

        // if ( isEndingSession ) {
        //     // FIXME, extract session ID somehow? do we need to save it? if so, how?
        //     SessionStatsEvent statEvent = new SessionStatsEvent( id /* FIXME */ );
        //     statEvent.setC2pBytes(sessionGlobalState.clientSideListener().rxBytes); // FIXME bytes client sent
        //     statEvent.setP2cBytes(sessionGlobalState.clientSideListener().txBytes); // FIXME bytes server sent
        //     statEvent.setC2pChunks(sessionGlobalState.clientSideListener().rxChunks); // FIXME packets client sent
        //     statEvent.setP2cChunks(sessionGlobalState.clientSideListener().txChunks); // FIXME packets server sent
        //     statEvent.setS2pBytes(sessionGlobalState.serverSideListener().rxBytes); // FIXME bytes server sent
        //     statEvent.setP2sBytes(sessionGlobalState.serverSideListener().txBytes); // FIXME bytes client sent
        //     statEvent.setS2pChunks(sessionGlobalState.serverSideListener().rxChunks); // FIXME bytes client sent
        //     statEvent.setP2sChunks(sessionGlobalState.serverSideListener().txChunks);
        //     UvmContextFactory.context().logEvent( statEvent );
        // }
            
        logger.error("FIXME conntrack hook called");
    }

}
