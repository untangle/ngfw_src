/**
 * $Id$
 */
package com.untangle.app.policy_manager;

import java.util.List;
import java.util.LinkedList;

/**
 * Policy Settings
 */
public class PolicySettings 
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
}
