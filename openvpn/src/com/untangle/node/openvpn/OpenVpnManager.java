/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * This class has all the logic for "managing" the openVPN daemon.
 * This includes writing all the server and client config files
 * and starting/stopping the daemon
 */
public class OpenVpnManager
{
    private final Logger logger = Logger.getLogger( this.getClass());

    private static final String OPENVPN_CONF_DIR      = "/etc/openvpn";
    private static final String OPENVPN_SERVER_FILE   = OPENVPN_CONF_DIR + "/server.conf";
    private static final String OPENVPN_CCD_DIR       = OPENVPN_CONF_DIR + "/ccd";
    private static final String CLIENT_CONF_FILE_BASE = System.getProperty( "uvm.conf.dir" ) + "/openvpn" + "/clients" + "/client-";

    private static final String VPN_START_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-start";
    private static final String VPN_STOP_SCRIPT  = System.getProperty( "uvm.bin.dir" ) + "/openvpn-stop";
    private static final String GENERATE_DISTRO_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-generate-distro";

    /**
     * Ping every x seconds
     */
    private static final int DEFAULT_PING_TIME      = 10;

    /**
     * If a ping response isn't received in this amount time, assume the connection is dead
     */
    private static final int DEFAULT_PING_TIMEOUT   = 60;

    /**
     * Default verbosity in the log messages(0-9) 
     * 0 -- No output except fatal errors.
     * 1 to 4 -- Normal usage range.
     * 5  --  Output  R  and W characters to the console for each packet read and write, uppercase is
     * used for TCP/UDP packets and lowercase is used for TUN/TAP packets.
     * 6 to 11 -- Debug info range (see errlevel.h for additional information on debug levels).
     */
    private static final int DEFAULT_VERBOSITY   = 1;

    /**
     * Just pick one that is unused (this is openvpn + 1)
     */
    static final int MANAGEMENT_PORT     = 1195;

    /**
     * Defaults for the server.conf
     */
    private static final String SERVER_DEFAULTS[] = new String[] {
        "mode server",
        "multihome",
        "ca   data/ca.crt",
        "cert data/server.crt",
        "key  data/server.key",
        "dh   data/dh.pem",
        
        // "ifconfig-pool-persist ipp.txt", // XXX should this be enabled?
        "client-config-dir ccd",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        "user nobody",
        "group nogroup",

        "ccd-exclusive", /* Only talk to clients with a client configuration file */
        "tls-server",
        "comp-lzo",

        "persist-key",
        "persist-tun",

        "status openvpn-status.log",
        "verb " + DEFAULT_VERBOSITY,

        /* Stop logging repeated messages (after 20). */
        "mute 20",

        /* Allow management from localhost */
        "management 127.0.0.1 " + MANAGEMENT_PORT,

        /* max clients */
        "max-clients 2048",

        /* device */
        "dev tun0"
    };

    /**
     * Defaults for the client.conf
     */
    private static final String CLIENT_DEFAULTS[] = new String[] {
        "client",
        "resolv-retry 20",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        "nobind",
        "mute-replay-warnings",
        "ns-cert-type server",
        "comp-lzo",
        "verb 2",
        "persist-key",
        "persist-tun",
        "verb " + DEFAULT_VERBOSITY,
        /* Exit if unable to connect to the server */
        "tls-exit",
        /* device */
        "dev tun0"
    };

    private static final String WIN_CLIENT_DEFAULTS[]  = new String[] {};
    private static final String WIN_EXTENSION          = "ovpn";

    private static final String UNIX_CLIENT_DEFAULTS[] = new String[] {};
    private static final String UNIX_EXTENSION         = "conf";

    protected OpenVpnManager() { }

