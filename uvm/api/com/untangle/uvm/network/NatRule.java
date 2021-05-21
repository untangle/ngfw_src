/**
 * $Id$
 */
package com.untangle.uvm.network;

import java.util.List;
import java.io.Serializable;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;
import org.apache.log4j.Logger;

/**
 * This in the implementation of a Nat Rule
 *
 * A rule is basically a collection of NatRuleConditions (matchers)
 * and what to do if the matchers match (block, log, etc)
 */
@SuppressWarnings("serial")
public class NatRule implements JSONString, Serializable
{
    private final Logger logger = Logger.getLogger(getClass());

    private List<NatRuleCondition> matchers;

    private Integer ruleId;
    private Boolean enabled;
    private Boolean auto;
    private InetAddress newSource;
    private String description;
    private Boolean ngfwAdded = false;
    private String addedBy;
    
    public NatRule() { }

    public NatRule(boolean enabled, List<NatRuleCondition> matchers, InetAddress newSource, String description)
    {
        this.setConditions(matchers);
        this.setEnabled(Boolean.valueOf(enabled));
        this.setNewSource(newSource);
        this.setDescription(description);
        this.setNgfwAdded(false);
        this.setAddedBy("user-created");
    }
    
    public List<NatRuleCondition> getConditions() { return this.matchers; }
    public void setConditions( List<NatRuleCondition> matchers ) { this.matchers = matchers; }

    public Integer getRuleId() { return this.ruleId; }
    public void setRuleId(Integer ruleId) { this.ruleId = ruleId; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled( Boolean enabled ) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription( String description ) { this.description = description; }

    public Boolean getAuto() { return auto; }
    public void setAuto( Boolean auto ) { this.auto = auto; }

    public InetAddress getNewSource() { return newSource; }
    public void setNewSource( InetAddress newSource ) { this.newSource = newSource; }

    public boolean getNgfwAdded() { return this.ngfwAdded; }
    public void setNgfwAdded( Boolean ngfwAdded ) { this.ngfwAdded = ngfwAdded; }

    public String getAddedBy() { return this.addedBy; }
    public void setAddedBy( String addedBy ) { this.addedBy = addedBy; } 

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

