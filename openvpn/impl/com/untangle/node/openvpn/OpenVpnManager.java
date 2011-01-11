/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.openvpn;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.script.ScriptRunner;
import com.untangle.uvm.node.script.ScriptWriter;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.networking.NetworkSettings;
import com.untangle.uvm.networking.InterfaceSettings;

public class OpenVpnManager
{
    static final String OPENVPN_CONF_DIR      = "/etc/openvpn";
    private static final String OPENVPN_SERVER_FILE   = OPENVPN_CONF_DIR + "/server.conf";
    private static final String OPENVPN_CCD_DIR       = OPENVPN_CONF_DIR + "/ccd";
    private static final String CLIENT_CONF_FILE_BASE = Constants.PACKAGES_DIR + "/client-";

    private static final String VPN_START_SCRIPT = Constants.SCRIPT_DIR + "/start-openvpn";
    private static final String VPN_STOP_SCRIPT  = Constants.SCRIPT_DIR + "/stop-openvpn";
    private static final String GENERATE_DISTRO_SCRIPT = Constants.SCRIPT_DIR + "/generate-distro";

    private static final String PACKET_FILTER_RULES_FILE = System.getProperty( "uvm.conf.dir" ) + "/openvpn/packet-filter-rules";

    /* Most likely want to bind to the outside address when using NAT */
    //unused private static final String FLAG_LOCAL       = "local";

    private static final String FLAG_PORT        = "port";

    //unused private static final String FLAG_PROTOCOL    = "proto";
    //unused private static final String DEFAULT_PROTOCOL = "udp";
    private static final String FLAG_DEVICE      = "dev";
    private static final String DEVICE_BRIDGE    = "tap0";
    private static final String DEVICE_ROUTING   = "tun0";

    private static final String FLAG_ROUTE        = "route";
    private static final String FLAG_IFCONFIG     = "ifconfig";
    private static final String FLAG_CLI_IFCONFIG = "ifconfig-push";
    private static final String FLAG_CLI_ROUTE    = "iroute";
    //unused private static final String FLAG_BRIDGE_GROUP = "server-bridge";

    private static final String FLAG_PUSH         = "push";
    //unused private static final String FLAG_EXPOSE_CLI   = "client-to-client";

    private static final String FLAG_MAX_CLI      = "max-clients";

    static final String FLAG_REMOTE               = "remote";
    private static final String SUPPORTED_CIPHER  = "AES-128-CBC";

    private static final String FLAG_CERT         = "cert";
    private static final String FLAG_KEY          = "key";
    private static final String FLAG_CA           = "ca";

    /* The directory where the key material ends up for a client */
    private static final String CLI_KEY_DIR       = "untangle-vpn";

    /* Ping every x seconds */
    private static final int DEFAULT_PING_TIME      = 10;

    /* If a ping response isn't received in this amount time, assume the connection is dead */
    private static final int DEFAULT_PING_TIMEOUT   = 120;

    /* Default verbosity in the log messages(0-9) *
     * 0 -- No output except fatal errors.
     * 1 to 4 -- Normal usage range.
     * 5  --  Output  R  and W characters to the console for each packet read and write, uppercase is
     * used for TCP/UDP packets and lowercase is used for TUN/TAP packets.
     * 6 to 11 -- Debug info range (see errlevel.h for additional information on debug levels). */
    private static final int DEFAULT_VERBOSITY   = 1;

    /* XXX Just pick one that is unused (this is openvpn + 1) */
    static final int MANAGEMENT_PORT     = 1195;

    /* Key management directives */
    private static final String SERVER_DEFAULTS[] = new String[] {
        "mode server",
        "ca   data/ca.crt",
        "cert data/server.crt",
        "key  data/server.key",
        "dh   data/dh.pem",
        // XXX This is only valid if you specify a pool
        // "ifconfig-pool-persist ipp.txt",
        "client-config-dir ccd",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        "cipher " + SUPPORTED_CIPHER,
        "user nobody",
        "group nogroup",

        /* Only talk to clients with a client configuration file */
        /* XXX This may need to go away to support pools */
        "ccd-exclusive",
        "tls-server",
        "comp-lzo",

        /* XXX Be careful, restarts that change the key will not take this into account */
        "persist-key",
        "persist-tun",

        "status openvpn-status.log",
        "verb " + DEFAULT_VERBOSITY,

        /* Stop logging repeated messages (after 20). */
        "mute 20",

        /* Allow management from localhost */
        "management 127.0.0.1 " + MANAGEMENT_PORT        
    };

    private static final String CLIENT_DEFAULTS[] = new String[] {
        "client",
        "proto udp",
        "resolv-retry 20",
        "keepalive " + DEFAULT_PING_TIME + " " + DEFAULT_PING_TIMEOUT,
        "cipher " + SUPPORTED_CIPHER,
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
    };

