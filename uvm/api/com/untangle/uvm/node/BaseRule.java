/*
 * $Id: BaseRule.java,v 1.00 2011/09/06 14:41:27 dmorris Exp $
 */
package com.untangle.uvm.node;

@SuppressWarnings("serial")
public class BaseRule implements java.io.Serializable
{
    Integer id = null;
    String name = null; 
    String description = null; 
    String category = null; 
    Boolean enabled = null;
    Boolean blocked = null;
    Boolean flagged = null;

    public BaseRule() {}

    public BaseRule(String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = Boolean.valueOf(enabled);
        this.blocked = Boolean.valueOf(blocked);
        this.flagged = Boolean.valueOf(flagged);
    }

    public BaseRule(String name, String category, String description, boolean enabled)
    {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = Boolean.valueOf(enabled);
    }

    public BaseRule(boolean enabled)
    {
        this.enabled = Boolean.valueOf(enabled);
    }
    
    public Integer getId()
    {
        return this.id;
    }

    public void setId(Integer id)
    {
        this.id = id;
    }

    public Boolean getBlocked()
    {
        return this.blocked;
    }

    public void setBlocked(Boolean blocked)
    {
        this.blocked = blocked;
    }

    public Boolean getEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }
    
    public Boolean getFlagged()
    {
        return this.flagged;
    }

    public void setFlagged(Boolean flagged)
    {
        this.flagged = flagged;
    }
    
    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getCategory()
    {
        return this.category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getName()
    {
        return this.name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
    
}