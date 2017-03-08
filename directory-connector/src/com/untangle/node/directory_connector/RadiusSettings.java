/*
 * $Id$
 */
package com.untangle.node.directory_connector;

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
    
    public RadiusSettings() { }

    public RadiusSettings(boolean isEnabled, String server, int authPort, int acctPort, String sharedSecret, String authenticationMethod)
    {
        this.isEnabled = isEnabled;
        this.server = server;
        this.authPort = authPort;
        this.acctPort = acctPort;
        this.sharedSecret = sharedSecret;
        this.authenticationMethod = authenticationMethod;
    }

    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean isEnabled) { this.isEnabled = isEnabled; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public int getAuthPort() { return authPort; }
    public void setAuthPort(int authPort) { this.authPort = authPort; }

    public int getAcctPort() { return acctPort; }
    public void setAcctPort(int acctPort) { this.acctPort = acctPort; }

    public String getSharedSecret() { return sharedSecret; }
    public void setSharedSecret(String sharedSecret) { this.sharedSecret = sharedSecret; }

    public String getAuthenticationMethod() { return this.authenticationMethod; }
    public void setAuthenticationMethod( String authenticationMethod ) { this.authenticationMethod = authenticationMethod; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
