/**
 * $Id$
 */
package com.untangle.app.tunnel_vpn;

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
import java.util.Random;
import java.io.FilenameFilter;
import java.io.File;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * This class has all the logic for "managing" the tunnel configs.
 * This includes writing all config files and starting/stopping the processes
 */
public class TunnelVpnManager
{
    private final Logger logger = Logger.getLogger( this.getClass());

    private static final String TUNNEL_START_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/tunnel-start";
    private static final String TUNNEL_STOP_SCRIPT  = System.getProperty( "uvm.bin.dir" ) + "/tunnel-stop";
    private static final String IPTABLES_SCRIPT = System.getProperty( "prefix" ) + "/etc/untangle-netd/iptables-rules.d/350-tunnel-vpn";
    private static final String IMPORT_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-import";
    private static final String VALIDATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-validate";
    private static final String LAUNCH_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-launch";

    private final TunnelVpnApp app;
    private int newTunnelId = -1;
    
    protected TunnelVpnManager(TunnelVpnApp app)
    {
        this.app = app;
    }

    protected synchronized void restartProcesses()
    {
        killProcesses();
        launchProcesses();
    }

    protected synchronized void killProcesses()
    {
        logger.info("Killing OpenVPN processes...");
        try {
            File dir = new File("/run/openvpn/");
            File[] matchingFiles = dir.listFiles(new FilenameFilter() {
                    public boolean accept(File dir, String name) {
                        return name.startsWith("tunnel-") && name.endsWith("pid");
                    }
                });
            if( matchingFiles != null ){
                for(File f: matchingFiles) {
                    String pid = new String(Files.readAllBytes(f.toPath())).replaceAll("(\r|\n)","");
                    logger.info("Killing OpenVPN process: " + pid);
                    UvmContextFactory.context().execManager().execOutput("kill -INT " + pid);
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to kill processes",e);
        }
    }
    
    protected synchronized void launchProcesses()
    {
        insertIptablesRules();        

        for( TunnelVpnTunnelSettings tunnelSettings : app.getSettings().getTunnels() ) {
            if ( !tunnelSettings.getEnabled() ) {
                logger.info("Tunnel " + tunnelSettings.getTunnelId() + " not enabled. Skipping...");
                continue;
            }
            int tunnelId = tunnelSettings.getTunnelId();
            String directory = System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/tunnel-" + tunnelId;
            ExecManagerResult result = UvmContextFactory.context().execManager().exec( LAUNCH_SCRIPT + " " + tunnelSettings.getTunnelId() + " " + directory);
        }
    }

    protected void writeIptablesFiles( TunnelVpnSettings settings )
    {
        try {
            logger.info( "Writing File: " + IPTABLES_SCRIPT );

            FileWriter iptablesScript = new FileWriter( IPTABLES_SCRIPT, false );

            iptablesScript.write("#!/bin/dash" + "\n");
            iptablesScript.write("## Auto Generated on " + new Date() + "\n");
            iptablesScript.write("## DO NOT EDIT. Changes will be overwritten." + "\n");
            iptablesScript.write("\n\n");
            iptablesScript.close();

            UvmContextFactory.context().execManager().execResult( "chmod 755 " + IPTABLES_SCRIPT );

            return;

        } catch ( java.io.IOException exc ) {
            logger.error( "Error writing iptables script", exc );
        }
    }
    
    protected synchronized void importTunnelConfig( String filename, String provider )
    {
        if (filename==null || provider==null) {
            logger.warn("Invalid arguments");
            throw new RuntimeException("Invalid Arguments");
        }
        
        TunnelVpnSettings settings = app.getSettings();
        int tunnelId = findLowestAvailableTunnelId( settings );
        String tunnelName = "tunnel-" + provider + "-" + tunnelId;
        
        if (tunnelId < 1) {
            logger.warn("Failed to find available tunnel ID");
            throw new RuntimeException("Failed to find available tunnel ID");
        }
        
        ExecManagerResult result = UvmContextFactory.context().execManager().exec( IMPORT_SCRIPT + " \""  + filename + "\" \"" + provider + "\" " + tunnelId);

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( IMPORT_SCRIPT + ": ");
            for ( String line : lines ) {
                logger.info( IMPORT_SCRIPT + ": " + line);
            }
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to import client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to import client config");
        }

        List<TunnelVpnTunnelSettings> tunnels = settings.getTunnels();
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        /**
         * Sanity checks
         */
        for ( TunnelVpnTunnelSettings tunnelSettings : tunnels ) {
            if (tunnelId == tunnelSettings.getTunnelId()) {
                logger.error("Tunnel ID conflict: " + tunnelId);
                throw new RuntimeException("Tunnel ID conflict: " + tunnelId);
            }
        }
        for ( InterfaceSettings virtualInterfaceSettings : virtualInterfaces ) {
            if (tunnelId == virtualInterfaceSettings.getInterfaceId()) {
                logger.error("Tunnel ID conflict: " + tunnelId);
                throw new RuntimeException("Tunnel ID conflict: " + tunnelId);
            }
        }

        /**
         * Set TunnelVPN settings
         */
        TunnelVpnTunnelSettings tunnelSettings = new TunnelVpnTunnelSettings();
        tunnelSettings.setName( tunnelName );
        tunnelSettings.setEnabled( false ); //newly imported tunnels are not enabled on import
        tunnelSettings.setAllTraffic( false );
        tunnelSettings.setTags( new LinkedList<String>() );
        tunnelSettings.setTunnelId( tunnelId );
        tunnels.add( tunnelSettings );
        settings.setTunnels( tunnels );
        app.setSettings( settings );

        return;
    }

    protected synchronized void validateTunnelConfig( String filename, String provider )
    {
        if (filename==null || provider==null) {
            logger.warn("Invalid arguments");
            throw new RuntimeException("Invalid Arguments");
        }

        TunnelVpnSettings settings = app.getSettings();
        int tunnelId = findLowestAvailableTunnelId( settings );
        String tunnelName = "tunnel-" + provider + "-" + tunnelId;

        if (tunnelId < 1) {
            logger.warn("Failed to find available tunnel ID");
            throw new RuntimeException("Failed to find available tunnel ID");
        }

        ExecManagerResult result = UvmContextFactory.context().execManager().exec( VALIDATE_SCRIPT + " \""  + filename + "\" \"" + provider + "\" " + tunnelId);

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info( VALIDATE_SCRIPT + ": ");
            for ( String line : lines ) {
                logger.info( VALIDATE_SCRIPT + ": " + line);
            }
        } catch (Exception e) {}

        if ( result.getResult() != 0 ) {
            logger.error("Failed to validate client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to validate client config: " + result.getOutput().trim());
        }

        return;
    }

    protected int getNewTunnelId()
    {
        return this.newTunnelId;
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

    /**
     * Inserts iptables rules
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

    private int findLowestAvailableTunnelId( TunnelVpnSettings settings )
    {
        if ( settings.getTunnels() == null )
            return 1;

        for (int i=200; i<240; i++) {
            boolean found = false;
            for (TunnelVpnTunnelSettings tunnelSettings: settings.getTunnels()) {
                if ( tunnelSettings.getTunnelId() != null && i == tunnelSettings.getTunnelId() ) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                newTunnelId = i;
                return i;
            }
        }

        logger.error("Failed to find available tunnel ID");
        return -1;
    }
}
