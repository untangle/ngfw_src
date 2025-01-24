/**
 * $Id$
 */

package com.untangle.app.dynamic_lists;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;
import org.json.JSONString;


/**
 * Settings for the DynamicLists app.
 */
@SuppressWarnings("serial")
public class DynamicListsSettings implements Serializable, JSONString
{
    private Integer version = 1;
    private List<DynamicList> dynamicList = new LinkedList<>();

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public List<DynamicList> getDynamicList() { return dynamicList; }
    public void setDynamicList(List<DynamicList> dynamicList) { this.dynamicList = dynamicList; }
 
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
