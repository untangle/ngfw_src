/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.network.DnsSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.NetworkSettings;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Network settings v2.
 */
@SuppressWarnings("serial")
public class NetworkSettingsGeneric implements Serializable, JSONString {

    private LinkedList<InterfaceSettingsGeneric> interfaces = null;     /* Declared using LinkedList to ensure correct type during Jabsorb deserialization */
    private LinkedList<InterfaceSettingsGeneric> virtualInterfaces = null;
    private LinkedList<RuleGeneric> port_forward_rules = null;
    private LinkedList<RuleGeneric> nat_rules = null;
    private LinkedList<RuleGeneric> bypass_rules = null;
    private LinkedList<RuleGeneric> filter_rules = null;
    private LinkedList<StaticRouteGeneric> staticRoutes = null;

    private DnsSettingsGeneric dnsSettings;

    public NetworkSettingsGeneric() {
        super();
    }

    public LinkedList<InterfaceSettingsGeneric> getInterfaces() { return interfaces; }
    public void setInterfaces(LinkedList<InterfaceSettingsGeneric> interfaces) { this.interfaces = interfaces; }

    public LinkedList<InterfaceSettingsGeneric> getVirtualInterfaces() { return virtualInterfaces; }
    public void setVirtualInterfaces(LinkedList<InterfaceSettingsGeneric> virtualInterfaces) { this.virtualInterfaces = virtualInterfaces; }

    public LinkedList<RuleGeneric> getPort_forward_rules() { return port_forward_rules; }
    public void setPort_forward_rules(LinkedList<RuleGeneric> port_forward_rules) { this.port_forward_rules = port_forward_rules; }
    public LinkedList<RuleGeneric> getNat_rules() { return nat_rules; }
    public void setNat_rules(LinkedList<RuleGeneric> nat_rules) { this.nat_rules = nat_rules; }
    public LinkedList<RuleGeneric> getBypass_rules() { return bypass_rules; }
    public void setBypass_rules(LinkedList<RuleGeneric> bypass_rules) { this.bypass_rules = bypass_rules; }
    public LinkedList<RuleGeneric> getFilter_rules() { return filter_rules; }
    public void setFilter_rules(LinkedList<RuleGeneric> filter_rules) { this.filter_rules = filter_rules; }

    public LinkedList<StaticRouteGeneric> getStaticRoutes() { return staticRoutes; }
    public void setStaticRoutes(LinkedList<StaticRouteGeneric> staticRoutes) { this.staticRoutes = staticRoutes; }

    public DnsSettingsGeneric getDnsSettings() { return dnsSettings; }
    public void setDnsSettings(DnsSettingsGeneric dnsSettings) { this.dnsSettings = dnsSettings; }

    /**
     * Populates the provided {@link NetworkSettings} instance with data from this
     * {@link NetworkSettingsGeneric} instance.
     * Each {@link InterfaceSettingsGeneric} is transformed into a corresponding
     * {@link InterfaceSettings} and added to the target {@code networkSettings}.
     * If an interface with the same ID already exists, it is updated in place.
     *
     * @param networkSettings the target {@link NetworkSettings} object to be updated.
     *                        If {@code null}, a new instance should be created before use.
     */
    public void transformGenericToNetworkSettings(NetworkSettings networkSettings) {
        if (networkSettings == null)
            networkSettings = new NetworkSettings();

        // Transform interfaces
        if (networkSettings.getInterfaces() == null)
            networkSettings.setInterfaces(new LinkedList<>());
        List<InterfaceSettings> existingInterfaces = networkSettings.getInterfaces();

        // CLEANUP: Remove deleted interfaces first
        deleteOrphanInterfaces(existingInterfaces);

        // Build a map for quick lookup by interfaceId
        Map<Integer, InterfaceSettings> interfaceMap = existingInterfaces.stream()
            .collect(Collectors.toMap(InterfaceSettings::getInterfaceId, Function.identity()));

        boolean qosEnabled = false;
        for (InterfaceSettingsGeneric intfSettingsGen : this.interfaces) {
            InterfaceSettings matchingIntf = interfaceMap.get(intfSettingsGen.getInterfaceId());

            if (matchingIntf == null) {
                matchingIntf = new InterfaceSettings();
                existingInterfaces.add(matchingIntf);
            }

            // Transform data from generic to original InterfaceSettings
            intfSettingsGen.transformGenericToInterfaceSettings(matchingIntf);

            // Track if any WAN interface has QoS enabled
            if (intfSettingsGen.isWan() && intfSettingsGen.isQosEnabled()) {
                qosEnabled = true;
            }
        }

        // QOS Settings
        if(networkSettings.getQosSettings() != null) {
            networkSettings.getQosSettings().setQosEnabled(qosEnabled);
        }

        // Transform Port Forward Rules
        if (this.getPort_forward_rules() != null)
            networkSettings.setPortForwardRules(RuleGeneric.transformGenericToLegacyPortForwardRules(this.getPort_forward_rules(), networkSettings.getPortForwardRules()));

        // Transform NAT Rules
        if (this.getNat_rules() != null)
            networkSettings.setNatRules(RuleGeneric.transformGenericToLegacyNatRules(this.getNat_rules(), networkSettings.getNatRules()));

        // Transform Bypass Rules
        if (this.getBypass_rules() != null)
            networkSettings.setBypassRules(RuleGeneric.transformGenericToLegacyBypassRules(this.getBypass_rules(), networkSettings.getBypassRules()));

        // Transform Filter Rules
        if (this.getFilter_rules() != null)
            networkSettings.setFilterRules(RuleGeneric.transformGenericToLegacyFilterRules(this.getFilter_rules(), networkSettings.getFilterRules()));

        // Transform Static Routes
        if (this.getStaticRoutes() != null)
            networkSettings.setStaticRoutes(StaticRouteGeneric.transformGenericToStaticRoutes(this.getStaticRoutes(), networkSettings.getStaticRoutes()));

        // Transform DNS Settings
        if (this.getDnsSettings() != null)
            networkSettings.setDnsSettings(this.getDnsSettings().transformGenericToDnsSettings(networkSettings.getDnsSettings()));

        // Write other transformations below
        
    }

    /**
     * deletes the orphan interfaces
     * @param existingInterfaces List<InterfaceSettings>
     */
    private void deleteOrphanInterfaces(List<InterfaceSettings> existingInterfaces) {
        Set<Integer> incomingIds = this.interfaces.stream()
                .map(InterfaceSettingsGeneric::getInterfaceId)
                .collect(Collectors.toSet());
        existingInterfaces.removeIf(intf -> !incomingIds.contains(intf.getInterfaceId()));
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}