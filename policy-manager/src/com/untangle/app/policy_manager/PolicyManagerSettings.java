/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Policy Manager Settings
 */
@SuppressWarnings("serial")
public class PolicyManagerSettings implements Serializable, JSONString
{
    private List<PolicyRule> rules = new LinkedList<>(); 
    private List<PolicySettings> policies  = new LinkedList<>();
    private int nextPolicyId = 2;
    
    public PolicyManagerSettings() {}

    public List<PolicyRule> getRules() { return this.rules; }
    public void setRules( List<PolicyRule> rules ) { this.rules = rules; }

    public List<PolicySettings> getPolicies() { return this.policies; }
    public void setPolicies( List<PolicySettings> policies ) { this.policies = policies; }

    public synchronized Integer getNextPolicyId() { return this.nextPolicyId; }
    public synchronized void setNextPolicyId( Integer nextPolicyId ) { this.nextPolicyId = nextPolicyId; }

    public synchronized Integer nextAvailablePolicyId()
    {
        return nextPolicyId++;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}
