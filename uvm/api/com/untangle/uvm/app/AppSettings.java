/**
 * $Id$
 */
package com.untangle.uvm.app;

import java.io.Serializable;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * App Settings.
 */
@SuppressWarnings("serial")
public class AppSettings implements Serializable, JSONString, Comparable<AppSettings>
{
    private Long id = null;
    private Integer policyId = null;
    private String appName = null;
    private AppState targetState = AppState.INITIALIZED;
    
    public enum AppState {
        LOADED,
        INITIALIZED,
        RUNNING, 
        DESTROYED
    }
    
    public AppSettings() {}

    public AppSettings(Long id, Integer policyId, String appName)
    {
        this.id = id;
        this.policyId = policyId;
        this.appName = appName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPolicyId() { return policyId; }
    public void setPolicyId(Integer policyId) { this.policyId = policyId; }

    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }

    public AppState getTargetState() { return targetState; }
    public void setTargetState(AppState targetState) { this.targetState = targetState; }
    
    public int compareTo(AppSettings tid)
    {
        return id < tid.getId() ? -1 : (id > tid.getId() ? 1 : 0);
    }

    public String toString()
    {
        return toJSONString();
    }

    public boolean equals(Object o)
    {
        if (!(o instanceof AppSettings)) {
            return false;
        }
        AppSettings t = (AppSettings)o;

        return id.equals(t.getId());
    }

    public int hashCode()
    {
        return id.hashCode();
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
