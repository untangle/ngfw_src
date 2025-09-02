/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.network.DnsLocalServer;
import com.untangle.uvm.network.DnsSettings;
import com.untangle.uvm.network.DnsStaticEntry;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Dns settings generic.
 */
@SuppressWarnings("serial")
public class DnsSettingsGeneric implements Serializable, JSONString {

    private LinkedList<DnsStaticEntry> staticEntries = new LinkedList<>();
    private LinkedList<DnsLocalServer> localServers = new LinkedList<>();

    public LinkedList<DnsStaticEntry> getStaticEntries() { return staticEntries; }
    public void setStaticEntries(LinkedList<DnsStaticEntry> staticEntries) { this.staticEntries = staticEntries; }
    public LinkedList<DnsLocalServer> getLocalServers() { return localServers; }
    public void setLocalServers(LinkedList<DnsLocalServer> localServers) { this.localServers = localServers; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a {@link DnsSettingsGeneric} object into its v1 DnsSettings representation.
     * @param legacyDnsSettings DnsSettings
     * @return DnsSettings
     */
    public DnsSettings transformGenericToDnsSettings(DnsSettings legacyDnsSettings) {
        if (legacyDnsSettings == null)
            legacyDnsSettings = new DnsSettings();
        legacyDnsSettings.setLocalServers(this.getLocalServers() != null ? this.getLocalServers() : new LinkedList<>());
        legacyDnsSettings.setStaticEntries(this.getStaticEntries() != null ? this.getStaticEntries() : new LinkedList<>());
        return legacyDnsSettings;
    }
}
