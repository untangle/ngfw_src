/*
 * $Id: FacebookSettings.java,v 1.00 2016/04/02 22:04:36 dmorris Exp $
 */
package com.untangle.node.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for Facebook
 */
@SuppressWarnings("serial")
public class FacebookSettings implements java.io.Serializable, JSONString
{
    private boolean authenticationEnabled = false;

    public FacebookSettings() { }

    public boolean getAuthenticationEnabled() { return authenticationEnabled; }
    public void setAuthenticationEnabled(boolean newValue) { this.authenticationEnabled = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
