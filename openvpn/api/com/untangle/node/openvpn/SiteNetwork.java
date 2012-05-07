/**
 * $Id$
 */
package com.untangle.node.openvpn;

import com.untangle.uvm.node.IPAddress;

/**
 * A network that is available at a site.
 */
@SuppressWarnings("serial")
public class SiteNetwork
{
    private IPAddress network;
    private IPAddress netmask;
    private String name = "";
    private boolean live = true;

    // constructors -----------------------------------------------------------

    public SiteNetwork() { }

    // accessors --------------------------------------------------------------

    /**
     * The network exported by this client or server.
     */
    public IPAddress getNetwork() { return this.network; }
    public void setNetwork( IPAddress network ) { this.network = network; }

    /**
     * Get the range of netmask on the client side(null for site->machine).
     */
    public IPAddress getNetmask() { return this.netmask; }
    public void setNetmask( IPAddress netmask ) { this.netmask = netmask; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean getLive() { return live; }
    public void setLive(boolean live) { this.live = live; }
}
