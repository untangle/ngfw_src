/**
 * $Id$
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
    private String appsViewType = "default";

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

    /**
     * Get the application view type used in the administration client 
     */
    public String getAppsViewType() { return appsViewType; }
    public void setAppsViewType(String appsViewType) { this.appsViewType = appsViewType; }
}
