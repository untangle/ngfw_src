/**
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
    private int version = 3;

    private boolean apiEnabled = false;
    private String apiSecretKey = null;
    private boolean apiManualAddressAllowed = false;
    private ActiveDirectorySettings activeDirectorySettings;
    private RadiusSettings radiusSettings;
    private GoogleSettings googleSettings;

    /**
     * Constructor with default values.
     */
    public DirectoryConnectorSettings() { }

    /**
     * Get active directory settings.
     *
     * @return
     *      Active Directory settings.
     */
    public ActiveDirectorySettings getActiveDirectorySettings() { return this.activeDirectorySettings; }
    /**
     * Set active directory settings.
     *
     * @param newValue
     *      Set new Active Directory settings.
     */
    public void setActiveDirectorySettings( ActiveDirectorySettings newValue ) { this.activeDirectorySettings = newValue; }

    /**
     * Get Radius settings.
     *
     * @return
     *      Radius settings.
     */
    public RadiusSettings getRadiusSettings() { return radiusSettings; }
    /**
     * Set Radius settings.
     *
     * @param newValue
     *      Set new Radius settings.
     */
    public void setRadiusSettings( RadiusSettings newValue ) { radiusSettings = newValue; }

    /**
     * Get Google settings.
     *
     * @return
     *      Google settings.
     */
    public GoogleSettings getGoogleSettings() { return googleSettings; }
    /**
     * Set Google settings.
     *
     * @param newValue
     *      Set new Google settings.
     */
    public void setGoogleSettings( GoogleSettings newValue ) { googleSettings = newValue; }
    
    /**
     * Get settings version.
     *
     * @return
     *      Current settings version.
     */
    public int getVersion() { return this.version; }
    /**
     * Set setting settings.
     *
     * @param newValue
     *      Set newversion.
     */
    public void setVersion( int newValue ) { this.version = newValue; }

    /**
     * Get whether API is enabled.
     *
     * @return
     *      true if enabled, false otherwise.
     */
    public boolean getApiEnabled() { return this.apiEnabled; };
    /**
     * Set API enabled or disabled.
     *
     * @param newValue
     *      true if enabled, false if disabled.
     */
    public void setApiEnabled( boolean newValue ) { this.apiEnabled = newValue; }

    /**
     * Get API secret key.
     *
     * @return
     *      API secret key.
     */
    public String getApiSecretKey() { return this.apiSecretKey; };
    /**
     * Set API secret key.
     *
     * @param newValue
     *      New API key.
     */
    public void setApiSecretKey( String newValue ) { this.apiSecretKey = newValue; }

    /**
     * This is a hidden setting used to maintain backwards compatibility
     * On new installs this is always false
     * Manual specification of the client IP is only allowed when using the secret key
     *
     * However on old installs we allowed manual specification of the address with no secret key
     * So on upgrade this was set to true
     *
     * @return
     *      true if address is allowed, false otherwise.
     */
    public boolean getApiManualAddressAllowed() { return this.apiManualAddressAllowed; };
    /**
     * This is a hidden setting used to maintain backwards compatibility
     * On new installs this is always false
     * Manual specification of the client IP is only allowed when using the secret key
     *
     * However on old installs we allowed manual specification of the address with no secret key
     * So on upgrade this was set to true
     *
     * @param newValue
     *      true if address is allowed, false otherwise.
     */
    public void setApiManualAddressAllowed( boolean newValue ) { this.apiManualAddressAllowed = newValue; }

    /**
     * Convert settings to JSON string.
     *
     * @return
     *      JSON string.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
