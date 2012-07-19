/**
 * $Id$
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

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.networking.NetworkConfiguration;
import com.untangle.uvm.networking.InterfaceConfiguration;

public class OpenVpnManager
{
    static final String OPENVPN_CONF_DIR      = "/etc/openvpn";
    private static final String OPENVPN_SERVER_FILE   = OPENVPN_CONF_DIR + "/server.conf";
    private static final String OPENVPN_CCD_DIR       = OPENVPN_CONF_DIR + "/ccd";
    private static final String CLIENT_CONF_FILE_BASE = Constants.PACKAGES_DIR + "/client-";

    private static final String VPN_START_SCRIPT = Constants.SCRIPT_DIR + "/start-openvpn";
    private static final String VPN_STOP_SCRIPT  = Constants.SCRIPT_DIR + "/stop-openvpn";
    private static final String GENERATE_DISTRO_SCRIPT = Constants.SCRIPT_DIR + "/generate-distro";

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

    static final int OPENVPN_PORT        = 1194;

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

        UvmContextFactory.context().execManager().exec( VPN_START_SCRIPT );

        try {
            UvmContextFactory.context().networkManager().refreshNetworkConfig();
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

        UvmContextFactory.context().execManager().exec( VPN_STOP_SCRIPT );

        try {
            UvmContextFactory.context().networkManager().refreshNetworkConfig();
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
    }

    private void writeSettings( VpnSettings settings ) throws Exception
    {
        ScriptWriter sw = new VpnScriptWriter();

        /* Insert all of the default parameters */
        sw.appendLines( SERVER_DEFAULTS );

        /* May want to expose this in the GUI */
        sw.appendVariable( FLAG_PORT, String.valueOf( VpnSettings.DEFAULT_PUBLIC_PORT ));

        sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );
        IPAddress localEndpoint  = settings.getServerAddress().getIp();
        IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );

        sw.appendVariable( FLAG_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );
        writePushRoute( sw, localEndpoint, null );

        /* Get all of the routes for all of the different groups */
        writeGroups( sw, settings );

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
        for ( SiteNetwork siteNetwork : settings.getExportedAddressList()) {
            if ( !siteNetwork.getLive()) continue;

            writePushRoute( sw, siteNetwork.getNetwork(), siteNetwork.getNetmask());
        }

        Map<String,VpnGroup> groupMap = buildGroupMap(settings);

        /* The client configuration file is written in writeClientFiles */
        for ( VpnSite site : settings.getSiteList()) {
            VpnGroup group = groupMap.get(site.getGroupName());

            if ( !site.trans_isEnabled() || ( group == null ) || !group.getLive()) {
                continue;
            }

            for ( SiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.getLive()) continue;

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
            if ( !group.getLive()) continue;

            writeRoute( sw, group.getAddress(), group.getNetmask());
        }

        sw.appendLine();
    }

    /**
     * Create all of the client configuration files
     */
    void writeClientConfigurationFiles( VpnSettings settings, VpnClient client, String method )
        throws Exception
    {
        Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-openvpn");
        I18nUtil i18nUtil = new I18nUtil(i18nMap);
        String title = "OpenVPN";
        String msgBody1 = "", msgBody2 = "", msgBody3 = "";
        if (client.trans_isUntanglePlatform()) {
            msgBody1 = i18nUtil.tr("Please input the following information into the \"VPN Client\" wizard.");
            msgBody2 = i18nUtil.tr("Server Address:");
            msgBody3 = i18nUtil.tr("Passphrase:");
        } else {
            msgBody1 = i18nUtil.tr("Click here to download the OpenVPN client.");
            msgBody2 = i18nUtil.tr("Or copy and paste the following link into your Web Browser.");
        }

        String publicUrl = UvmContextFactory.context().systemManager().getPublicUrl();
        writeClientConfigurationFile( settings, client, UNIX_CLIENT_DEFAULTS, UNIX_EXTENSION );
        writeClientConfigurationFile( settings, client, WIN_CLIENT_DEFAULTS,  WIN_EXTENSION );

        if (logger.isDebugEnabled()) {
            logger.debug( "Executing: " + GENERATE_DISTRO_SCRIPT + "[" + method + "]" );
        }

        try {
            String key = client.getDistributionKey();
            if ( key == null ) key = "";

            String cmdStr = GENERATE_DISTRO_SCRIPT + " " +
                "\"" + client.trans_getInternalName() + "\"" + " " +
                "\"" + key + "\"" + " " +
                "\"" + publicUrl + "\"" + " " +
                "\"" + method + "\"" + " " +
                "\"" + String.valueOf( client.trans_isUntanglePlatform()) + "\"" + " " +
                "\"" + settings.trans_getInternalSiteName() + "\"" + " " +
                "\"" + title + "\"" + " " +
                "\"" + msgBody1 + "\"" + " " +
                "\"" + msgBody2 + "\"" + " " +
                "\"" + msgBody3 + "\"";

            UvmContextFactory.context().execManager().exec(cmdStr);
        } catch ( Exception e ) {
            logger.warn( "Unable to execute distribution script", e );
            throw e;
        }
    }

    /*
     * Write a client configuration file (unix or windows)
     */
    private void writeClientConfigurationFile( VpnSettings settings, VpnClient client, String[] defaults, String extension )
        throws Exception
    {
        ScriptWriter sw = new VpnScriptWriter();

        /* Insert all of the default parameters */
        sw.appendLines( CLIENT_DEFAULTS );
        sw.appendLines( defaults );
        sw.appendVariable( FLAG_DEVICE, DEVICE_ROUTING );

        String name = client.trans_getInternalName();
        String siteName = settings.trans_getInternalSiteName();

        sw.appendVariable( FLAG_CERT, CLI_KEY_DIR + "/" + siteName + "-" + name + ".crt" );
        sw.appendVariable( FLAG_KEY,  CLI_KEY_DIR + "/" + siteName + "-" + name + ".key" );
        sw.appendVariable( FLAG_CA,   CLI_KEY_DIR + "/" + siteName + "-ca.crt" );

        String publicAddress = UvmContextFactory.context().systemManager().getPublicUrl();

        /* Strip off the port, (This guarantees if they set it to a hostname the value will be correct) */
        publicAddress = publicAddress.split( ":" )[0];
        publicAddress = publicAddress.trim();

        sw.appendVariable( FLAG_REMOTE, publicAddress + " " + OPENVPN_PORT );
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
        NetworkConfiguration networkSettings = UvmContextFactory.context().networkManager().getNetworkConfiguration();

        Map<String,VpnGroup> groupMap = buildGroupMap(settings);

        for ( VpnClient client : settings.getClientList()) {
            VpnGroup group = groupMap.get(client.getGroupName());

            if ( !client.trans_isEnabled() || ( group == null ) || !group.getLive()) {
                continue;
            }

            ScriptWriter sw = new VpnScriptWriter();

            IPAddress localEndpoint  = client.getAddress();
            IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
            String name           = client.trans_getInternalName();

            logger.info( "Writing client configuration file for [" + name + "]" );

            /* XXXX This won't work for a bridge configuration */
            sw.appendVariable( FLAG_CLI_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );

            if(group.getFullTunnel()) {
                sw.appendVariable( "push", "\"redirect-gateway def1\"");
            }

            if(group.getUseDNS()) {
                List<IPAddress> dnsServers = null;

                if ( settings.getIsDnsOverrideEnabled()) {
                    dnsServers = settings.trans_getDnsServerList();
                } else {
                    dnsServers = new LinkedList<IPAddress>();
                    for (InterfaceConfiguration intf : networkSettings.getInterfaceList()) {
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

            if ( !site.trans_isEnabled() || ( group == null ) || !group.getLive()) {
                continue;
            }

            ScriptWriter sw = new VpnScriptWriter();

            IPAddress localEndpoint  = site.getAddress();
            IPAddress remoteEndpoint = getRemoteEndpoint( localEndpoint );
            String name           = site.trans_getInternalName();

            sw.appendVariable( FLAG_CLI_IFCONFIG, "" + localEndpoint + " " + remoteEndpoint );

            for ( SiteNetwork siteNetwork : site.getExportedAddressList()) {
                if ( !siteNetwork.getLive()) continue;

                writeClientRoute( sw, siteNetwork.getNetwork(), siteNetwork.getNetmask());
            }

            logger.info( "Writing site configuration file for [" + name + "]: " + OPENVPN_CCD_DIR + "/" + name);
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
