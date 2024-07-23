/**
 * $Id$
 */
package com.untangle.uvm;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;
import com.untangle.uvm.util.ValidSerializable;

/**
 * Uvm skin settings.
 */
@SuppressWarnings("serial")
@ValidSerializable
public class SkinSettings implements Serializable, JSONString
{
    private String skinName = "material";

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
