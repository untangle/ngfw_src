/**
 * $Id: DhcpOption.java,v 1.00 2013/05/31 10:22:08 dmorris Exp $
 */
package com.untangle.uvm.network;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Dns static entry.
 */
@SuppressWarnings("serial")
public class DhcpOption implements Serializable, JSONString
{
    private boolean enabled;
    private String description;
    private String value;
    
    public DhcpOption() {}

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }
    
    public String getDescription() { return this.description; }
    public void setDescription( String newValue ) { this.description = newValue; }

    public String getValue() { return this.value; }
    public void setValue( String newValue ) { this.value = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}