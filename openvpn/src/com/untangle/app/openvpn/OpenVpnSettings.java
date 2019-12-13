/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.*;

import org.json.JSONObject;
import org.json.JSONString;

import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Settings for the open vpn app.
 */
@SuppressWarnings("serial")
public class OpenVpnSettings implements java.io.Serializable, JSONString
{

    private Integer version = 0;

    public static enum AuthenticationType
    {
        NONE, LOCAL_DIRECTORY, RADIUS, ACTIVE_DIRECTORY, ANY_DIRCON
    };

    private static final int DEFAULT_PING_TIME    = 10;
    private static final int DEFAULT_PING_TIMEOUT = 60;
    private static final int DEFAULT_VERBOSITY    = 1;
    static final int MANAGEMENT_PORT              = 1195;

    private String protocol = "udp"; /* "tcp" or "udp" */
    private int port = 1194;
    private String cipher = "AES-128-CBC";
    private boolean clientToClient = true;
    
    private String siteName = "untangle";
    private IPMaskedAddress addressSpace;

    private boolean serverEnabled = false;
    private boolean natOpenVpnTraffic = true;
    private boolean authUserPass = false;
    private AuthenticationType authenticationType = AuthenticationType.LOCAL_DIRECTORY;
    
    private LinkedList<OpenVpnConfigItem> clientConfiguration;
    private LinkedList<OpenVpnConfigItem> serverConfiguration;
    
    /**
     * List of addresses visible to those connecting to the VPN
     */
    private List<OpenVpnExport> exports = new LinkedList<>();

    /**
     * List of the various group of remote clients
     */
    private List<OpenVpnGroup> groups = new LinkedList<>();

    /**
     * List of all the remote clients
     */
    private List<OpenVpnRemoteClient> remoteClients = new LinkedList<>();

    /**
     * List of all the remote servers
     */
    private List<OpenVpnRemoteServer> remoteServers = new LinkedList<>();

    public OpenVpnSettings()
    {
        serverConfiguration = new LinkedList<>();
        
        serverConfiguration.add(new OpenVpnConfigItem("mode", "server", true));
        serverConfiguration.add(new OpenVpnConfigItem("multihome", true));
        serverConfiguration.add(new OpenVpnConfigItem("ca", "data/ca.crt", true));
        serverConfiguration.add(new OpenVpnConfigItem("cert", "data/server.crt", true));
        serverConfiguration.add(new OpenVpnConfigItem("key", "data/server.key", true));
        serverConfiguration.add(new OpenVpnConfigItem("dh", "data/dh.pem", true));
        serverConfiguration.add(new OpenVpnConfigItem("client-config-dir", "ccd", true));
        serverConfiguration.add(new OpenVpnConfigItem("keepalive", Integer.toString(DEFAULT_PING_TIME) + " " + Integer.toString(DEFAULT_PING_TIMEOUT), true));
        serverConfiguration.add(new OpenVpnConfigItem("user", "nobody", true));
        serverConfiguration.add(new OpenVpnConfigItem("group", "nogroup", true));
        serverConfiguration.add(new OpenVpnConfigItem("tls-server", true));
        serverConfiguration.add(new OpenVpnConfigItem("compress", true));
        serverConfiguration.add(new OpenVpnConfigItem("status", "openvpn-status.log", true));
        serverConfiguration.add(new OpenVpnConfigItem("log", "/var/log/openvpn.log", true));
        serverConfiguration.add(new OpenVpnConfigItem("verb", Integer.toString(DEFAULT_VERBOSITY), true));
        serverConfiguration.add(new OpenVpnConfigItem("dev", "tun0", true));
        serverConfiguration.add(new OpenVpnConfigItem("max-clients", "2048", true));

        /* Only talk to clients with a client configuration file */
        serverConfiguration.add(new OpenVpnConfigItem("ccd-exclusive", true));

        /* Do not re-read key after SIGUSR1 */
        serverConfiguration.add(new OpenVpnConfigItem("persist-key", true));
        
        /* Do not re-init tun0 after SIGUSR1 */
        serverConfiguration.add(new OpenVpnConfigItem("persist-tun", true));
        
        /* Stop logging repeated messages (after 20). */
        serverConfiguration.add(new OpenVpnConfigItem("mute", "20", true));

        /* persist pool address assignments */
        serverConfiguration.add(new OpenVpnConfigItem("ifconfig-pool-persist", "/etc/openvpn/address-pool-assignments.txt", true));
        
        /* push register-dns to reset DNS on windows machines */
        serverConfiguration.add(new OpenVpnConfigItem("push", "\"register-dns\"", true));

        clientConfiguration = new LinkedList<>();
        
        clientConfiguration.add(new OpenVpnConfigItem("client", true));
        clientConfiguration.add(new OpenVpnConfigItem("resolv-retry", "20", true));
        clientConfiguration.add(new OpenVpnConfigItem("keepalive", Integer.toString(DEFAULT_PING_TIME) + " " + Integer.toString(DEFAULT_PING_TIMEOUT), true));
        clientConfiguration.add(new OpenVpnConfigItem("nobind", true));
        clientConfiguration.add(new OpenVpnConfigItem("mute-replay-warnings", true));
        clientConfiguration.add(new OpenVpnConfigItem("remote-cert-tls", "server", true));
        clientConfiguration.add(new OpenVpnConfigItem("compress", true));
        clientConfiguration.add(new OpenVpnConfigItem("verb", Integer.toString(DEFAULT_VERBOSITY), true));
        
        /* Do not re-read key after SIGUSR1 */
        clientConfiguration.add(new OpenVpnConfigItem("persist-key", true));
        
        /* Do not re-init tun0 after SIGUSR1 */
        clientConfiguration.add(new OpenVpnConfigItem("persist-tun", true));
        
        /* notify server when exitting */
        clientConfiguration.add(new OpenVpnConfigItem("explicit-exit-notify", "1", true));
        
        /* device */
        clientConfiguration.add(new OpenVpnConfigItem("dev", "tun", true));
    }

