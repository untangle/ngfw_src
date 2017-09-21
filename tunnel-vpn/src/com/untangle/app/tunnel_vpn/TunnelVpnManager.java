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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.AppSettings;
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

    private static final String IPTABLES_SCRIPT = System.getProperty( "prefix" ) + "/etc/untangle-netd/iptables-rules.d/350-tunnel-vpn";
    private static final String IMPORT_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-import";
    private static final String VALIDATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-validate";

    private final TunnelVpnApp app;
    private int newTunnelId = -1;

    private HashMap<Integer, Process> processMap = new HashMap<Integer, Process>();
    
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
            File dir = new File("/run/tunnelvpn/");
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
        logger.info("Launching OpenVPN processes...");

        insertIptablesRules();

        for( TunnelVpnTunnelSettings tunnelSettings : app.getSettings().getTunnels() ) {
            launchProcess( tunnelSettings );
        }
    }

    protected synchronized void launchProcess( TunnelVpnTunnelSettings tunnelSettings )
    {
        if ( !tunnelSettings.getEnabled() ) {
            logger.info("Tunnel " + tunnelSettings.getTunnelId() + " not enabled. Skipping...");
            return;
        }
        int tunnelId = tunnelSettings.getTunnelId();
        String directory = System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/tunnel-" + tunnelId;
        String tunnelName = "tunnel-" + tunnelId;
    
        String cmd = "/usr/sbin/openvpn ";
        cmd += "--config " + directory + "/tunnel.conf ";
        cmd += "--writepid /run/tunnelvpn/" + tunnelName + ".pid ";
        cmd += "--dev tun" + tunnelId + " ";
        cmd += "--cd " + directory + " ";
        cmd += "--log-append /var/log/uvm/tunnel.log ";
        cmd += "--auth-user-pass auth.txt ";
        cmd += "--script-security 2 ";
        cmd += "--up " + System.getProperty("prefix") + "/usr/share/untangle/bin/tunnel-vpn-up.sh ";
        cmd += "--down " + System.getProperty("prefix") + "/usr/share/untangle/bin/tunnel-vpn-down.sh ";
        cmd += "--management 127.0.0.1 " + (2000+tunnelId) + " ";

        Process proc = UvmContextFactory.context().execManager().execEvilProcess( cmd );
        processMap.put( tunnelId, proc );
    }
    
    protected synchronized void importTunnelConfig( String filename, String provider, int tunnelId )
    {
        if (filename==null || provider==null) {
            logger.warn("Invalid arguments");
            throw new RuntimeException("Invalid Arguments");
        }
        
        TunnelVpnSettings settings = app.getSettings();
        
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
    
    public List<JSONObject> getTunnelStates()
    {
        List<JSONObject> states = new LinkedList<JSONObject>();

        if ( app.getSettings() == null || app.getSettings().getTunnels() == null )
            return states;
        
        try {
            for( TunnelVpnTunnelSettings tunnelSettings : app.getSettings().getTunnels() ) {
                org.json.JSONObject json = new org.json.JSONObject();
                if (tunnelSettings.getTunnelId() == null)
                    continue;
                json.put("tunnelId",tunnelSettings.getTunnelId());
                json.put("name",tunnelSettings.getName());
                json.put("provider",tunnelSettings.getProvider());

                if(app.getRunState() != AppSettings.AppState.RUNNING) {
                    json.put("state","OFF");
                } else if (!tunnelSettings.getEnabled()) {
                    json.put("state","DISABLED");
                } else {
                    json.put("state",getTunnelState(tunnelSettings.getTunnelId()));
                }
                states.add( json );
            }
        } catch (Exception e) {
            logger.error("Error generating tunnel status", e);
        }

        return states;
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
        File f = new File( IPTABLES_SCRIPT );
        if (!f.exists())
            return;

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

    private String getTunnelState(int tunnelId)
    {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        
        try {
            try {
                /* Connect to the management port */
                socket = new Socket((String)null, 2000+tunnelId );
                socket.setSoTimeout( 2000 ); // 2 seconds

                in = new BufferedReader( new InputStreamReader( socket.getInputStream()));
                out = new BufferedWriter( new OutputStreamWriter( socket.getOutputStream()));

                /* Read out the hello message */
                in.readLine();

                out.write( "state" + "\n" );
                out.flush();

                String state = in.readLine();
                logger.info("Tunnel " + tunnelId + " state: " + state);

                if ( state == null )
                    return null;

                String[] splits = state.split(",");
                if (splits.length < 2)
                    return null;
                else
                    return splits[1];
            } finally {
                if ( out != null )    out.close();
                if ( in != null )     in.close();
                if ( socket != null ) socket.close();
            }
        } catch (java.net.ConnectException e) {
            return "DISCONNECTED";
        } catch (Exception e) {
            logger.warn("Failed to get tunnel status",e);
            return null;
        }
    }
}
