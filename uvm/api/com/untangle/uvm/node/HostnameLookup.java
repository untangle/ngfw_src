/**
 * $Id: HostnameLookup.java,v 1.00 2012/02/16 15:24:14 dmorris Exp $
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
