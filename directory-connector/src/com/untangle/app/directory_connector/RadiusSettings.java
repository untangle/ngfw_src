/**
 * $Id$
 */
package com.untangle.app.directory_connector;

import org.json.JSONObject;
import org.json.JSONString;

/**
 * Settings for Radius
 */
@SuppressWarnings("serial")
public class RadiusSettings implements java.io.Serializable, JSONString
{
    private boolean isEnabled = false;
    private Long id;
    private String server;
    private int authPort = 1812;
    private int acctPort = 1813;
    private String sharedSecret;
    
    //public enum AuthenticationMethod { CLEARTEXT, PAP, CHAP, MSCHAPV1, MSCHAPV2 };
    private String authenticationMethod = "PAP";

    /**
     * Construct with default values.
     */
    public RadiusSettings() { }

    /**
     * Construct with settings.
     *
     * @param isEnabled
     *      true if Radius client enabled, false if disabled.
     * @param server
     *      Radius server address.
     * @param authPort
     *      Authentication port.
     * @param acctPort
     *      Accounting port.
     * @param sharedSecret
     *      Shared secret.
     * @param authenticationMethod
     *      Authentication method.
     */
    public RadiusSettings(boolean isEnabled, String server, int authPort, int acctPort, String sharedSecret, String authenticationMethod)
    {
        this.isEnabled = isEnabled;
        this.server = server;
        this.authPort = authPort;
        this.acctPort = acctPort;
        this.sharedSecret = sharedSecret;
        this.authenticationMethod = authenticationMethod;
    }

    /**
     * Get whether Radius client is enabled.
     *
     * @return
     *      true if enabled, false if disabled.
     */
    public boolean isEnabled() { return isEnabled; }
    /**
     * Set Radius client enabled or disabled.
     *
     * @param isEnabled
     *      true to enable, false to disable.
     */
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }

    /**
     * Get Radius server address.
     *
     * @return
     *      Radius server address
     */
    public String getServer() { return server; }
    /**
     * Set Radius server address.
     *
     * @param server
     *      Radius server address.
     */
    public void setServer(String server) { this.server = server; }

    /**
     * Get authentication port.
     *
     * @return
     *      Radius server authentication port.
     */
    public int getAuthPort() { return authPort; }
    /**
     * Set authentication port.
     *
     * @param authPort
     *      Set Radius server authentication port.
     */
    public void setAuthPort(int authPort) { this.authPort = authPort; }

    /**
     * Get accounting port.
     *
     * @return
     *      Radius server accounting port.
     */
    public int getAcctPort() { return acctPort; }
    /**
     * Set accounting port.
     *
     * @param acctPort
     *      Set Radius server accounting port.
     */
    public void setAcctPort(int acctPort) { this.acctPort = acctPort; }

    /**
     * Get Radius server shared secret.
     *
     * @return
     *      Radius server shared secret.
     */
    public String getSharedSecret() { return sharedSecret; }
    /**
     * Set Radius server shared secret.
     *
     * @param sharedSecret
     *      Set Radius server shared secret.
     */
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }

    /**
     * Get Radius server authentication method.
     *
     * @return
     *      Radius server authentication method.
     */
    public String getAuthenticationMethod() { return this.authenticationMethod; }
    /**
     * Set Radius server authentication method.
     *
     * @param authenticationMethod
     *      Set Radius server authentication method.
     */
    public void setAuthenticationMethod( String authenticationMethod ) { this.authenticationMethod = authenticationMethod; }

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
