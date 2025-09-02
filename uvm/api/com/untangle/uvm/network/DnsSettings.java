/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.network.generic.DnsSettingsGeneric;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Dns settings.
 */
@SuppressWarnings("serial")
public class DnsSettings implements Serializable, JSONString
{
    private List<DnsStaticEntry> staticEntries = new LinkedList<>();
    private List<DnsLocalServer> localServers = new LinkedList<>();
    
    public DnsSettings() {}

    public List<DnsStaticEntry> getStaticEntries() { return this.staticEntries; }
    public void setStaticEntries( List<DnsStaticEntry> newValue ) { this.staticEntries = newValue; }

    public List<DnsLocalServer> getLocalServers() { return this.localServers; }
    public void setLocalServers( List<DnsLocalServer> newValue ) { this.localServers = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link DnsSettings} object into its generic representation.
     * @param dnsSettings DnsSettings
     * @return DnsSettingsGeneric
     */
    public static DnsSettingsGeneric transformDnsSettingsToGeneric(DnsSettings dnsSettings) {
        DnsSettingsGeneric dnsSettingsGeneric = new DnsSettingsGeneric();
        dnsSettingsGeneric.setLocalServers(
                dnsSettings.getLocalServers() != null
                        ? new LinkedList<>(dnsSettings.getLocalServers())
                        : new LinkedList<>()
        );
        dnsSettingsGeneric.setStaticEntries(
                dnsSettings.getStaticEntries() != null
                        ? new LinkedList<>(dnsSettings.getStaticEntries())
                        : new LinkedList<>()
        );
        return dnsSettingsGeneric;
    }
}