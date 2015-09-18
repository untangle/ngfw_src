/**
 * $Id$
 */
package com.untangle.node.policy_manager;

import java.util.List;
import java.util.LinkedList;

/**
 * Policy Settings
 */
public class PolicySettings 
{
    private Long policyId;
    private String name;
    private String description;
    private Long parentId;

    public PolicySettings() {}

    public PolicySettings(Long policyId, String name, String description, Long parentId)
    {
        this.policyId = policyId;
        this.name = name;
        this.description = description;
        this.parentId = parentId;
    }
    
    public Long getPolicyId() { return this.policyId; }
    public void setPolicyId( Long policyId ) { this.policyId = policyId; }

    public String getName() { return this.name; }
    public void setName( String name ) { this.name = name; }

    public String getDescription() { return this.description; }
    public void setDescription( String description ) { this.description = description; }

    public Long getParentId() { return this.parentId; }
    public void setParentId( Long parentId ) { this.parentId = parentId; }
}
