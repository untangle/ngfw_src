/**
 * $Id: CaptureSettings.java,v 1.00 2011/12/14 01:02:03 mahotz Exp $
 */

package com.untangle.node.capture; // API

import java.util.LinkedList;
import java.util.Hashtable;
import org.json.JSONString;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class CaptureSettings implements java.io.Serializable, JSONString
{
    private boolean networkDebug = false;

    public boolean getNetworkDebug()
    {
        return (networkDebug);
    }

    public void setNetworkDebug(boolean networkDebug)
    {
        this.networkDebug = networkDebug;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
