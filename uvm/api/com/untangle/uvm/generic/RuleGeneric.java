/**
 * $Id$
 */
package com.untangle.uvm.generic;

import com.untangle.uvm.network.BypassRule;
import com.untangle.uvm.network.BypassRuleCondition;
import com.untangle.uvm.network.FilterRule;
import com.untangle.uvm.network.FilterRuleCondition;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.NatRuleCondition;
import com.untangle.uvm.network.PortForwardRule;
import com.untangle.uvm.network.PortForwardRuleCondition;
import com.untangle.uvm.network.QosRule;
import com.untangle.uvm.network.QosRuleCondition;
import com.untangle.uvm.network.UpnpRule;
import com.untangle.uvm.network.UpnpRuleCondition;
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
 * This in the Generic Rule Class
 * Used for vue model transformations
 */
@SuppressWarnings("serial")
public class RuleGeneric implements JSONString, Serializable {

    public RuleGeneric() {}

    public RuleGeneric(boolean enabled, String description, String ruleId) {
        this.enabled = enabled;
        this.description = description;
        this.ruleId = ruleId;
    }

    // Common To All Rules
    private boolean enabled;
    private String description;
    private String ruleId;
    private RuleActionGeneric action;
    private LinkedList<RuleConditionGeneric> conditions;
    private Boolean readOnlyRule;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public RuleActionGeneric getAction() { return action; }
    public void setAction(RuleActionGeneric action) { this.action = action; }
    public LinkedList<RuleConditionGeneric> getConditions() { return conditions; }
    public void setConditions(LinkedList<RuleConditionGeneric> conditions) { this.conditions = conditions; }
    public Boolean getReadOnlyRule() { return readOnlyRule; }
    public void setReadOnlyRule(Boolean readOnlyRule) { this.readOnlyRule = readOnlyRule; }

    // Required for Filter Rules
    private boolean ipv6Enabled;

    public boolean isIpv6Enabled() { return ipv6Enabled; }
    public void setIpv6Enabled(boolean ipv6Enabled) { this.ipv6Enabled = ipv6Enabled; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

    /**
     * Transforms a list of Port Forward RuleGeneric objects into their v1 PortForwardRule representation.
     * @param portForwardRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<PortForwardRule>
     * @return List<PortForwardRule>
     */
    public static List<PortForwardRule> transformGenericToLegacyPortForwardRules(LinkedList<RuleGeneric> portForwardRulesGen, List<PortForwardRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        deleteOrphanRules(
                portForwardRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, PortForwardRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(PortForwardRule::getRuleId, Function.identity()));

        List<PortForwardRule> portForwardRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : portForwardRulesGen) {
            PortForwardRule portForwardRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            portForwardRule = RuleGeneric.transformPortForwardRule(ruleGeneric, portForwardRule);
            portForwardRules.add(portForwardRule);
        }
        return portForwardRules;
    }

    /**
     * Transforms a Port Forward Rule Generic object into v1 PortForwardRule representation.
     * @param ruleGeneric RuleGeneric
     * @param portForwardRule PortForwardRule
     * @return PortForwardRule
     */
    private static PortForwardRule transformPortForwardRule(RuleGeneric ruleGeneric, PortForwardRule portForwardRule) {
        if (portForwardRule == null)
            portForwardRule = new PortForwardRule();

        // Transform enabled, ruleId, description
        portForwardRule.setEnabled(ruleGeneric.isEnabled());
        portForwardRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
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
        return portForwardRule;
    }

