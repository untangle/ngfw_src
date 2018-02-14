/**
 * $Id$
 */
package com.untangle.app.openvpn;

import java.net.InetAddress;

@SuppressWarnings("serial")
public class OpenVpnRemoteServer implements java.io.Serializable
{
    /**
     * Is this remote server enabled?
     */
    private boolean enabled = true;

    /**
     * Name of the remote server
     */
    private String name;
    private boolean authUserPass = false;
    private String authUsername;
    private String authPassword;

    public OpenVpnRemoteServer() {}

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
}
