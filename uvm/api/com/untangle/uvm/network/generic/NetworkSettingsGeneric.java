/**
 * $Id$
 */
package com.untangle.uvm.network.generic;

import com.untangle.uvm.generic.RuleConditionGeneric;
import com.untangle.uvm.generic.RuleGeneric;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.PortForwardRuleCondition;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
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
    private LinkedList<RuleGeneric> natRules = null;
    private LinkedList<RuleGeneric> portForwardRules = null;

    public NetworkSettingsGeneric() {
        super();
    }

    public LinkedList<InterfaceSettingsGeneric> getInterfaces() { return interfaces; }
    public void setInterfaces(LinkedList<InterfaceSettingsGeneric> interfaces) { this.interfaces = interfaces; }

    public LinkedList<RuleGeneric> getNatRules() { return natRules; }
    public void setNatRules(LinkedList<RuleGeneric> natRules) { this.natRules = natRules; }

    public LinkedList<RuleGeneric> getPortForwardRules() { return portForwardRules; }
    public void setPortForwardRules(LinkedList<RuleGeneric> portForwardRules) { this.portForwardRules = portForwardRules; }

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
        if (this.portForwardRules != null)
            networkSettings.setPortForwardRules(transformGenericToLegacyPortForwardRules(networkSettings.getPortForwardRules()));

        // Transform NAT Rules
        if (this.natRules != null)
            transformGenericToLegacyNatRules(networkSettings);

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

    /**
     * @param legacyRules List<PortForwardRule>
     * @return List<PortForwardRule>
     */
    private List<PortForwardRule> transformGenericToLegacyPortForwardRules(List<PortForwardRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        deleteOrphanRules(
                this.portForwardRules,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, PortForwardRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(PortForwardRule::getRuleId, Function.identity()));

        for (RuleGeneric ruleGeneric : getPortForwardRules()) {
            PortForwardRule portForwardRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));

            if (portForwardRule == null) {
                portForwardRule = new PortForwardRule();
                legacyRules.add(portForwardRule);
            }

            // Transform enabled, ruleId, description
            portForwardRule.setEnabled(ruleGeneric.isEnabled());
            portForwardRule.setDescription(ruleGeneric.getDescription());
            portForwardRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

            // Transform Action
            if(ruleGeneric.getAction() != null) {
                portForwardRule.setNewDestination(ruleGeneric.getAction().getDnat_address());
                portForwardRule.setNewPort(StringUtil.getInstance().parseInt(ruleGeneric.getAction().getDnat_port(), 80));
            }

            // Transform Conditions
            List<PortForwardRuleCondition> ruleConditions = new LinkedList<>();
            for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
                PortForwardRuleCondition portForwardRuleCondition = new PortForwardRuleCondition();

                portForwardRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
                portForwardRuleCondition.setConditionType(ruleConditionGen.getType());
                portForwardRuleCondition.setValue(ruleConditionGen.getValue());

                ruleConditions.add(portForwardRuleCondition);
            }
            portForwardRule.setConditions(ruleConditions);
        }
        return legacyRules;
    }

    /**
     *
     * @param networkSettings NetworkSettings
     */
    private void transformGenericToLegacyNatRules(NetworkSettings networkSettings) {
    }

//    /**
//     *
//     * @param legacyRules
//     */
//    private void deleteOrphanRules(List<PortForwardRule> legacyRules) {
//        Set<String> incomingIds = this.portForwardRules.stream()
//                .map(RuleGeneric::getRuleId)
//                .collect(Collectors.toSet());
//        legacyRules.removeIf(rule -> !incomingIds.contains(String.valueOf(rule.getRuleId())));
//    }

    /**
     *
     * @param newRules
     * @param legacyRules
     * @param currentIdExtractor
     * @param legacyIdExtractor
     */
    private <T, U> void deleteOrphanRules(
            List<T> newRules,
            List<U> legacyRules,
            Function<T, String> currentIdExtractor,
            Function<U, String> legacyIdExtractor
    ) {
        Set<String> incomingIds = newRules.stream()
                .map(currentIdExtractor)
                .collect(Collectors.toSet());

        legacyRules.removeIf(rule -> !incomingIds.contains(legacyIdExtractor.apply(rule)));
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}