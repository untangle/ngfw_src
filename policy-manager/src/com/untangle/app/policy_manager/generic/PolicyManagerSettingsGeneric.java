/**
 * $Id$
 */
package com.untangle.app.policy_manager.generic;

import com.untangle.app.policy_manager.PolicySettings;
import com.untangle.uvm.generic.RuleGeneric;
import org.json.JSONObject;
import org.json.JSONString;

import java.io.Serializable;
import java.util.LinkedList;

/**
 * Policy Manager Settings Generic
 */
@SuppressWarnings("serial")
public class PolicyManagerSettingsGeneric implements Serializable, JSONString {

    private LinkedList<PolicySettings> policies  = new LinkedList<>();
    private LinkedList<RuleGeneric> policy_rules = new LinkedList<>();
    private int nextPolicyId = 2;


    public LinkedList<PolicySettings> getPolicies() { return policies; }
    public void setPolicies(LinkedList<PolicySettings> policies) { this.policies = policies; }
    public LinkedList<RuleGeneric> getPolicy_rules() { return policy_rules; }
    public void setPolicy_rules(LinkedList<RuleGeneric> policy_rules) { this.policy_rules = policy_rules; }
    public int getNextPolicyId() { return nextPolicyId; }
    public void setNextPolicyId(int nextPolicyId) { this.nextPolicyId = nextPolicyId; }

    public String toJSONString() {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}