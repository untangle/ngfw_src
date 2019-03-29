/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.util.HashMap;
import java.util.Iterator;
import java.io.FilenameFilter;
import java.io.File;
import java.nio.file.Files;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.network.InterfaceStatus;

/**
 * This class has all the logic for "managing" the tunnel configs. This includes
 * writing all config files and starting/stopping the processes
 */
public class TunnelVpnManager
{
    private final Logger logger = Logger.getLogger(this.getClass());

    private static final String IPTABLES_SCRIPT = System.getProperty("prefix") + "/etc/untangle/iptables-rules.d/350-tunnel-vpn";
    private static final String IMPORT_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-import";
    private static final String VALIDATE_SCRIPT = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-validate";

    private final TunnelVpnApp app;
    private int newTunnelId = -1;

    private HashMap<Integer, Process> processMap = new HashMap<>();

    /**
     * Constructor
     * 
     * @param app
     *        The tunnel vpn application
     */
    protected TunnelVpnManager(TunnelVpnApp app)
    {
        this.app = app;
    }

    /**
     * Launches the openvpn process for each configured tunnel
     */
    protected synchronized void launchProcesses()
    {
        logger.info("Launching OpenVPN processes...");

        insertIptablesRules();

        try {
            File dir = new File("/run/tunnelvpn/");
            dir.mkdir();
        } catch (Exception e) {
            logger.warn("Unable to create PID directory", e);
        }

        for (TunnelVpnTunnelSettings tunnelSettings : app.getSettings().getTunnels()) {
            launchProcess(tunnelSettings);
        }
    }

    /**
     * Kills the openvpn process for each active tunnel
     */
    protected synchronized void killProcesses()
    {
        logger.info("Killing OpenVPN processes...");

        try {
            // First call kill on any PIDs
            File dir = new File("/run/tunnelvpn/");
            File[] matchingFiles = dir.listFiles(new FilenameFilter()
            {
                /**
                 * accept method for FilenameFilter
                 * 
                 * @param dir
                 *        The directory where the file is located
                 * @param name
                 *        The name of the file
                 * @return True to accept, otherwise false
                 */
                public boolean accept(File dir, String name)
                {
                    return name.startsWith("tunnel-") && name.endsWith("pid");
                }
            });
            if (matchingFiles != null) {
                for (File f : matchingFiles) {
                    String pid = new String(Files.readAllBytes(f.toPath())).replaceAll("(\r|\n)", "");
                    logger.info("Killing OpenVPN process: " + pid);
                    UvmContextFactory.context().execManager().execOutput("kill -INT " + pid);
                    UvmContextFactory.context().execManager().execOutput("kill -TERM " + pid);
                    UvmContextFactory.context().execManager().execOutput("kill -KILL " + pid);
                    logger.info("Deleting: " + f);
                    f.delete();
                }
            }

            // Second call destroy on any process
            // This is necessary because the pid file may not have been written yet
            for(Iterator<Integer>it=processMap.keySet().iterator();it.hasNext();){
                Integer entry = it.next();
                Process p = processMap.get(entry);
                if (p.isAlive()) {
                    logger.warn("Killing OpenVPN process " + entry);
                    p.destroy();
                }
                if (p.isAlive()) {
                    logger.warn("OpenVPN process still alive");
                }
                if (p.isAlive()) {
                    logger.warn("Forcibly Killing OpenVPN process " + entry);
                    p.destroyForcibly();
                }
                if (p.isAlive()) {
                    logger.warn("OpenVPN process still alive");
                }
                it.remove();
            }

        } catch (Exception e) {
            logger.warn("Failed to kill processes", e);
        }
    }

    /**
     * Kills and restarts the process for each enabled tunnel
     */
    protected synchronized void restartProcesses()
    {
        killProcesses();
        launchProcesses();
    }

    /**
     * Starts an openvpn instance for a tunnel
     * 
     * @param tunnelSettings
     *        The tunnel to be started
     */
    protected synchronized void launchProcess(TunnelVpnTunnelSettings tunnelSettings)
    {
        if (!tunnelSettings.getEnabled()) {
            logger.info("Tunnel " + tunnelSettings.getTunnelId() + " not enabled. Skipping...");
            return;
        }
        int tunnelId = tunnelSettings.getTunnelId();
        Process proc = processMap.get(tunnelId);
        if (proc != null && proc.isAlive()) {
            proc.destroy();
            proc.destroyForcibly();
            try {
                proc.waitFor();
            } catch (InterruptedException e) {
                logger.warn("Interrupted",e);
            }
        }

        String directory = System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/tunnel-" + tunnelId;
        String tunnelName = "tunnel-" + tunnelId;
        Integer interfaceId = tunnelSettings.getBoundInterfaceId();
        boolean localBound = false;
        
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
        cmd += "--management 127.0.0.1 " + (TunnelVpnApp.BASE_MGMT_PORT + tunnelId) + " ";
        if (interfaceId != null && interfaceId != 0) {
            // if bound to a specific interface, specify that interface's main IP as the local address
            InterfaceStatus status = UvmContextFactory.context().networkManager().getInterfaceStatus(interfaceId);
            if (status != null && status.getV4Address() != null) {
                cmd += "--bind --local " + status.getV4Address().getHostAddress() + " --port 0 ";
                localBound = true;
            }
        }
        if (!localBound) {
            cmd += "--nobind ";
        }

        proc = UvmContextFactory.context().execManager().execEvilProcess(cmd);
        processMap.put(tunnelId, proc);
    }

