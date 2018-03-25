/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

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

    public OemSettings() { }

    public OemSettings( String oemName, String oemUrl)
    {
        this.oemName = oemName;
        this.oemUrl = oemUrl;
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

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
