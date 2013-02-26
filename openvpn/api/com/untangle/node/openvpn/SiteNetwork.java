/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.net.InetAddress;

/**
 * A network that is available at a site.
 */
@SuppressWarnings("serial")
public class SiteNetwork
{
    private InetAddress network;
    private InetAddress netmask;
    private String name = "";
    private boolean live = true;

    // constructors -----------------------------------------------------------

    public SiteNetwork() { }

    // accessors --------------------------------------------------------------

    /**
     * The network exported by this client or server.
     */
    public InetAddress getNetwork() { return this.network; }
    public void setNetwork( InetAddress network ) { this.network = network; }

    /**
     * Get the range of netmask on the client side(null for site->machine).
     */
    public InetAddress getNetmask() { return this.netmask; }
    public void setNetmask( InetAddress netmask ) { this.netmask = netmask; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean getLive() { return live; }
    public void setLive(boolean live) { this.live = live; }
}
