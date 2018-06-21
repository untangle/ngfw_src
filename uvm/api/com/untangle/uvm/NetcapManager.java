/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.vnet.PipelineConnector;

public interface NetcapManager
{    
    /** Get the number of sessions from the SessionTable */
    public int getSessionCount();

    /** Get the number of sessions from the SessionTable */
    public int getSessionCount( short protocol );

    /** See if a addr:port binding is already in use by an existing session */
    public boolean isTcpPortUsed( java.net.InetAddress addr, int port );

    /**
     * Lookup MAC address for IP in ARP table
     */
    public String arpLookup( String ipAddress );
    
    /** Shutdown all of the sessions that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcher matcher );
    public void shutdownMatches( SessionMatcher matcher, PipelineConnector connector );

}