    void start( OpenVpnSettings settings ) throws Exception
    {
        logger.info( "Starting openvpn server" );

        ExecManagerResult result = UvmContextFactory.context().execManager().exec( VPN_START_SCRIPT );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( VPN_START_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( VPN_START_SCRIPT + ": " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to start OpenVPN daemon (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to start OpenVPN daemon");
        }

        //FIXME update iptables rules
    }

    void stop() throws Exception
    {
        logger.info( "Stopping openvpn server" );

        ExecManagerResult result = UvmContextFactory.context().execManager().exec( VPN_STOP_SCRIPT );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( VPN_STOP_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( VPN_STOP_SCRIPT + ": " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 )
            logger.error("Failed to stop OpenVPN daemon (return code: " + result.getResult() + ")");
        
        //FIXME update iptables rules
    }

    void configure( OpenVpnSettings settings ) throws Exception
    {
        writeSettings( settings );
        writeClientFiles( settings );
    }

    private void writeSettings( OpenVpnSettings settings ) throws Exception
    {
        StringBuilder sb = new StringBuilder();

        for ( String line : SERVER_DEFAULTS ) {
            sb.append( line + "\n" );
        }

        /* May want to expose this in the GUI */
        sb.append( "proto" + " " + settings.getProtocol() + "\n" );
        sb.append( "port" + " " + settings.getPort() + "\n");
        sb.append( "cipher" + " " + settings.getCipher() + "\n");

        //InetAddress localEndpoint  = getLocalEndpoint( settings.getAddressSpace() );
        //InetAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
        //sb.append( "ifconfig" + " " +  localEndpoint.getHostAddress() + " " + remoteEndpoint.getHostAddress() + "\n");
        //writePushRoute( sb, localEndpoint, null );

        sb.append( "server" + " " +  settings.getAddressSpace().getMaskedAddress().getHostAddress() + " " + settings.getAddressSpace().getNetmask().getHostAddress() + "\n");
        // XXX necessary to write route for this network?
        // writeRoute( sb, settings.getAddressSpace().getMaskedAddress(), settings.getAddressSpace().getNetmask());

        writeExports( sb, settings );

        writeFile( OPENVPN_SERVER_FILE, sb );
    }

    private void writeExports( StringBuilder sb, OpenVpnSettings settings )
    {
        sb.append( "# Exports\n" );

        for ( OpenVpnExport export : settings.getExports() ) {
            if ( export.getEnabled() ) {
                writePushRoute( sb, export.getNetwork().getMaskedAddress(), export.getNetwork().getNetmask() );
            }
        }

        /* The client configuration file is written in writeClientFiles */
        for ( OpenVpnRemoteClient client : settings.getRemoteClients() ) {
            if ( !client.getEnabled() || !client.getExport() || client.getExportNetwork() == null )
                continue;

            IPMaskedAddress maskedAddr = client.getExportNetwork();
            
            writeRoute( sb, maskedAddr.getMaskedAddress(), maskedAddr.getNetmask() );
            writePushRoute( sb, maskedAddr.getMaskedAddress(), maskedAddr.getNetmask() );
        }

        sb.append("\n");
    }

    /**
     * Create all of the client configuration files
     */
    void writeClientConfigurationFiles( OpenVpnSettings settings, OpenVpnRemoteClient client )
    {
        String publicUrl = UvmContextFactory.context().systemManager().getPublicUrl();

        writeClientConfigurationFile( settings, client, UNIX_CLIENT_DEFAULTS, UNIX_EXTENSION );
        writeClientConfigurationFile( settings, client, WIN_CLIENT_DEFAULTS,  WIN_EXTENSION );

        logger.debug( "Executing: " + GENERATE_DISTRO_SCRIPT );

        String cmdStr = GENERATE_DISTRO_SCRIPT + " " +
            "\"" + client.getName() + "\"" + " " +
            "\"" + publicUrl + "\"" + " " +
            "\"" + settings.getSiteName() + "\"";

        ExecManagerResult result = UvmContextFactory.context().execManager().exec(cmdStr);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_DISTRO_SCRIPT + ": ");
            for ( String line : lines )
                logger.info(GENERATE_DISTRO_SCRIPT + ": " + line);
        } catch (Exception e) {}
        
    }
    
    /*
     * Write a client configuration file (unix or windows)
     */
    private void writeClientConfigurationFile( OpenVpnSettings settings, OpenVpnRemoteClient client, String[] defaults, String extension )
    {
        final String CLI_KEY_DIR = "untangle-openvpn";
        StringBuilder sb = new StringBuilder();

        /* Insert all of the default parameters */
        for ( String line : CLIENT_DEFAULTS ) {
            sb.append( line + "\n" );
        }
        for ( String line : defaults ) {
            sb.append( line + "\n" );
        }

        sb.append( "proto" + " " + settings.getProtocol() + "\n" );
        sb.append( "port" + " " + settings.getPort() + "\n" );
        sb.append( "cipher" + " " + settings.getCipher() + "\n" );

        String name = client.getName();
        String siteName = settings.getSiteName();

        sb.append( "cert" + " " + CLI_KEY_DIR + "/" + siteName + "-" + name + ".crt" + "\n");
        sb.append( "key"  + " " + CLI_KEY_DIR + "/" + siteName + "-" + name + ".key" + "\n");
        sb.append( "ca"   + " " + CLI_KEY_DIR + "/" + siteName + "-ca.crt" + "\n");

        //FIXME this should be shown in the UI with a link to where to configure it
        String publicAddress = UvmContextFactory.context().systemManager().getPublicUrl();

        /* Strip off the port, (This guarantees if they set it to a hostname the value will be correct) */
        publicAddress = publicAddress.split( ":" )[0];
        publicAddress = publicAddress.trim();

        sb.append( "remote" + " " + publicAddress + " " + settings.getPort() + "\n");
        writeFile( CLIENT_CONF_FILE_BASE + name + "." + extension, sb );
    }

    private void writeClientFiles( OpenVpnSettings settings )
    {
        /* Delete the old client files */
        try {
            File baseDirectory = new File( OPENVPN_CCD_DIR );
            if ( baseDirectory.exists()) {
                for ( File clientConfig : baseDirectory.listFiles()) {
                    if (logger.isDebugEnabled()) {
                        logger.debug( "Deleting the file: " + clientConfig );
                    }
                    clientConfig.delete();
                }
            } else {
                baseDirectory.mkdir();
            }
        } catch ( Exception e ) {
            logger.error( "Unable to delete the previous client configuration files." );
        }
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();

        for ( OpenVpnRemoteClient client : settings.getRemoteClients() ) {
            OpenVpnGroup group = getGroup( settings, client.getGroupId() );

            if ( !client.getEnabled() || ( group == null ) ) 
                continue;

            String name = client.getName();
            logger.info( "Writing client configuration file for [" + name + "]" );

            StringBuilder sb = new StringBuilder();

            // FIXME (need AddressMapper?)
            // InetAddress localEndpoint  = client.getAddress();
            // InetAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
            // sb.append( "ifconfig-push" + " " + localEndpoint + " " + remoteEndpoint + "\n");

            if( group.getFullTunnel() ) {
                sb.append( "push" + " " + "\"redirect-gateway def1\"" + "\n");
            }

            if( group.getPushDns() ) {
                List<InetAddress> dnsServers = null;
                dnsServers = new LinkedList<InetAddress>();

                if ( group.getIsDnsOverrideEnabled()) {
                    InetAddress dns1 = group.getDnsOverride1();
                    InetAddress dns2 = group.getDnsOverride2();
                    if ( dns1 != null ) dnsServers.add( dns1 );
                    if ( dns2 != null ) dnsServers.add( dns2 );
                } else {
                    for ( InterfaceSettings intf : UvmContextFactory.context().networkManager().getEnabledInterfaces() ) {
                        /**
                         * Only use statically configured IPv4 DNS servers from WANs
                         * We could use the interface status to export DHCP-acquired DNS settings,
                         * however you would need to rewrite and restart the server each time a new lease was acquired
                         */
                        if ( !intf.getIsWan() || intf.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC )
                            continue;

                        //InetAddress dns1 = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns1();
                        //InetAddress dns2 = UvmContextFactory.context().networkManager().getInterfaceStatus( intf.getInterfaceId() ).getV4Dns2();
                        InetAddress dns1 = intf.getV4StaticDns1();
                        InetAddress dns2 = intf.getV4StaticDns2();
                        if ( dns1 != null) dnsServers.add( dns1 );
                        if ( dns2 != null) dnsServers.add( dns2 );
                    }
                }

                for(InetAddress addr : dnsServers) {
                    sb.append( "push" + " " + "\"dhcp-option DNS " + addr.toString() + "\"" + "\n");
                }

                // FIXME - should be from openvpn settings
                String localDomain = "FIXME";
                //String localDomain = networkSettings.getDnsLocalDomain();
                //If the domain is set - push it
                if(localDomain != null) {
                    sb.append( "push" + " " + "\"dhcp-option DOMAIN " + localDomain + "\"" + "\n");
                }
            }

            if ( client.getExport() && client.getExportNetwork() != null ) {
                writeClientRoute( sb, client.getExportNetwork().getMaskedAddress(), client.getExportNetwork().getNetmask());
            }
            
            writeFile( OPENVPN_CCD_DIR + "/" + name, sb );
        }
    }

    private void writePushRoute( StringBuilder sb, InetAddress address, InetAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write a push route with null address" );
            return;
        }

        String value = "\"route ";
        if ( netmask != null ) {
            IPMaskedAddress maskedAddr = new IPMaskedAddress( address, netmask );
            value += maskedAddr.getMaskedAddress().getHostAddress() + "/" + maskedAddr.getPrefixLength();
            value += " " + netmask.getHostAddress();
        } else {
            value += address.getHostAddress();
        }

        value += "\"";

        sb.append( "push" + " " + value + "\n" );
    }

