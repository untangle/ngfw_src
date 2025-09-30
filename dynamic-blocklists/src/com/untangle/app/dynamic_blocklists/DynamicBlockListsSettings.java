/**
 * $Id$
 */

package com.untangle.app.dynamic_blocklists;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Settings for the DynamicBlockLists app.
 */
@SuppressWarnings("serial")
public class DynamicBlockListsSettings implements Serializable, JSONString
{
    private Integer version = 1;
    private Boolean enabled = false;
    private LinkedList<DynamicBlockList> configurations = new LinkedList<DynamicBlockList>();
    public DynamicBlockListsSettings() { }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LinkedList<DynamicBlockList> getConfigurations() { return configurations; }
    public void setConfigurations(LinkedList<DynamicBlockList> configurations) { this.configurations = configurations; }
    
    public Boolean getEnabled() { return enabled; } 
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