    /**
     * Imports an uploaded tunnel configuration
     * 
     * @param filename
     *        The filename to import
     * @param provider
     *        The name of the tunnel provider
     * @param tunnelId
     *        The tunnel ID
     */
    protected synchronized void importTunnelConfig(String filename, String provider, int tunnelId)
    {
        if (filename == null || provider == null) {
            logger.warn("Invalid arguments");
            throw new RuntimeException("Invalid Arguments");
        }

        TunnelVpnSettings settings = app.getSettings();

        if (tunnelId < 1) {
            logger.warn("Failed to find available tunnel ID");
            throw new RuntimeException("Failed to find available tunnel ID");
        }

        ExecManagerResult result = UvmContextFactory.context().execManager().exec(IMPORT_SCRIPT + " \"" + filename + "\" \"" + provider + "\" " + tunnelId);

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(IMPORT_SCRIPT + ": ");
            for (String line : lines) {
                logger.info(IMPORT_SCRIPT + ": " + line);
            }
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to import client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to import client config");
        }

        return;
    }

    /**
     * Validates a tunnel configuration
     * 
     * @param filename
     *        The filename to validate
     * @param provider
     *        The tunnel provider
     */
    protected synchronized void validateTunnelConfig(String filename, String provider)
    {
        if (filename == null || provider == null) {
            logger.warn("Invalid arguments");
            throw new RuntimeException("Invalid Arguments");
        }

        TunnelVpnSettings settings = app.getSettings();
        int tunnelId = findLowestAvailableTunnelId(settings);

        if (tunnelId < 1) {
            logger.warn("Failed to find available tunnel ID");
            throw new RuntimeException("Failed to find available tunnel ID");
        }

        ExecManagerResult result = UvmContextFactory.context().execManager().exec(VALIDATE_SCRIPT + " \"" + filename + "\" \"" + provider + "\" " + tunnelId);

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(VALIDATE_SCRIPT + ": ");
            for (String line : lines) {
                logger.info(VALIDATE_SCRIPT + ": " + line);
            }
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to validate client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to validate client config: " + result.getOutput().trim());
        }

        return;
    }

    /**
     * Gets a nwe tunnel ID value
     * 
     * @return The tunnel ID value
     */
    protected int getNewTunnelId()
    {
        return this.newTunnelId;
    }

    /**
     * Stops and restarts a tunnel
     * 
     * @param tunnelId
     *        The tunnel to restart
     */
    public void recycleTunnel(int tunnelId)
    {
        for (TunnelVpnTunnelSettings tunnelSettings : app.getSettings().getTunnels()) {
            if (tunnelSettings.getTunnelId() == null) continue;
            if (tunnelSettings.getTunnelId() != tunnelId) continue;
            if (!tunnelSettings.getEnabled()) continue;

            try {
                File pidFile = new File("/run/tunnelvpn/tunnel-" + tunnelSettings.getTunnelId() + ".pid");
                String pidData = new String(Files.readAllBytes(pidFile.toPath())).replaceAll("(\r|\n)", "");
                logger.info("Recycling tunnel connection: " + tunnelSettings.getName() + "PID:" + pidData);
                logger.info("Deleting: " + pidFile);
                pidFile.delete();

                /*
                 * We get called when the user clicks recycle from the web
                 * interface so we send three signals to make sure the process
                 * goes away as quickly and cleanly as possible. The first will
                 * interrupt any system call in progress. The second lets the
                 * daemon know to terminate and hopefully begin a clean
                 * shutdown. The third tells it we do not want to wait.
                 */
                UvmContextFactory.context().execManager().execOutput("kill -INT " + pidData);
                UvmContextFactory.context().execManager().execOutput("kill -TERM " + pidData);
                UvmContextFactory.context().execManager().execOutput("kill -KILL " + pidData);

                processMap.remove(tunnelSettings.getTunnelId());

                launchProcess(tunnelSettings);
            } catch (Exception exn) {
                logger.warn("Exception attempting to recycle tunnel");
            }
        }
    }

    /**
     * Inserts iptables rules
     */
    private synchronized void insertIptablesRules()
    {
        File f = new File(IPTABLES_SCRIPT);
        if (!f.exists()) return;

        ExecManagerResult result = UvmContextFactory.context().execManager().exec(IPTABLES_SCRIPT);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(IPTABLES_SCRIPT + ": ");
            for (String line : lines)
                logger.info(IPTABLES_SCRIPT + ": " + line);
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to execute iptables script (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to execute iptables script");
        }
    }

    /**
     * Finds the lowest unused tunnel ID value
     * 
     * @param settings
     *        The application settings
     * @return The lowest unused tunnel ID value
     */
    private int findLowestAvailableTunnelId(TunnelVpnSettings settings)
    {
        if (settings.getTunnels() == null) return 1;

        for (int i = 200; i < 240; i++) {
            boolean found = false;
            for (TunnelVpnTunnelSettings tunnelSettings : settings.getTunnels()) {
                if (tunnelSettings.getTunnelId() != null && i == tunnelSettings.getTunnelId()) {
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
