/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;
import java.util.List;
import java.util.LinkedList;

/**
 * Intrusion prevention rule
 */
@SuppressWarnings("serial")
public class IntrusionPreventionRule implements Serializable, JSONString
{
    private String action = "default";
    private List<IntrusionPreventionRuleCondition> conditions = new LinkedList<>();
    private String description = "";
    private Boolean enabled = false;
    private String id = "unid";
    private String sourceNetworks = "recommended";
    private String destinationNetworks = "recommended";

    public IntrusionPreventionRule() { }

    public IntrusionPreventionRule(String action, List<IntrusionPreventionRuleCondition> conditions, String description, Boolean enabled, String id)
    {
        this.action = action;
        this.conditions = conditions;
        this.description = description;
        this.enabled = enabled;
        this.id = id;
    }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<IntrusionPreventionRuleCondition> getConditions() { return conditions; }
    public void setConditions(List<IntrusionPreventionRuleCondition> conditions) { this.conditions = conditions; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSourceNetworks() { return sourceNetworks; }
    public void setSourceNetworks(String sourceNetworks) { this.sourceNetworks = sourceNetworks; }

    public String getDestinationNetworks() { return destinationNetworks; }
    public void setDestinationNetworks(String destinationNetworks) { this.destinationNetworks = destinationNetworks; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}

