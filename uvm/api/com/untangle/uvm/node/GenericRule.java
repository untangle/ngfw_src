/*
 * $HeadURL: svn://chef/work/src/webfilter-base/api/com/untangle/node/webfilter/WebFilterSettings.java $
 */
package com.untangle.uvm.node;

import java.io.Serializable;
import com.untangle.uvm.node.GenericRule;

@SuppressWarnings("serial")
public class GenericRule implements Serializable
{
    Integer id = null;
    String name = null; 
    String string = null;
    String description = null; 
    String category = null; 
    Boolean enabled = null;
    Boolean blocked = null;
    Boolean flagged = null;

    public GenericRule() {}

    public GenericRule(String string, String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        this.string = string;
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = Boolean.valueOf(enabled);
        this.blocked = Boolean.valueOf(blocked);
        this.flagged = Boolean.valueOf(flagged);
    }

    public GenericRule(String string, String name, String category, String description, boolean enabled)
    {
        this.string = string;
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = Boolean.valueOf(enabled);
    }

    public GenericRule(String string, String name, String category, boolean enabled)
    {
        this.string = string;
        this.name = name;
        this.category = category;
        this.enabled = Boolean.valueOf(enabled);
    }

    public GenericRule(String string, boolean enabled)
    {
        this.string = string;
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

    public String getString()
    {
        return this.string;
    }

    public void setString(String string)
    {
        this.string = string;
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