    private void writeClientRoute( StringBuilder sb, InetAddress address, InetAddress netmask )
    {
        writeRoute( sb, "iroute", address, netmask );
    }

    private void writeRoute( StringBuilder sb, InetAddress address, InetAddress netmask )
    {
        writeRoute( sb, "route", address, netmask );
    }
    
    private void writeRoute( StringBuilder sb, String type, InetAddress address, InetAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write a route with a null address" );
            return;
        }

        String value = "";

        if ( netmask != null ) {
            IPMaskedAddress maddr = new IPMaskedAddress( address, netmask );
            value += maddr.getMaskedAddress().getHostAddress();
            value += " " + netmask;
        } else {
            value += address;
        }

        sb.append( type + " " + value + "\n" );
    }

    /**
     * Gets the corresponding localEndpoint for the provided addressSpace (the first available address in that space)
     */
    private InetAddress getLocalEndpoint( IPMaskedAddress addressSpace )
    {
        InetAddress localEndpoint = addressSpace.getMaskedAddress();
        byte[] data = localEndpoint.getAddress();
        data[3] += 1;
        try {
            return InetAddress.getByAddress( data );
        } catch ( UnknownHostException e ) {
            logger.error( "getByAddress failed: " + data.length + " bytes", e );
        }
        return null;        
    }

    /**
     * This gets the corresponding remote endpoint, see the openvpn howto for
     * the definition of a remote and local endpoint
     */
    private InetAddress getRemoteEndpoint( InetAddress localEndpoint )
    {
        byte[] data = localEndpoint.getAddress();
        data[3] += 1;
        try {
            return InetAddress.getByAddress( data );
        } catch ( UnknownHostException e ) {
            logger.error( "getByAddress failed: " + data.length + " bytes", e );
        }
        return null;        
    }

    private void writeFile( String fileName, StringBuilder sb )
    {
        BufferedWriter out = null;

        try {
            String data = sb.toString();
            out = new BufferedWriter(new FileWriter( fileName ));
            out.write( data, 0, data.length());
        } catch ( Exception ex ) {
            logger.error( "Error writing file " + fileName + ":", ex );
        }

        try {
            if ( out != null ) out.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file", ex );
        }

    }

    private OpenVpnGroup getGroup( OpenVpnSettings settings, int groupId )
    {
        if ( settings.getGroups() == null )
            return null;
        
        for ( OpenVpnGroup group : settings.getGroups() ) {
            if ( group.getId() == groupId )
                return group;
        }

        return null;
    }
}
