/*
 * $Id$
 */
package com.untangle.app.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for the Directory Connector
 */
@SuppressWarnings("serial")
public class DirectoryConnectorSettings implements java.io.Serializable, JSONString
{
    private int version = 1;

    private boolean apiEnabled = true;
    private String apiSecretKey = null;
    
    private ActiveDirectorySettings activeDirectorySettings;
    private RadiusSettings radiusSettings;
    private GoogleSettings googleSettings;
    private FacebookSettings facebookSettings;
    
    public DirectoryConnectorSettings() { }

    public ActiveDirectorySettings getActiveDirectorySettings() { return this.activeDirectorySettings; }
    public void setActiveDirectorySettings( ActiveDirectorySettings newValue ) { this.activeDirectorySettings = newValue; }

    public RadiusSettings getRadiusSettings() { return radiusSettings; }
    public void setRadiusSettings( RadiusSettings newValue ) { radiusSettings = newValue; }

    public GoogleSettings getGoogleSettings() { return googleSettings; }
    public void setGoogleSettings( GoogleSettings newValue ) { googleSettings = newValue; }

    public FacebookSettings getFacebookSettings() { return facebookSettings; }
    public void setFacebookSettings( FacebookSettings newValue ) { facebookSettings = newValue; }
    
    public int getVersion() { return this.version; }
    public void setVersion( int newValue ) { this.version = newValue; }

    public boolean getApiEnabled() { return this.apiEnabled; };
    public void setApiEnabled( boolean newValue ) { this.apiEnabled = newValue; }

    public String getApiSecretKey() { return this.apiSecretKey; };
    public void setApiSecretKey( String newValue ) { this.apiSecretKey = newValue; }
    
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
