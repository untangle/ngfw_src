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
        logger.error("FIXME conntrack hook called");
    }

}
