/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * This class has all the logic for "managing" the WireGuardVpn daemon. This includes
 * writing all the server and client config files and starting/stopping the
 * daemon
 * 
 * @author mahotz
 * 
 */
public class WireGuardVpnManager
{
    private static final String IPTABLES_SCRIPT = "/etc/untangle/iptables-rules.d/720-wireguard-vpn";
    private static final String WIREGUARD_APP = "/usr/bin/wg";
    private static final String WIREGUARD_QUICK_APP = "/usr/bin/wg-quick";
    private static final String WIREGUARD_QUICK_CONFIG = "/etc/wireguard/wg0.conf";

    private final Logger logger = Logger.getLogger(this.getClass());
    private final WireGuardVpnApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The WireGuard Vpn application
     */
    protected WireGuardVpnManager(WireGuardVpnApp app)
    {
        this.app = app;
    }

    /**
     * Start all WireGuard VPN instances
     */
    protected void start()
    {
        logger.info("Starting WireGuard interface and tunnels");
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(WIREGUARD_QUICK_APP + " up " + WIREGUARD_QUICK_CONFIG);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(WIREGUARD_QUICK_APP + ": ");
            for (String line : lines)
                logger.info(WIREGUARD_QUICK_APP + ": " + line);
        } catch (Exception e) {
        }
        if (result.getResult() != 0) logger.error("Failed calling WireGuard start script (return code: " + result.getResult() + ")");

        configureIptables();
    }

    /**
     * Stop all WireGuard VPN instances
     */
    protected void stop()
    {
        logger.info("Stopping WireGuard interface and tunnels");
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(WIREGUARD_QUICK_APP + " down " + WIREGUARD_QUICK_CONFIG);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(WIREGUARD_QUICK_APP + ": ");
            for (String line : lines)
                logger.info(WIREGUARD_QUICK_APP + ": " + line);
        } catch (Exception e) {
        }
        if (result.getResult() != 0) logger.error("Failed calling WireGuard start script (return code: " + result.getResult() + ")");

        configureIptables();
    }

    /**
     * Restart all WireGuard VPN instances
     */
    protected void restart()
    {
        stop();
        start();
    }

    /**
     * Create the WireGuard VPN file from the application settings
     */
    protected void configure()
    {
        /**
         * Update WireGuard quick config and iptables script.
         */
        UvmContextFactory.context().syncSettings().run(app.getSettingsFilename(), UvmContextFactory.context().networkManager().getNetworkSettingsFilename());
    }

    /**
     * Add a tunnel
     * @param publicKey String of key to lookup in tunnel settings.
     */
    public void addTunnel(String publicKey)
    {
        WireGuardVpnTunnel addTunnel = null;
        for(WireGuardVpnTunnel tunnel : app.getSettings().getTunnels()){
            if(tunnel.getPublicKey().equals(publicKey)){
                addTunnel = tunnel;
            }
        }
        if(addTunnel == null){
            logger.warn("addTunnel: publicKey=" + publicKey+ " not found");
            return;
        }
        if(addTunnel.getEnabled() == false){
            logger.warn("addTunnel: publicKey=" + publicKey+ " not enabled");
            return;
        }
        ArrayList<String> allowedIps = new ArrayList<String>();
        allowedIps.add(addTunnel.getPeerAddress().getHostAddress() + "/32");
        String[] networks = addTunnel.getNetworks().split("\\n");
        for (int x = 0;x < networks.length;x++) {
            if (networks[x].trim().length() == 0) continue;
            allowedIps.add(networks[x].trim());
        }
        String command = WIREGUARD_APP +
            " set wg0 peer " + publicKey +
            ( addTunnel.getEndpointDynamic() != true
                ? " endpoint " + addTunnel.getEndpointAddress().getHostAddress() + ":" + addTunnel.getEndpointPort()
                : ""
            ) +
            " persistent-keepalive " + app.getSettings().getKeepaliveInterval() +
            " allowed-ips " + String.join(",", allowedIps);
        this.wireguardCommand(command);
    }

    /**
     * Remove a tunnel
     * @param publicKey String of key to lookup in tunnel settings.
     */
    public void deleteTunnel(String publicKey)
    {
        String command = WIREGUARD_APP + " set wg0 peer " + publicKey + " remove";
        this.wireguardCommand(command);
    }

    /**
     * Configure appropriate iptables rules.
     */
    private synchronized void configureIptables()
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(IPTABLES_SCRIPT);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(IPTABLES_SCRIPT + ": ");
            for (String line : lines)
                logger.info(IPTABLES_SCRIPT + ": " + line);
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to configure WireGuard VPN iptables rules (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to configure WireGuard VPN iptables rules");
        }
    }


    /**
     * Generate private key from wireguard app.
     * 
     * @return String of generated private key.
     */
    public String createPrivateKey()
    {
        return wireguardCommand(WIREGUARD_APP + " genkey");
    }

    /**
     * Derive public key from private key.
     *
     * @param privateKey private key to derive public key from
     * @return String of derived public key.
     */
    public String getPublicKey(String privateKey)
    {
        return wireguardCommand("echo " + privateKey + " | " + WIREGUARD_APP + " pubkey");
    }

    /**
     * Call wireguard and get output
     * @param command String of command to pass to wg
     * @return String of result of call to wg.
     */
    private String wireguardCommand(String command)
    {
        String wireguardResult = "";
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(command);
        try {
            wireguardResult = result.getOutput();
        } catch (Exception e) {
        }
        return wireguardResult;
    }

}