    public Integer getVersion() { return version; }
    public void setVersion( Integer newValue ) { this.version = newValue; }

    public LinkedList<OpenVpnConfigItem> getClientConfiguration(List<String> specificConfigItems) {
        return this.clientConfiguration.stream()
        .filter(t -> specificConfigItems.contains(t.getOptionName()))
        .collect(Collectors.toCollection(LinkedList::new));}
    public LinkedList<OpenVpnConfigItem> getClientConfiguration() { return this.clientConfiguration; }
    public void setClientConfiguration( LinkedList<OpenVpnConfigItem> argList) { this.clientConfiguration = argList; }

    public LinkedList<OpenVpnConfigItem> getServerConfiguration(List<String> specificConfigItems) {
        return this.serverConfiguration.stream()
        .filter(t -> specificConfigItems.contains(t.getOptionName()))
        .collect(Collectors.toCollection(LinkedList::new));}
    public LinkedList<OpenVpnConfigItem> getServerConfiguration() { return this.serverConfiguration; }
    public void setServerConfiguration( LinkedList<OpenVpnConfigItem> argList) { this.serverConfiguration = argList; }

    public boolean getServerEnabled() { return this.serverEnabled; }
    public void setServerEnabled( boolean newValue ) { this.serverEnabled = newValue; }

    public boolean getNatOpenVpnTraffic() { return this.natOpenVpnTraffic; }
    public void setNatOpenVpnTraffic( boolean newValue ) { this.natOpenVpnTraffic = newValue; }

    public AuthenticationType getAuthenticationType() { return this.authenticationType; }
    public void setAuthenticationType( AuthenticationType argValue ) { this.authenticationType = argValue; }

    public boolean getAuthUserPass() { return this.authUserPass; }
    public void setAuthUserPass( boolean newValue ) { this.authUserPass = newValue; }

    public String getProtocol() { return this.protocol; }
    public void setProtocol( String newValue ) { this.protocol = newValue; }

    public int getPort() { return this.port; }
    public void setPort( int newValue ) { this.port = newValue; }

    public String getCipher() { return this.cipher; }
    public void setCipher( String newValue ) { this.cipher = newValue; }

    public boolean getClientToClient() { return this.clientToClient; }
    public void setClientToClient( boolean newValue ) { this.clientToClient = newValue; }
    
    public String getSiteName() { return this.siteName; }
    public void setSiteName( String newValue ) { this.siteName = newValue; }

    public IPMaskedAddress getAddressSpace() { return this.addressSpace; }
    public void setAddressSpace( IPMaskedAddress newValue ) { this.addressSpace = newValue; }

    public List<OpenVpnExport> getExports() { return this.exports; }
    public void setExports( List<OpenVpnExport> newValue ) { this.exports = newValue; }

    public List<OpenVpnGroup> getGroups() { return this.groups; }
    public void setGroups( List<OpenVpnGroup> newValue ) { this.groups = newValue; }
    
    public List<OpenVpnRemoteClient> getRemoteClients() { return this.remoteClients; }
    public void setRemoteClients( List<OpenVpnRemoteClient> newValue ) { this.remoteClients = newValue; }

    public List<OpenVpnRemoteServer> getRemoteServers() { return this.remoteServers; }
    public void setRemoteServers( List<OpenVpnRemoteServer> newValue ) { this.remoteServers = newValue; }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
