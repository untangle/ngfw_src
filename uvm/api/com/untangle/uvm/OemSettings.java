/**
 * $Id: AdminSettings.java 32043 2012-05-31 21:31:47Z dmorris $
 */
package com.untangle.uvm;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Uvm administrator settings.
 */
@SuppressWarnings("serial")
public class OemSettings implements Serializable, JSONString
{
    private String oemName;
    private String oemUrl;
    private String hiddenLibitems;

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl)
    {
        this.oemName = oemName;
        this.oemUrl = oemUrl;
        this.hiddenLibitems = null;
    }

    /**
     * The OEM name, ie "Untangle"
     */
    public String getOemName() { return oemName; }
    public void setOemName( String newValue ) { this.oemName = newValue; }

    /**
     * The OEM URL, ie "http://untangle.com"
     */
    public String getOemUrl() { return oemUrl; }
    public void setOemUrl( String newValue ) { this.oemUrl = newValue; }

    /**
     * Hidden libitems "foo,bar"
     */
    public String getHiddenLibitems() { return hiddenLibitems; }
    public void setHiddenLibitems( String newValue ) { this.hiddenLibitems = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