    private static final String WIN_CLIENT_DEFAULTS[]  = new String[] {};
    private static final String WIN_EXTENSION          = "ovpn";


    private static final String UNIX_CLIENT_DEFAULTS[] = new String[] {
        // ??? Questionable because not all installs will have these users and groups.
        // "user nobody",
        // "group nogroup"
    };

    private static final String UNIX_EXTENSION         = "conf";

    private final Logger logger = Logger.getLogger( this.getClass());

    OpenVpnManager()
    {
    }

    void start( VpnSettings settings ) throws Exception
    {
        logger.info( "Starting openvpn server" );

        ScriptRunner.getInstance().exec( VPN_START_SCRIPT );

        try {
            // XXX ALPACA_INTEGRATION
            /* ** XXXXXXX Bridge mode is unsupported */
//             LocalUvmContextFactory.context().localIntfManager().
//                 registerIntf( DEVICE_ROUTING, IntfConstants.VPN_INTF );

            /* ** XXXXXXX Bridge mode is unsupported */

            // if ( isBridgeMode ) {
            // am.enableInternalBridgeIntf( LocalUvmContextFactory.context().networkingManager().get(), intf );
            // }
            LocalUvmContextFactory.context().networkManager().refreshNetworkConfig();
//         } catch ( ArgonException e ) {
//             throw new Exception( e );
        } catch ( Exception e ) {
            throw new Exception( e );
        }
    }

    void restart( VpnSettings settings ) throws Exception
    {
        /* The start script handles the case where it has to be stopped */
        start( settings );
    }

    void stop() throws Exception
    {
        logger.info( "Stopping openvpn server" );
        ScriptRunner.getInstance().exec( VPN_STOP_SCRIPT );

        try {
            //
            // am.disableInternalBridgeIntf( LocalUvmContextFactory.context().networkingManager().get());
            LocalUvmContextFactory.context().networkManager().refreshNetworkConfig();
        } catch ( Exception e ) {
            throw new Exception( e );
        }
    }

    void configure( VpnSettings settings ) throws Exception
    {
        /* Nothing to start */
        if ( settings.isUntanglePlatformClient()) {
            return;
        }

        writeSettings( settings );
        writeClientFiles( settings );
        writePacketFilterRules( settings );
    }

    private void writeSettings( VpnSettings settings ) throws Exception
    {
        ScriptWriter sw = new VpnScriptWriter();

        /* Insert all of the default parameters */
        sw.appendLines( SERVER_DEFAULTS );

        /* May want to expose this in the GUI */
        sw.appendVariable( FLAG_PORT, String.valueOf( VpnSettings.DEFAULT_PUBLIC_PORT ));

        /* Bridging or routing */
        if ( settings.isBridgeMode()) {
            sw.appendVariable( FLAG_DEVICE, DEVICE_BRIDGE );
        } else {
            sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );
            IPAddress localEndpoint  = settings.getServerAddress().getIp();
            IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );

            sw.appendVariable( FLAG_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );
            writePushRoute( sw, localEndpoint, null );

