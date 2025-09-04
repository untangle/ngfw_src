/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.net.InetAddress;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Dns static entry.
 */
@SuppressWarnings("serial")
public class DhcpRelay implements Serializable, JSONString
{
    // !! default values
    private boolean enabled;
    private String description;
    private InetAddress rangeStart;
    private InetAddress rangeEnd;
    private Integer leaseDuration;
    private InetAddress gateway;
    private Integer prefix;
    private String dns;
    private List<DhcpOption> options = null; /* DHCP dnsmasq options */
    
    public DhcpRelay() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }
    
    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public InetAddress getRangeStart() { return this.rangeStart; }
    public void setRangeStart( InetAddress newValue ) { this.rangeStart = newValue; }

    public InetAddress getRangeEnd() { return this.rangeEnd; }
    public void setRangeEnd( InetAddress newValue ) { this.rangeEnd = newValue; }

    public Integer getLeaseDuration() { return this.leaseDuration; }
    public void setLeaseDuration( Integer newValue ) { this.leaseDuration = newValue; }

    public InetAddress getGateway() { return this.gateway; }
    public void setGateway( InetAddress newValue ) { this.gateway = newValue; }

    public Integer getPrefix() { return this.prefix; }
    public void setPrefix( Integer newValue ) { this.prefix = newValue; }

    public String getDns() { return this.dns; }
    public void setDns( String newValue ) { this.dns = newValue; }

    public List<DhcpOption> getOptions() { return this.options; }
    public void setOptions( List<DhcpOption> newValue ) { this.options = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms the given list of {@link DhcpRelay} objects by ensuring that
     * each relay's options list is initialized as a {@link LinkedList}.
     * @param dhcpRelays the list of DhcpRelay objects to transform
     * @return the same list with updated options lists
     */
    public static List<DhcpRelay> transformGenericToDhcpRelay(LinkedList<DhcpRelay> dhcpRelays) {
        for (DhcpRelay relay : dhcpRelays) {
            List<DhcpOption> opts = relay.getOptions();
            relay.setOptions(opts != null ? new LinkedList<>(opts) : new LinkedList<>());
        }
        return dhcpRelays;
    }
}