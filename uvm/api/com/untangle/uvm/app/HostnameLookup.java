/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.net.InetAddress;

/**
 * Supports the ability to lookup a hostname
 */
public interface HostnameLookup
{
    /**
     * Lookup the hostname based on the current DHCP information
     */
    String lookupHostname( InetAddress address );
}