            /* Get all of the routes for all of the different groups */
            writeGroups( sw, settings );
        }

        writeExports( sw, settings );

        int maxClients = settings.getMaxClients();
        if ( maxClients > 0 ) sw.appendVariable( FLAG_MAX_CLI, String.valueOf( maxClients ));

        sw.writeFile( OPENVPN_SERVER_FILE );
    }

    private void writeExports( ScriptWriter sw, VpnSettings settings )
    {
        sw.appendComment( "Exports" );

        /* XXX This may need additional entries in the routing table,
         * because the edgeguard must also know how to route this
         * traffic
         * not sure about this comment, the entries seem to get
         * pushed automatically. */
        for ( ServerSiteNetwork siteNetwork : settings.getExportedAddressList()) {
            if ( !siteNetwork.isLive()) continue;

            writePushRoute( sw, siteNetwork.getNetwork(), siteNetwork.getNetmask());
        }
        
        Map<String,VpnGroup> groupMap = buildGroupMap(settings);

        /* The client configuration file is written in writeClientFiles */
        for ( VpnSite site : settings.getSiteList()) {
            VpnGroup group = groupMap.get(site.getGroupName());
            
            if ( !site.isEnabled() || ( group == null ) || !group.isLive()) {
                continue;
            }

            for ( ClientSiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;

                IPAddress network = siteNetwork.getNetwork();
                IPAddress netmask = siteNetwork.getNetmask();

                writeRoute( sw, network, netmask );
                writePushRoute( sw, network, netmask );
            }
        }

        sw.appendLine();
    }

    private void writeGroups( ScriptWriter sw, VpnSettings settings )
    {
        /* XXX Need some group consolidation */
        /* XXX Need some checking for overlapping groups */
        /* XXX Do not exports groups that are not used */

        sw.appendComment( "Groups" );

        for ( VpnGroup group : settings.getGroupList()) {
            if ( !group.isLive()) continue;

            writeRoute( sw, group.getAddress(), group.getNetmask());
        }

        sw.appendLine();
    }

    /**
     * Create all of the client configuration files
     */
    void writeClientConfigurationFiles( VpnSettings settings, VpnClientBase client, String method )
        throws Exception
    {
        LocalUvmContext uvm = LocalUvmContextFactory.context();
        NetworkManager nm = uvm.networkManager();

        Map<String,String> i18nMap = uvm.languageManager().getTranslations("untangle-node-openvpn");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String title = "OpenVPN";
        String msgBody1 = "", msgBody2 = "", msgBody3 = "";
        if (client.isUntanglePlatform()) {
            msgBody1 = i18nUtil.tr("Please input the following information into the \"VPN Client\" wizard.");
            msgBody2 = i18nUtil.tr("Server Address:");
            msgBody3 = i18nUtil.tr("Passphrase:");
        } else {
            msgBody1 = i18nUtil.tr("Click here to download the OpenVPN client.");
            msgBody2 = i18nUtil.tr("Or copy and paste the following link into your Web Browser.");
        }
        
        String publicAddress = nm.getPublicAddress();
        writeClientConfigurationFile( settings, client, UNIX_CLIENT_DEFAULTS, UNIX_EXTENSION );
        writeClientConfigurationFile( settings, client, WIN_CLIENT_DEFAULTS,  WIN_EXTENSION );

        if (logger.isDebugEnabled()) {
            logger.debug( "Executing: " + GENERATE_DISTRO_SCRIPT + "[" + method + "]" );
        }

        try {
            String key = client.getDistributionKey();
            if ( key == null ) key = "";

            /* USB Distribution is no longer supported. */
            ScriptRunner.getInstance().exec( GENERATE_DISTRO_SCRIPT, client.getInternalName(),
                                             key, publicAddress, method,
                                             String.valueOf( client.isUntanglePlatform()),
                                             settings.getInternalSiteName(),
                                             title, msgBody1, msgBody2, msgBody3);
        } catch ( Exception e ) {
            logger.warn( "Unable to execute distribution script", e );
            throw e;
        }
    }

    /*
     * Write a client configuration file (unix or windows)
     */
    private void writeClientConfigurationFile( VpnSettings settings, VpnClientBase client,
                                               String[] defaults, String extension )
    {
        ScriptWriter sw = new VpnScriptWriter();

        /* Insert all of the default parameters */
        sw.appendLines( CLIENT_DEFAULTS );
        sw.appendLines( defaults );

        if ( settings.isBridgeMode()) {
            sw.appendVariable( FLAG_DEVICE, DEVICE_BRIDGE );
        } else {
            sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );
        }

        String name = client.getInternalName();
        String siteName = settings.getInternalSiteName();

        sw.appendVariable( FLAG_CERT, CLI_KEY_DIR + "/" + siteName + "-" + name + ".crt" );
        sw.appendVariable( FLAG_KEY,  CLI_KEY_DIR + "/" + siteName + "-" + name + ".key" );
        sw.appendVariable( FLAG_CA,   CLI_KEY_DIR + "/" + siteName + "-ca.crt" );

        /* VPN configuratoins needs information from the networking settings. */
        NetworkManager networkManager = LocalUvmContextFactory.context().networkManager();

        /* This is kind of janky */
        String publicAddress = networkManager.getPublicAddress();

        /* Strip off the port, (This guarantees if they set it to a hostname the value will be
         * correct) */
        publicAddress = publicAddress.split( ":" )[0];

        publicAddress = publicAddress.trim();

        sw.appendVariable( FLAG_REMOTE, publicAddress + " " + settings.getPublicPort());

        sw.writeFile( CLIENT_CONF_FILE_BASE + name + "." + extension );
    }

    private void writeClientFiles( VpnSettings settings )
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
        NetworkSettings networkSettings = LocalUvmContextFactory.context().networkManager().getNetworkSettings();
        
        Map<String,VpnGroup> groupMap = buildGroupMap(settings);

        for ( VpnClient client : settings.getClientList()) {
            VpnGroup group = groupMap.get(client.getGroupName());
            
            if ( !client.isEnabled() || ( group == null ) || !group.isLive()) {
                continue;
            }

            ScriptWriter sw = new VpnScriptWriter();

            IPAddress localEndpoint  = client.getAddress();
            IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
            String name           = client.getInternalName();

            logger.info( "Writing client configuration file for [" + name + "]" );

            /* XXXX This won't work for a bridge configuration */
            sw.appendVariable( FLAG_CLI_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );

            if(group.getUseDNS()) {
                List<IPAddress> dnsServers = null;

                if ( settings.getIsDnsOverrideEnabled()) {
                    dnsServers = settings.getDnsServerList();
                } else {
                    dnsServers = new LinkedList<IPAddress>();
                    for (InterfaceSettings intf : networkSettings.getInterfaceList()) {
                        if (intf.isWAN()) {
                            if (intf.getDns1() != null)
                                dnsServers.add(new IPAddress(intf.getDns1()));
                            if (intf.getDns2() != null)
                                dnsServers.add(new IPAddress(intf.getDns2()));
                        }
                    }
                }

                for(IPAddress addr : dnsServers) {
                    sw.appendVariable( "push", "\"dhcp-option DNS " + addr.toString() + "\"");
                }

                String localDomain = networkSettings.getDnsLocalDomain();
                //If the domain is set - push it
                if(localDomain != null) {
                    sw.appendVariable( "push", "\"dhcp-option DOMAIN " + localDomain + "\"");
                }
            }


            sw.writeFile( OPENVPN_CCD_DIR + "/" + name );
        }

        for ( VpnSite site : settings.getSiteList()) {
            VpnGroup group = groupMap.get(site.getGroupName());
            
            if ( !site.isEnabled() || ( group == null ) || !group.isLive()) {
                continue;
            }

            ScriptWriter sw = new VpnScriptWriter();

            IPAddress localEndpoint  = site.getAddress();
            IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
            String name           = site.getInternalName();

            logger.info( "Writing site configuration file for [" + name + "]" );

            /* XXXX This won't work for a bridge configuration */
            sw.appendVariable( FLAG_CLI_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );

            for ( ClientSiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.isLive()) continue;

                writeClientRoute( sw, siteNetwork.getNetwork(), siteNetwork.getNetmask());
            }

            sw.writeFile( OPENVPN_CCD_DIR + "/" + name );
        }
    }

    private void writePushRoute( ScriptWriter sw, IPAddress address, IPAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write route with null address" );
            return;
        }

        writePushRoute( sw, address.getAddr(), ( netmask == null ) ? null : netmask.getAddr());
    }

    private void writePushRoute( ScriptWriter sw, InetAddress address, InetAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write a push route with null address" );
            return;
        }

        String value = "\"route ";
        if ( netmask != null ) {
            /* the route command complains you do not pass in the base address */
            value += IPAddress.and( new IPAddress(address ), new IPAddress(netmask ));
            value += " " + netmask.getHostAddress();
        } else {
            value += address.getHostAddress();
        }

        value += "\"";

        sw.appendVariable( FLAG_PUSH,  value );
    }

    private void writeRoute( ScriptWriter sw, IPAddress address, IPAddress netmask )
    {
        writeRoute( sw, FLAG_ROUTE, address, netmask );
    }

    private void writeClientRoute( ScriptWriter sw, IPAddress address, IPAddress netmask )
    {
        writeRoute( sw, FLAG_CLI_ROUTE, address, netmask );
    }

    private void writeRoute( ScriptWriter sw, String type, IPAddress address, IPAddress netmask )
    {
        if ( address == null || address.getAddr() == null ) {
            logger.warn( "attempt to write a route with a null address" );
            return;
        }

        String value = "";

        if ( netmask != null && ( netmask.getAddr() != null )) {
            value += IPAddress.and( address, netmask );
            value += " " + netmask;
        } else {
            value += address;
        }

        sw.appendVariable( type, value );
    }

    private void writePacketFilterRules( VpnSettings settings )
    {
        AlpacaRulesWriter arw = new AlpacaRulesWriter();
        
        /* Append all of the exported addresses */
        arw.appendExportedAddresses( settings.getExportedAddressList());
        
        arw.writeFile( PACKET_FILTER_RULES_FILE );
    }

    /* A safe function (exceptionless) for InetAddress.getByAddress */
    private IPAddress getByAddress( byte[] data )
    {
        try {
            return new IPAddress(InetAddress.getByAddress( data ));
        } catch ( UnknownHostException e ) {
            logger.error( "Something happened, array should be 4 actually " + data.length + " bytes", e );
        }
        return null;
    }

    /* For Tunnel nodes, this gets the corresponding remote endpoint, see the openvpn howto for
     * the definition of a remote and local endpoint */
    private IPAddress getRemoteEndpoint( IPAddress localEndpoint )
    {
        byte[] data = localEndpoint.getAddr().getAddress();
        data[3] += 1;
        return getByAddress( data );
    }

    public static Map<String, VpnGroup> buildGroupMap(VpnSettings settings) {
        Map<String,VpnGroup> groupMap = new HashMap<String,VpnGroup>();
        for ( VpnGroup group : settings.getGroupList()) {
            groupMap.put(group.getName(), group);
        }
        return groupMap;
    }
}
