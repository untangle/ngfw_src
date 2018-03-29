/**
 * $Id$
 */

package com.untangle.app.openvpn;

/**
 * Class to represent an OpenVPN remote server
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class OpenVpnRemoteServer implements java.io.Serializable, org.json.JSONString
{
    private boolean enabled = true;
    private String name;
    private boolean authUserPass = false;
    private String authUsername;
    private String authPassword;

    public OpenVpnRemoteServer()
    {
    }

// THIS IS FOR ECLIPSE - @formatter:off

    public boolean getEnabled() { return this.enabled; }
    public void setEnabled( boolean newValue ) { this.enabled = newValue; }

    public String getName() { return name; }
    public void setName(String name) { this.name = ( name == null ? null : name.replaceAll("\\s","") ); }

    public boolean getAuthUserPass() { return authUserPass; }
    public void setAuthUserPass( boolean newValue ) { this.authUserPass = newValue; }

    public String getAuthUsername() { return this.authUsername; }
    public void setAuthUsername( String newValue ) { this.authUsername = newValue; }

    public String getAuthPassword() { return this.authPassword; }
    public void setAuthPassword( String newValue ) { this.authPassword = newValue; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }
}
