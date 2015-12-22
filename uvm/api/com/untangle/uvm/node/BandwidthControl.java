/**
 * $Id: BandwidthControl.java,v 1.00 2015/12/21 14:56:47 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

/**
 * Supports the ability to lookup a hostname
 */
public interface BandwidthControl
{
    void reprioritizeHostSessions(InetAddress addr);
}
