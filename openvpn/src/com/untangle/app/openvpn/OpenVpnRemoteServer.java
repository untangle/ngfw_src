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
    private OpenVpnConfFile openvpnConfFile;
    private String remoteServerEncryptedPassword;

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

    public String getRemoteServerEncryptedPassword() { return this.remoteServerEncryptedPassword; }
    public void setRemoteServerEncryptedPassword( String newValue ) { this.remoteServerEncryptedPassword = newValue; }

    public OpenVpnConfFile getOpenvpnConfFile() { return openvpnConfFile; }
    public void setOpenvpnConfFile(OpenVpnConfFile openvpnConfFile) { this.openvpnConfFile = openvpnConfFile; }

// THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        org.json.JSONObject jO = new org.json.JSONObject(this);
        return jO.toString();
    }

    /**
     * Represents an OpenVPN configuration file with its encoded contents and encoding format.
     * 
     * <p>This class is typically used to hold the contents of an OpenVPN configuration file
     * encoded in a specific format (e.g., Base64). It can be deserialized from a JSON structure
     * where the configuration content and encoding method are provided.</p>
     */
    public static class OpenVpnConfFile {
        private String contents;
        private String encoding = "base64";

        public OpenVpnConfFile() {
        }

        public String getContents() {
            return contents;
        }

        public void setContents(String contents) {
            this.contents = contents;
        }

        public String getEncoding() {
            return encoding;
        }

        public void setEncoding(String encoding) {
            this.encoding = encoding;
        }
    }
}
