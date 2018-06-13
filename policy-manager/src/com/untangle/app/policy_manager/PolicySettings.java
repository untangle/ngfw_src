/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.io.Serializable;
import org.json.JSONString;
import org.json.JSONObject;

/**
 * Policy Settings
 */
@SuppressWarnings("serial")
public class PolicySettings implements Serializable, JSONString
{
    private Integer policyId;
    private String name;
    private String description;
    private Integer parentId;

    public PolicySettings() {}

    public PolicySettings(Integer policyId, String name, String description, Integer parentId)
    {
        this.policyId = policyId;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }
    
    public Integer getPolicyId() { return this.policyId; }
    public void setPolicyId( Integer policyId ) { this.policyId = policyId; }

    public String getName() { return this.name; }
    public void setName( String name ) { this.name = name; }

    public String getDescription() { return this.description; }
    public void setDescription( String description ) { this.description = description; }

    public Integer getParentId() { return this.parentId; }
    public void setParentId( Integer parentId ) { this.parentId = parentId; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
