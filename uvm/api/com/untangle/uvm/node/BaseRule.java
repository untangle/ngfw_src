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
    Object attachment = null;

    public BaseRule() {}

    public BaseRule(String name, String category, String description, Boolean enabled, Boolean blocked, Boolean flagged)
    {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = enabled;
        this.blocked = blocked;
        this.flagged = flagged;
    }

    public BaseRule(String name, String category, String description, Boolean enabled)
    {
        this.name = name;
        this.category = category;
        this.description = description;
        this.enabled = enabled;
    }

    public BaseRule(String name, String category, String description)
    {
        this.name = name;
        this.category = category;
        this.description = description;
    }
    
    public BaseRule(Boolean enabled)
    {
        this.enabled = enabled;
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

    /**
     * Attach an object to this rule.
     * This is just a utility for saving some state about this rule
     * It will not be saved with the Rule, nor is it ever guaranteed to be there
     * If you use the attachment you should always check to see if it is null first
     *
     * This is used in cases where the rule may store a regular expression that needs to be compiled
     * The regex can be compiled and attached, if the attachment is null then just recompile a new regex and attach
     *
     * Note: These do not use getAttachment and setAttachment getters and setters to prevent them from being serialized to JSON
     */
    public void attach(Object o)
    {
        this.attachment = o;
    }

    /**
     * Returns the current attachment to this rule (if any)
     *
     * Note: These do not use getAttachment and setAttachment getters and setters to prevent them from being serialized to JSON
     */
    public Object attachment()
    {
        return attachment;
    }
}