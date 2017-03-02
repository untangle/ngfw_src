/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
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

    private static final String VPN_START_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-start";
    private static final String VPN_STOP_SCRIPT  = System.getProperty( "uvm.bin.dir" ) + "/openvpn-stop";
    private static final String GENERATE_ZIP_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-generate-client-zip";
    private static final String GENERATE_EXE_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-generate-client-exec";
    private static final String GENERATE_ONC_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/openvpn-generate-client-onc";
    private static final String IPTABLES_SCRIPT = "/etc/untangle-netd/iptables-rules.d/720-openvpn";
    
    private static final String OPENVPN_CONF_DIR      = "/etc/openvpn";
    private static final String OPENVPN_SERVER_FILE   = OPENVPN_CONF_DIR + "/server.conf";
    private static final String OPENVPN_CCD_DIR       = OPENVPN_CONF_DIR + "/ccd";

    private static final String CLIENT_CONF_FILE_DIR  = "/tmp/openvpn/client-packages/"; 
    private static final String CLIENT_CONF_FILE_BASE = CLIENT_CONF_FILE_DIR + "/client-";

    private static final String WIN_CLIENT_DEFAULTS[]  = new String[] {};
    private static final String WIN_EXTENSION          = "ovpn";

    private static final String UNIX_CLIENT_DEFAULTS[] = new String[] {};
    private static final String UNIX_EXTENSION         = "conf";

    protected OpenVpnManager() { }

    protected void restart()
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

        insertIptablesRules();
    }

    protected void stop()
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
        
        insertIptablesRules(); // remove since openvpn is not running
    }

    protected void configure( OpenVpnSettings settings )
    {
        deleteFiles( );
        writeIptablesFiles( settings );
        writeServerSettings( settings );
        writeRemoteClientFiles( settings );
        writeRemoteServerFiles( settings );
    }


    private void writeConfFiles( OpenVpnSettings settings, OpenVpnRemoteClient client )
    {
        writeRemoteClientConfigurationFile( settings, client, UNIX_CLIENT_DEFAULTS, UNIX_EXTENSION );
        writeRemoteClientConfigurationFile( settings, client, WIN_CLIENT_DEFAULTS,  WIN_EXTENSION );
    }
    /**
     * Create all of the client zip configuration files
     */
    protected void createClientDistributionZip( OpenVpnSettings settings, OpenVpnRemoteClient client )
    {
        writeConfFiles( settings, client );

        String cmdStr;
        ExecManagerResult result;

        cmdStr = GENERATE_ZIP_SCRIPT + " " + "\"" + client.getName() + "\"" + " " + "\"" + settings.getSiteName() + "\"";
        logger.debug( "Executing: " + cmdStr );
        result = UvmContextFactory.context().execManager().exec(cmdStr);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_ZIP_SCRIPT + ": ");
            for ( String line : lines )
                logger.info(GENERATE_ZIP_SCRIPT + ": " + line);
        } catch (Exception e) {}
    }

    /**
     * Create all of the client exe configuration files
     */
    protected void createClientDistributionExe( OpenVpnSettings settings, OpenVpnRemoteClient client )
    {
        writeConfFiles( settings, client );

        String cmdStr;
        ExecManagerResult result;

        cmdStr = GENERATE_EXE_SCRIPT + " " + "\"" + client.getName() + "\"" + " " + "\"" + settings.getSiteName() + "\"";
        logger.debug( "Executing: " + cmdStr );
        result = UvmContextFactory.context().execManager().exec(cmdStr);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_EXE_SCRIPT + ": ");
            for ( String line : lines )
                logger.info(GENERATE_EXE_SCRIPT + ": " + line);
        } catch (Exception e) {}
    }

    /**
     * Create all of the client onc configuration files
     */
    protected void createClientDistributionOnc( OpenVpnSettings settings, OpenVpnRemoteClient client )
    {
        writeConfFiles( settings, client );

        String cmdStr;
        ExecManagerResult result;

        String publicAddress = UvmContextFactory.context().networkManager().getPublicUrl();

        /* Strip off the port, (This guarantees if they set it to a hostname the value will be correct) */
        publicAddress = publicAddress.split( ":" )[0];
        publicAddress = publicAddress.trim();
        
        cmdStr = GENERATE_ONC_SCRIPT + " " + "\"" + client.getName() + "\"" + " " + "\"" + settings.getSiteName() + "\"" + " " + "\"" + publicAddress + "\"";
        logger.debug( "Executing: " + cmdStr );
        result = UvmContextFactory.context().execManager().exec(cmdStr);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( GENERATE_ONC_SCRIPT + ": ");
            for ( String line : lines )
                logger.info(GENERATE_ONC_SCRIPT + ": " + line);
        } catch (Exception e) {}
    }

    private void writeServerSettings( OpenVpnSettings settings )
    {
        if ( ! settings.getServerEnabled() )
            return;

        StringBuilder sb = new StringBuilder();

        for ( OpenVpnConfigItem item : settings.getServerConfiguration()) {
            sb.append( item.toString() + "\n" );
        }

        sb.append( "proto" + " " + settings.getProtocol() + "\n" );
        sb.append( "port" + " " + settings.getPort() + "\n");
        sb.append( "cipher" + " " + settings.getCipher() + "\n");

        if ( settings.getClientToClient() )
            sb.append( "client-to-client" + "\n");
        
        sb.append( "server" + " " +  settings.getAddressSpace().getMaskedAddress().getHostAddress() + " " + settings.getAddressSpace().getNetmask().getHostAddress() + "\n");

        writeExports( sb, settings );

        writeFile( OPENVPN_SERVER_FILE, sb );
    }

    private void writeExports( StringBuilder sb, OpenVpnSettings settings )
    {
        sb.append( "# Exports\n" );

        /**
         * Write the exports
         */
        for ( OpenVpnExport export : settings.getExports() ) {
            if ( export.getEnabled() ) {
                writePushRoute( sb, export.getNetwork().getMaskedAddress(), export.getNetwork().getNetmask() );
            }
        }

        /**
         * Write the remote client networks
         */
        for ( OpenVpnRemoteClient client : settings.getRemoteClients() ) {
            if ( !client.getEnabled() || !client.getExport() || client.getExportNetwork() == null )
                continue;

            for ( String net : client.getExportNetwork().split(",") ) {
                try {
                    IPMaskedAddress maskedAddr = new IPMaskedAddress( net );
                    writeRoute( sb, maskedAddr.getMaskedAddress(), maskedAddr.getNetmask() );
                    writePushRoute( sb, maskedAddr.getMaskedAddress(), maskedAddr.getNetmask() );
                } catch (Exception e) {
                    logger.warn( "Error processing network: " + net, e );
                }
            }
        }

        sb.append("\n");
    }
    
    /**
     * Write a client configuration file (unix or windows)
     */
    private void writeRemoteClientConfigurationFile( OpenVpnSettings settings, OpenVpnRemoteClient client, String[] defaults, String extension )
    {
        final String KEY_DIR = "keys";
        StringBuilder sb = new StringBuilder();

        /* Insert all of the default parameters */
        for ( OpenVpnConfigItem item : settings.getClientConfiguration()) {
            sb.append( item.toString() + "\n" );
        }
        for ( String line : defaults ) {
            sb.append( line + "\n" );
        }

        sb.append( "proto" + " " + settings.getProtocol() + "\n" );
        sb.append( "port" + " " + settings.getPort() + "\n" );
        sb.append( "cipher" + " " + settings.getCipher() + "\n" );

        String name = client.getName();
        String siteName = settings.getSiteName();

        sb.append( "cert" + " " + KEY_DIR + "/" + siteName + "-" + name + ".crt" + "\n");
        sb.append( "key"  + " " + KEY_DIR + "/" + siteName + "-" + name + ".key" + "\n");
        sb.append( "ca"   + " " + KEY_DIR + "/" + siteName + "-" + name + "-ca.crt" + "\n");

        String publicAddress = UvmContextFactory.context().networkManager().getPublicUrl();

        /* Strip off the port, (This guarantees if they set it to a hostname the value will be correct) */
        publicAddress = publicAddress.split( ":" )[0];
        publicAddress = publicAddress.trim();

        sb.append( "remote" + " " + publicAddress + " " + settings.getPort() + " # public address \n");

        /**
         * Also write the static IP of any static WANs
         * This will be used as a backup if publicAddress fails or is wrong.
         * This will help for multi-WAN failover
         * Bug #10828
         */
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        for ( InterfaceSettings interfaceSettings : networkSettings.getInterfaces() ) {
            if ( interfaceSettings.getIsWan() && interfaceSettings.getV4ConfigType() == InterfaceSettings.V4ConfigType.STATIC )
                sb.append( "remote" + " " + interfaceSettings.getV4StaticAddress().getHostAddress() + " " + settings.getPort() + " # static WAN " + interfaceSettings.getInterfaceId() + "\n");
        }
        
        File dir = new File( CLIENT_CONF_FILE_DIR );
        if ( ! dir.exists() )
            dir.mkdirs();

        writeFile( CLIENT_CONF_FILE_BASE + name + "." + extension, sb );
    }

    private void writeRemoteClientFiles( OpenVpnSettings settings )
    {
        for ( OpenVpnRemoteClient client : settings.getRemoteClients() ) {
            OpenVpnGroup group = getGroup( settings, client.getGroupId() );

            if ( !client.getEnabled() || ( group == null ) ) 
                continue;

            String name = client.getName();
            logger.info( "Writing client configuration file for [" + name + "]" );

            StringBuilder sb = new StringBuilder();

            if( group.getFullTunnel() ) {
                sb.append( "push" + " " + "\"redirect-gateway def1\"" + "\n");
            }

            /**
             * If PushDNS is enabled, we need to push the DNS settings
             */
            if( group.getPushDns() ) {

                /**
                 * If push DNS Self is set, push openvpn's addr
                 * Otherwise, push custom
                 */
                if ( group.getPushDnsSelf() ) {
                    sb.append( "push" + " " + "\"dhcp-option DNS " + settings.getAddressSpace().getFirstMaskedAddress().getHostAddress() + "\"" + "\n");
                } else {
                    InetAddress dns1 = group.getPushDns1();
                    if ( dns1 != null )
                        sb.append( "push" + " " + "\"dhcp-option DNS " + dns1.getHostAddress() + "\"" + "\n");
                    InetAddress dns2 = group.getPushDns2();
                    if ( dns2 != null )
                        sb.append( "push" + " " + "\"dhcp-option DNS " + dns2.getHostAddress() + "\"" + "\n");
                }
                String dnsDomain = group.getPushDnsDomain();
                if ( dnsDomain != null && !"".equals(dnsDomain.trim()) ) 
                    sb.append( "push" + " " + "\"dhcp-option DOMAIN " + dnsDomain + "\"" + "\n");
            }

            if ( client.getExport() && client.getExportNetwork() != null ) {
                for ( String net : client.getExportNetwork().split(",") ) {
                    try {
                        IPMaskedAddress maskedAddr = new IPMaskedAddress( net );
                        writeRemoteClientRoute( sb, maskedAddr.getMaskedAddress(), maskedAddr.getNetmask());
                    } catch (Exception e) {
                        logger.warn( "Error processing network: " + net, e );
                    }
                }
            }
            
            writeFile( OPENVPN_CCD_DIR + "/" + name, sb );
        }
    }

    private void deleteFiles( )
    {
        /**
         * Delete the old server files 
         * This is so that when we disable clients/servers the files will be removed.
         * Any enabled clients/servers will have their conf files re-written after this
         */
        try {
            File baseDirectory = new File( "/etc/openvpn" );
            if ( baseDirectory.exists()) {
                for ( File f : baseDirectory.listFiles()) {
                    if ( f.getName() == null || !f.getName().endsWith(".conf") )
                        continue;
                    logger.debug("Deleting remoteServer conf file: " + f.getName());
                    f.delete();
                }
            } else {
                baseDirectory.mkdir();
            }
        } catch ( Exception e ) {
            logger.error( "Unable to delete the previous server configuration files." );
        }

        /**
         * Delete the old client files
         * This is so that when we disable clients, their CCD files will be gone
         */
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
    }
    
    private void writeRemoteServerFiles( OpenVpnSettings settings )
    {
        
        /**
         * Copy the config file for all enabled remote servers
         */
        for ( OpenVpnRemoteServer server : settings.getRemoteServers() ) {
            if ( !server.getEnabled() )
                continue;

            String name = server.getName();
            logger.info( "Writing server configuration file for [" + name + "]" );

            String cpCmd = "cp -f " + System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/remote-servers/" + name + ".conf /etc/openvpn/";
            UvmContextFactory.context().execManager().exec( cpCmd );
        }

        /**
         * Copy all keys in place
         */
        UvmContextFactory.context().execManager().exec( "cp -rf " + System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/remote-servers/keys /etc/openvpn/" );
        /**
         * "untangle-vpn" was the key directory name in 9.4 and prior
         * keep this to maintain backwards compatibility with 9.4 and prior
         */
        UvmContextFactory.context().execManager().exec( "cp -rf " + System.getProperty("uvm.settings.dir") + "/untangle-node-openvpn/remote-servers/untangle-vpn /etc/openvpn/" );
    }
    
    private void writePushRoute( StringBuilder sb, InetAddress address, InetAddress netmask )
    {
        if ( address == null ) {
            logger.warn( "attempt to write a push route with null address" );
            return;
        }

        String value = "\"route ";
        if ( netmask != null ) {
            value += address.getHostAddress();
            value += " " + netmask.getHostAddress();
        } else {
            value += address.getHostAddress();
        }

        value += "\"";

        sb.append( "push" + " " + value + "\n" );
    }

    private void writeRemoteClientRoute( StringBuilder sb, InetAddress address, InetAddress netmask )
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
            value += " " + netmask.getHostAddress();
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
        logger.info( "Writing File: " + fileName );
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
            if ( group.getGroupId() == groupId )
                return group;
        }

        return null;
    }

    private void writeIptablesFiles( OpenVpnSettings settings )
    {
        int maxNumTunDevices = 1;
        for (OpenVpnRemoteServer server : settings.getRemoteServers()) {
            if ( server.getEnabled() )
                maxNumTunDevices++;
        }
        
        try {
            logger.info( "Writing File: " + IPTABLES_SCRIPT );

            int httpsPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpsPort();
            int httpPort = UvmContextFactory.context().networkManager().getNetworkSettings().getHttpPort();

            FileWriter iptablesScript = new FileWriter( IPTABLES_SCRIPT, false );

            iptablesScript.write("#!/bin/dash" + "\n");
            iptablesScript.write("## Auto Generated on " + new Date() + "\n");
            iptablesScript.write("## DO NOT EDIT. Changes will be overwritten." + "\n");
            iptablesScript.write("\n\n");

            iptablesScript.write("if [ -z \"$IPTABLES\" ] ; then IPTABLES=iptables ; fi" + "\n");
            iptablesScript.write("\n");

            iptablesScript.write("ADDR=\"`ip addr show tun0 2>/dev/null| awk '/^ *inet.*scope global/ { interface = $2 ; sub( \"/.*\", \"\", interface ) ; print interface ; exit }'`\"" + "\n");
            iptablesScript.write("\n");
            
            iptablesScript.write("# delete old mark rules (if they exist) (tun0-tun10) " + "\n");
            iptablesScript.write("for i in `seq 0 " + (maxNumTunDevices + 10 ) + "` ; do" + "\n");
            iptablesScript.write("    ${IPTABLES} -t mangle -D mark-src-intf -i tun$i -j MARK --set-mark 0xfa/0xff -m comment --comment \"Set src interface mark for openvpn\" >/dev/null 2>&1" + "\n");
            iptablesScript.write("    ${IPTABLES} -t mangle -D mark-dst-intf -o tun$i -j MARK --set-mark 0xfa00/0xff00 -m comment --comment \"Set dst interface mark for openvpn\" >/dev/null 2>&1" + "\n");
            iptablesScript.write("done" + "\n");
            iptablesScript.write("\n");

            iptablesScript.write("# delete old global NAT rule" + "\n");
            iptablesScript.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0xfa/0xff -j MASQUERADE -m comment --comment \"NAT openvpn traffic to the server\" >/dev/null 2>&1" + "\n");

            for ( InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces() ) {
                if ( intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan() ) {
                    iptablesScript.write("# delete old WAN NAT rule" + "\n");
                    iptablesScript.write("${IPTABLES} -t nat -D nat-rules -m mark --mark 0x" + Integer.toHexString( (intfSettings.getInterfaceId() << 8) + 0x00fa ) + "/0xffff " +
                                         "-j MASQUERADE -m comment --comment \"NAT WAN-bound openvpn traffic\" >/dev/null 2>&1" + "\n");
                }
            }

            iptablesScript.write("# delete old Handle admin from tun0 (openvpn server)" + "\n");
            iptablesScript.write("if [ ! -z \"$ADDR\" ] ; then " + "\n");
            iptablesScript.write("\t${IPTABLES} -t nat -D port-forward-rules -p tcp -d $ADDR --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send to apache\" >/dev/null 2>&1 \n");
            iptablesScript.write("\t${IPTABLES} -t nat -D port-forward-rules -p tcp -d $ADDR --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send to apache\" >/dev/null 2>&1 \n");
            iptablesScript.write("fi" + "\n");
            iptablesScript.write("\n");

            iptablesScript.write("# delete old nat-reverse-filter rule" + "\n");
            iptablesScript.write("${IPTABLES} -t filter -D nat-reverse-filter -m mark --mark 0xfa/0xff -j RETURN -m comment --comment \"Allow OpenVPN\" >/dev/null 2>&1 \n");
            iptablesScript.write("\n");
            
            iptablesScript.write("# mark traffic to/from openvpn interface" + "\n");
            iptablesScript.write("for i in `seq 0 " + (maxNumTunDevices-1) + "` ; do" + "\n");
            iptablesScript.write("    ${IPTABLES} -t mangle -I mark-src-intf 3 -i tun$i -j MARK --set-mark 0xfa/0xff -m comment --comment \"Set src interface mark for openvpn\"" + "\n");
            iptablesScript.write("    ${IPTABLES} -t mangle -I mark-dst-intf 3 -o tun$i -j MARK --set-mark 0xfa00/0xff00 -m comment --comment \"Set dst interface mark for openvpn\"" + "\n");
            iptablesScript.write("done" + "\n");
            iptablesScript.write("\n");

            iptablesScript.write("# Handle admin from tun0 (openvpn server)" + "\n");
            iptablesScript.write("if [ ! -z \"$ADDR\" ] ; then " + "\n");
            iptablesScript.write("\t${IPTABLES} -t nat -I port-forward-rules -p tcp -d $ADDR --destination-port " + httpsPort + " -j REDIRECT --to-ports 443 -m comment --comment \"Send to apache\" \n");
            iptablesScript.write("\t${IPTABLES} -t nat -I port-forward-rules -p tcp -d $ADDR --destination-port " + httpPort + " -j REDIRECT --to-ports 80 -m comment --comment \"Send to apache\" \n");
            iptablesScript.write("fi" + "\n");
            iptablesScript.write("\n");

            iptablesScript.write("# insert nat-reverse-filter rule to allow openvpn to penetrate NATd networks " + "\n");
            iptablesScript.write("${IPTABLES} -t filter -I nat-reverse-filter -m mark --mark 0xfa/0xff -j RETURN -m comment --comment \"Allow OpenVPN\" \n");
            iptablesScript.write("\n");
            
            if ( settings.getServerEnabled() && settings.getNatOpenVpnTraffic() ) {
                iptablesScript.write("# NAT traffic from the server openvpn interface" + "\n");
                iptablesScript.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0xfa/0xff -j MASQUERADE -m comment --comment \"NAT openvpn traffic to the server\"" + "\n");
            } else {
                for ( InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces() ) {
                    if ( intfSettings.getConfigType() == InterfaceSettings.ConfigType.ADDRESSED && intfSettings.getIsWan() ) {
                        iptablesScript.write("# Always NAT wan bound traffic" + "\n");
                        iptablesScript.write("${IPTABLES} -t nat -I nat-rules -m mark --mark 0x" + Integer.toHexString( (intfSettings.getInterfaceId() << 8) + 0x00fa ) + "/0xffff " +
                                             "-j MASQUERADE -m comment --comment \"NAT WAN-bound openvpn traffic\"" + "\n");
                    }
                }
            }

            
            iptablesScript.close();

            UvmContextFactory.context().execManager().execResult( "chmod 755 " + IPTABLES_SCRIPT );

            return;

        } catch ( java.io.IOException exc ) {
            logger.error( "Error writing iptables script", exc );
        }
    }

    /**
     * Inserts necessary iptables rules if OpenVPN daemon is running
     * Removes same rules if OpenVPN daemon is not running
     * safe to run multiple times
     */
    private synchronized void insertIptablesRules()
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( IPTABLES_SCRIPT );
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( IPTABLES_SCRIPT + ": ");
            for ( String line : lines )
                logger.info( IPTABLES_SCRIPT + ": " + line);
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to start OpenVPN daemon (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to start OpenVPN daemon");
        }
    }
}
