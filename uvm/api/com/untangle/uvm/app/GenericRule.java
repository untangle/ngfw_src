/**
 * $Id$
 */

package com.untangle.uvm.app;

/**
 * Generic rule 
 */
@SuppressWarnings("serial")
public class GenericRule extends BaseRule implements java.io.Serializable, org.json.JSONString
{
    String string = null;
    Object attachment = null;
    
    public GenericRule() {}

    public GenericRule(String string, String name, String category, String description, Boolean enabled, Boolean blocked, Boolean flagged)
    {
        super(name, category, description, enabled, blocked, flagged);
        this.string = string;
    }

    public GenericRule(String string, String name, String category, String description, Boolean enabled)
    {
        super(name, category, description, enabled);
        this.string = string;
    }

    public GenericRule(String string, String name, String category, String description)
    {
        super(name, category, description);
        this.string = string;
    }
    
    public GenericRule(String string, boolean enabled)
    {
        super(enabled);
        this.string = string;
    }
    
    public String getString()
    {
        return this.string;
    }

    public void setString(String string)
    {
        this.string = string;
    }

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
