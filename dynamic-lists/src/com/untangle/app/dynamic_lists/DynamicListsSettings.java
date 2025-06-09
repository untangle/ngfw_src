/**
 * $Id$
 */

package com.untangle.app.dynamic_lists;

import java.io.Serializable;
import java.util.List;
import java.net.InetAddress;

import org.json.JSONObject;
import org.json.JSONString;



/**
 * Settings for the DynamicLists app.
 */
@SuppressWarnings("serial")
public class DynamicListsSettings implements Serializable, JSONString
{
    private Integer version = 5;
 
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
