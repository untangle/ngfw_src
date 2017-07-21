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
    private int version = 2;

    private boolean apiEnabled = false;
    private String apiSecretKey = null;
    private boolean apiManualAddressAllowed = false;
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

    /**
     * This is a hidden setting used to maintain backwards compatibility
     * On new installs this is always false
     * Manual specification of the client IP is only allowed when using the secret key
     *
     * However on old installs we allowed manual specification of the address with no secret key
     * So on upgrade this was set to true
     */
    public boolean getApiManualAddressAllowed() { return this.apiManualAddressAllowed; };
    public void setApiManualAddressAllowed( boolean newValue ) { this.apiManualAddressAllowed = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
