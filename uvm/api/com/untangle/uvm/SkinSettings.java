/**
 * $Id: SkinSettings.java,v 1.00 2012/05/09 15:53:44 dmorris Exp $
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Uvm skin settings.
 */
@SuppressWarnings("serial")
public class SkinSettings implements Serializable, JSONString
{
    private String skinName = "default";

    public SkinSettings() { }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
    
    /**
     * Get the skin used in the administration client
     */
	public String getSkinName() { return skinName; }
	public void setSkinName( String skinName ) { this.skinName = skinName; }
}
