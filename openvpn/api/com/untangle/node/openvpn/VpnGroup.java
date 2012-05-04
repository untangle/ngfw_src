/**
 * $Id$
 */
package com.untangle.node.openvpn;

import com.untangle.uvm.node.IPAddress;

/**
 * A VPN group of address and clients.
 */
@SuppressWarnings("serial")
public class VpnGroup implements java.io.Serializable
{
    public static final String EMPTY_NAME        = "[no name]";
    public static final String EMPTY_DESCRIPTION = "[no description]";
    public static final String EMPTY_CATEGORY    = "[no category]";

    /* The interface that clients from the client pool are associated with */
    private int intf;

    private IPAddress address;
    private IPAddress netmask;
    private boolean useDNS = false;
    private String name = EMPTY_NAME;
    private String category = EMPTY_CATEGORY;
    private String description = EMPTY_DESCRIPTION;
    private boolean live = true;

    public VpnGroup() { }

    /**
     * Should clients use DNS from the server
     */
    public boolean getUseDNS() { return useDNS; }
    public void setUseDNS(boolean useDNS) { this.useDNS = useDNS; }

    /**
     * Get the pool of addresses for the clients.
     */
    public IPAddress getAddress() { return this.address; }
    public void setAddress( IPAddress address ) { this.address = address; }

    /**
     * Get the pool of netmaskes for the clients, in bridging mode
     * this must come from the pool that the interface is bridged
     * with.
     */
    public IPAddress getNetmask() { return this.netmask; }
    public void setNetmask( IPAddress netmask ) { this.netmask = netmask; }

    /**
     * @return Default interface to associate VPN traffic with.
     */
    public int trans_getIntf() { return this.intf; }
    public void trans_setIntf( int intf ) { this.intf = intf; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isLive() { return live; }
    public void setLive(boolean live) { this.live = live; }
    
    /**
     * This is the name that is used as the common name in the
     * certificate
     */
    public String trans_getInternalName()
    {
        return getName().trim().toLowerCase();
    }

    /**
     * GUI depends on get name for to string to show the list of
     * clients
     */
    public String toString()
    {
        return getName();
    }
}
