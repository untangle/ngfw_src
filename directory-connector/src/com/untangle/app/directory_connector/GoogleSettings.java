/**
 * $Id: GoogleSettings.java,v 1.00 2016/04/02 22:31:04 dmorris Exp $
 */
package com.untangle.app.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for Google
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
