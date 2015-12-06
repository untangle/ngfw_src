/*
 * $Id: RadiusSettings.java 41234 2015-09-12 00:47:13Z dmorris $
 */
package com.untangle.node.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for Radius
 */
@SuppressWarnings("serial")
public class GoogleSettings implements java.io.Serializable, JSONString
{
    private String driveRefreshToken = null;
    
    public GoogleSettings() { }

    public String getDriveRefreshToken() { return driveRefreshToken; }
    public void setDriveRefreshToken(String newValue) { this.driveRefreshToken = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
