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
    private List<DynamicBlockList> dynamicBlockList = new LinkedList<>();

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public List<DynamicBlockList> getDynamicBlockList() { return dynamicBlockList; }
    public void setDynamicBlockList(List<DynamicBlockList> dynamicBlockList) { this.dynamicBlockList = dynamicBlockList; }
 
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
