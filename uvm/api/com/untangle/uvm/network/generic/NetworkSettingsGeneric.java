/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

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

    public NetworkSettingsGeneric() {
        super();
    }

    public LinkedList<InterfaceSettingsGeneric> getInterfaces() { return interfaces; }
    public void setInterfaces(LinkedList<InterfaceSettingsGeneric> interfaces) { this.interfaces = interfaces; }

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