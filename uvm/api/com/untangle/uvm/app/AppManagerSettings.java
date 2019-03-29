/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;
import java.util.LinkedList;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * The App Manager Settings
 * This stores most of the settings related to the apps
 */
@SuppressWarnings("serial")
public class AppManagerSettings implements Serializable, JSONString
{
    private long nextAppId = 1;
    private LinkedList<AppSettings> apps = new LinkedList<>();

    public AppManagerSettings() {}

    public long getNextAppId() { return nextAppId; }
    public void setNextAppId( long newValue ) { this.nextAppId = newValue; }
    
    public LinkedList<AppSettings> getApps() { return apps; }
    public void setApps( LinkedList<AppSettings> newValue ) { this.apps = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}


