/**
 * $Id: OpenVPN.java,v 1.00 2012/05/24 11:20:58 dmorris Exp $
 */
package com.untangle.uvm.node;

import java.net.InetAddress;

public interface OpenVpn
{
    InetAddress getVpnServerAddress();
}