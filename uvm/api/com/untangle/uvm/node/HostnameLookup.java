/**
 * $Id$
 */
package com.untangle.uvm.node;

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
