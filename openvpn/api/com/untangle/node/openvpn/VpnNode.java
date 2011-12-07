package com.untangle.node.openvpn;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.ValidateException;
import com.untangle.uvm.node.Validator;

public interface VpnNode extends Node
{
    public enum ConfigFormat
    {
        SETUP_EXE,
        ZIP;
    };
    
    public void setVpnSettings( VpnSettings settings ) throws ValidateException;
    public VpnSettings getVpnSettings();

    /* Create a client certificate, if the client already has a certificate
     * this will automatically revoke their old one */
    public VpnClientBase generateClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Revoke a client license */
    public VpnClientBase revokeClientCertificate( VpnSettings settings, VpnClientBase client );

    /* Need the address to log where the request came from */
    public String lookupClientDistributionKey( String key, IPAddress address );

    /* Returns a URL to use to download the admin key. */
    public String getAdminDownloadLink( String clientName, ConfigFormat format )
        throws Exception;
     
    /* Returns true if this is the correct authentication key for
     * downloading keys as the administrator */
    public boolean isAdminKey( String key );

    /* Send out the client distribution */
    public void distributeClientConfig( VpnClientBase client ) throws Exception;

    public enum ConfigState { UNCONFIGURED, CLIENT, SERVER_BRIDGE, SERVER_ROUTE }
    public ConfigState getConfigState();
    public HostAddress getVpnServerAddress();

    public void startConfig(ConfigState state) throws ValidateException;

    /* Retrieve the link to use as a post to upload client configuration files. */
    public String getAdminClientUploadLink();
    public void completeConfig() throws Exception;

    /* This installs a client configuration that is somewhere on the
     * file system.  On success, this makes a copy of the
     * configuration. */
    public void installClientConfig( String path ) throws Exception;

    public void generateCertificate( CertificateParameters parameters ) throws Exception;
    public GroupList getAddressGroups() throws Exception;
    public void setAddressGroups( GroupList parameters ) throws Exception;
    public ExportList getExportedAddressList();
    public void setExportedAddressList( ExportList parameters ) throws Exception;
    public void setClients( ClientList parameters ) throws Exception;
    public void setSites( SiteList parameters ) throws Exception;

    /**
     * Access the EventManager for ClientConnectEvents
     */
    public EventManager<ClientConnectEvent> getClientActiveEventManager();
    /**
     * Access the EventManager for ClientConnectEvents
     */
    public EventManager<ClientConnectEvent> getClientClosedEventManager();

    public Validator getValidator();
}