    /**
     * Transforms a list of NAT RuleGeneric objects into their v1 NatRule representation.
     * @param natRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<NatRule>
     * @return List<NatRule>
     */
    public static List<NatRule> transformGenericToLegacyNatRules(LinkedList<RuleGeneric> natRulesGen, List<NatRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        deleteOrphanRules(
                natRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, NatRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(NatRule::getRuleId, Function.identity()));

        List<NatRule> natRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : natRulesGen) {
            NatRule natRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            natRule = RuleGeneric.transformNatRule(ruleGeneric, natRule);
            natRules.add(natRule);
        }
        return natRules;
    }

    /**
     * Transforms a NAT Rule Generic object into v1 NatRule representation.
     * @param ruleGeneric RuleGeneric
     * @param natRule NatRule
     * @return NatRule
     */
    private static NatRule transformNatRule(RuleGeneric ruleGeneric, NatRule natRule) {
        if (natRule == null)
            natRule = new NatRule();

        // Transform enabled, ruleId, description
        natRule.setEnabled(ruleGeneric.isEnabled());
        natRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        natRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        if(ruleGeneric.getAction() != null) {
            natRule.setAuto(ruleGeneric.getAction().getType() == RuleActionGeneric.Type.MASQUERADE);
            natRule.setNewSource(ruleGeneric.getAction().getSnat_address());
        }

        // Transform Conditions
        List<NatRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            NatRuleCondition natRuleCondition = new NatRuleCondition();

            natRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            natRuleCondition.setConditionType(ruleConditionGen.getType());
            natRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(natRuleCondition);
        }
        natRule.setConditions(ruleConditions);
        return natRule;
    }

    /**
     * Transforms a list of Bypass RuleGeneric objects into their v1 BypassRule representation.
     * Used for set api calls
     * @param bypassRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<BypassRule>
     * @return List<BypassRule>
     */
    public static List<BypassRule> transformGenericToLegacyBypassRules(LinkedList<RuleGeneric> bypassRulesGen, List<BypassRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                bypassRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, BypassRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(BypassRule::getRuleId, Function.identity()));

        List<BypassRule> bypassRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : bypassRulesGen) {
            BypassRule bypassRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            bypassRule = RuleGeneric.transformBypassRule(ruleGeneric, bypassRule);
            bypassRules.add(bypassRule);
        }
        return bypassRules;
    }

    /**
     * Transforms a Bypass Rule Generic object into v1 BypassRule representation.
     * @param ruleGeneric RuleGeneric
     * @param bypassRule BypassRule
     * @return BypassRule
     */
    public static BypassRule transformBypassRule(RuleGeneric ruleGeneric, BypassRule bypassRule) {
        if (bypassRule == null)
            bypassRule = new BypassRule();

        // Transform enabled, ruleId, description
        bypassRule.setEnabled(ruleGeneric.isEnabled());
        bypassRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        bypassRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        if (ruleGeneric.getAction() != null)
            bypassRule.setBypass(ruleGeneric.getAction().getType() == RuleActionGeneric.Type.BYPASS);

        // Transform Conditions
        List<BypassRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            BypassRuleCondition bypassRuleCondition = new BypassRuleCondition();

            bypassRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            bypassRuleCondition.setConditionType(ruleConditionGen.getType());
            bypassRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(bypassRuleCondition);
        }
        bypassRule.setConditions(ruleConditions);
        return bypassRule;
    }

    /**
     * Transforms a list of Filter RuleGeneric objects into their v1 FilterRule representation.
     * Used for set api calls
     * @param filterRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<FilterRule>
     * @return List<FilterRule>
     */
    public static List<FilterRule> transformGenericToLegacyFilterRules(LinkedList<RuleGeneric> filterRulesGen, List<FilterRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                filterRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, FilterRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(FilterRule::getRuleId, Function.identity()));

        List<FilterRule> filterRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : filterRulesGen) {
            FilterRule filterRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            filterRule = RuleGeneric.transformFilterRule(ruleGeneric, filterRule);
            filterRules.add(filterRule);
        }
        return filterRules;
    }

    /**
     * Transforms a Filter Rule Generic object into v1 FilterRule representation.
     * @param ruleGeneric RuleGeneric
     * @param filterRule FilterRule
     * @return FilterRule
     */
    private static FilterRule transformFilterRule(RuleGeneric ruleGeneric, FilterRule filterRule) {
        if (filterRule == null)
            filterRule = new FilterRule();

        // Transform enabled, ruleId, description
        filterRule.setEnabled(ruleGeneric.isEnabled());
        filterRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        filterRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));
        filterRule.setIpv6Enabled(ruleGeneric.isIpv6Enabled());
        filterRule.setReadOnly(ruleGeneric.getReadOnlyRule());

        // Transform Action
        if (ruleGeneric.getAction() != null)
            filterRule.setBlocked(ruleGeneric.getAction().getType() == RuleActionGeneric.Type.REJECT);

        // Transform Conditions
        List<FilterRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            FilterRuleCondition filterRuleCondition = new FilterRuleCondition();

            filterRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            filterRuleCondition.setConditionType(ruleConditionGen.getType());
            filterRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(filterRuleCondition);
        }
        filterRule.setConditions(ruleConditions);
        return filterRule;
    }

    /**
     * Transforms a list of QoS RuleGeneric objects into their v1 QosRule representation.
     * Used for set api calls
     * @param qosRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<QosRule>
     * @return List<QosRule>
     */
    public static List<QosRule> transformGenericToQoSRules(LinkedList<RuleGeneric> qosRulesGen, List<QosRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                qosRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, QosRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(QosRule::getRuleId, Function.identity()));

        List<QosRule> qosRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : qosRulesGen) {
            QosRule qosRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            qosRule = RuleGeneric.transformQosRule(ruleGeneric, qosRule);
            qosRules.add(qosRule);
        }
        return qosRules;
    }

    /**
     * Transforms a QoS Rule Generic object into v1 QosRule representation.
     * @param ruleGeneric RuleGeneric
     * @param qosRule QosRule
     * @return QosRule
     */
    private static QosRule transformQosRule(RuleGeneric ruleGeneric, QosRule qosRule) {
        if (qosRule == null)
            qosRule = new QosRule();

        // Transform enabled, ruleId, description
        qosRule.setEnabled(ruleGeneric.isEnabled());
        qosRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        qosRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        if (ruleGeneric.getAction() != null)
            qosRule.setPriority(ruleGeneric.getAction().getPriority());

        // Transform Conditions
        List<QosRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            QosRuleCondition qosRuleCondition = new QosRuleCondition();

            qosRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            qosRuleCondition.setConditionType(ruleConditionGen.getType());
            qosRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(qosRuleCondition);
        }
        qosRule.setConditions(ruleConditions);
        return qosRule;
    }

    /**
     * Transforms a list of Upnp RuleGeneric objects into their v1 UpnpRule representation.
     * Used for set api calls
     * @param upnpRulesGen LinkedList<RuleGeneric>
     * @param legacyRules List<UpnpRule>
     * @return List<UpnpRule>
     */
    public static List<UpnpRule> transformGenericToUpnpRules(LinkedList<RuleGeneric> upnpRulesGen, List<UpnpRule> legacyRules) {
        if (legacyRules == null)
            legacyRules = new LinkedList<>();

        // CLEANUP: Remove deleted rules first
        RuleGeneric.deleteOrphanRules(
                upnpRulesGen,
                legacyRules,
                RuleGeneric::getRuleId,
                r -> String.valueOf(r.getRuleId())
        );

        // Build a map for quick lookup by ruleId
        Map<Integer, UpnpRule> rulesMap = legacyRules.stream()
                .collect(Collectors.toMap(UpnpRule::getRuleId, Function.identity()));

        List<UpnpRule> upnpRules = new LinkedList<>();
        for (RuleGeneric ruleGeneric : upnpRulesGen) {
            UpnpRule upnpRule = rulesMap.get(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), 0));
            upnpRule = RuleGeneric.transformUpnpRule(ruleGeneric, upnpRule);
            upnpRules.add(upnpRule);
        }
        return upnpRules;
    }

    /**
     * Transforms a Upnp Rule Generic object into v1 UpnpRule representation.
     * @param ruleGeneric RuleGeneric
     * @param upnpRule UpnpRule
     * @return UpnpRule
     */
    private static UpnpRule transformUpnpRule(RuleGeneric ruleGeneric, UpnpRule upnpRule) {
        if (upnpRule == null)
            upnpRule = new UpnpRule();

        // Transform enabled, ruleId, description
        upnpRule.setEnabled(ruleGeneric.isEnabled());
        upnpRule.setDescription(ruleGeneric.getDescription());
        // For new rules added UI send uuid string as ruleId. Here its set to -1
        upnpRule.setRuleId(StringUtil.getInstance().parseInt(ruleGeneric.getRuleId(), -1));

        // Transform Action
        if (ruleGeneric.getAction() != null)
            upnpRule.setAllow(ruleGeneric.getAction().getType() == RuleActionGeneric.Type.ACCEPT);

        // Transform Conditions
        List<UpnpRuleCondition> ruleConditions = new LinkedList<>();
        for (RuleConditionGeneric ruleConditionGen : ruleGeneric.getConditions()) {
            UpnpRuleCondition upnpRuleCondition = new UpnpRuleCondition();

            upnpRuleCondition.setInvert(ruleConditionGen.getOp().equals(Constants.IS_NOT_EQUALS_TO));
            upnpRuleCondition.setConditionType(ruleConditionGen.getType());
            upnpRuleCondition.setValue(ruleConditionGen.getValue());

            ruleConditions.add(upnpRuleCondition);
        }
        upnpRule.setConditions(ruleConditions);
        return upnpRule;
    }

    /**
     * Common method to delete the orphan rules
     * @param newRules List<T>
     * @param legacyRules List<U>
     * @param currentIdExtractor Function<T, String>
     * @param legacyIdExtractor Function<U, String>
     */
    private static <T, U> void deleteOrphanRules(
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
}
