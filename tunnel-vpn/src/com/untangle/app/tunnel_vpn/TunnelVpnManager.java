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
    private static final String IPTABLES_SCRIPT = System.getProperty( "prefix" ) + "/etc/untangle-netd/iptables-rules.d/730-tunnelvpn";

    protected TunnelVpnManager() { }

    protected void restart()
    {
        logger.warn("FIXME");

        insertIptablesRules();
    }

    protected void stop()
    {
        logger.warn("FIXME");

        removeIptablesRules();
    }

    protected void configure( TunnelVpnSettings settings )
    {
        writeIptablesFiles( settings );
        for ( TunnelVpnTunnelSettings tunnelSettings : settings.getTunnels() )
            writeTunnelConfig( tunnelSettings );
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

    private void writeIptablesFiles( TunnelVpnSettings settings )
    {
        logger.warn("FIXME");
    }

    private void writeTunnelConfig( TunnelVpnTunnelSettings settings )
    {
        logger.warn("FIXME");
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

    /**
     * Removes iptables rules
     */
    private synchronized void removeIptablesRules()
    {
        logger.warn("FIXME");
    }
}
