/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.NetworkSettings;

/**
 * Network settings v2.
 */
@SuppressWarnings("serial")
public class NetworkSettingsGeneric implements Serializable, JSONString {

    private List<InterfaceSettingsGeneric> interfaces = null;

    public NetworkSettingsGeneric() {
        super();
    }

    public List<InterfaceSettingsGeneric> getInterfaces() { return interfaces; }
    public void setInterfaces(List<InterfaceSettingsGeneric> interfaces) { this.interfaces = interfaces; }

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

        // Write other transformations below
        
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}