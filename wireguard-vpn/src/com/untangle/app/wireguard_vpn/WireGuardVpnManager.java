/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Base64;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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
    private static final String WIREGUARD_POST_NETWORK_SCRIPT = "/etc/untangle/post-network-hook.d/300-wireguard";
    private static final String WIREGUARD_QUICK_CONFIG = "/etc/wireguard/wg0.conf";

    private static final String WIREGUARD_REMOTE_CONFIG_TEMPLATE_PUBLIC_KEY = "%PUBLIC_KEY%";
    private static final String WIREGUARD_REMOTE_CONFIG_TEMPLATE = "/etc/wireguard/untangle/remote-" + WIREGUARD_REMOTE_CONFIG_TEMPLATE_PUBLIC_KEY + ".conf";
    private static final String WG_KEY_REGEX = "^[A-Za-z0-9+/]{42}[AEIMQUYcgkosw480]=$";
    private static final Pattern PATTERN = Pattern.compile(WG_KEY_REGEX);

    private final Logger logger = LogManager.getLogger(this.getClass());
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
        configure();
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(WIREGUARD_POST_NETWORK_SCRIPT);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(WIREGUARD_QUICK_APP + ": ");
            for (String line : lines){
                logger.info(WIREGUARD_QUICK_APP + ": " + line);
            }
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
        removeConf();
        configureIptables();
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(WIREGUARD_POST_NETWORK_SCRIPT);
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(WIREGUARD_QUICK_APP + ": ");
            for (String line : lines)
                logger.info(WIREGUARD_QUICK_APP + ": " + line);
        } catch (Exception e) {
        }
        if (result.getResult() != 0) logger.error("Failed calling WireGuard start script (return code: " + result.getResult() + ")");
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
        UvmContextFactory.context().syncSettings().run(
            app.getSettingsFilename(),
            UvmContextFactory.context().networkManager().getNetworkSettingsFilename(),
            Map.entry("wireguardUrl", UvmContextFactory.context().networkManager().getPublicUrl()),
            Map.entry("wireguardHostname", UvmContextFactory.context().networkManager().getNetworkSettings().getHostName())
        );
    }

    /**
     * Remove the configuration file so network restart works
     */
    protected void removeConf() {
        logger.info("Removing wireguard config files");
        File confFile = new File(WIREGUARD_QUICK_CONFIG);
        if (confFile.isFile()) {
            confFile.delete();
        }
    }

    /**
     * Add a tunnel
     * @param publicKey String of key to lookup in tunnel settings.
     */
    public void addTunnel(String publicKey)
    {
        WireGuardVpnTunnel addTunnel = null;

        if (!isValidWGKey(publicKey)) {
            logger.info("Invalid wireguard public key");
            return;

        }
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

        ArrayList<String> commands = new ArrayList<String>();
        ArrayList<String> allowedIps = new ArrayList<String>();
        // Make sure we have our peer address in allowed list.
        allowedIps.add(addTunnel.getPeerAddress().getHostAddress() + "/32");

        String[] networks = addTunnel.getNetworks().split("\\n");
        for (int x = 0; x < networks.length; x++) {
            if (networks[x].trim().length() == 0) continue;
            allowedIps.add(networks[x].trim());
        }
        // Add tunnel to WireGuard
        commands.add(
            WIREGUARD_APP +
            " set wg0 peer " + publicKey +
            ( addTunnel.getEndpointDynamic() != true
                ? " endpoint " + addTunnel.getEndpointHostname() + ":" + addTunnel.getEndpointPort()
                : ""
            ) +
            " persistent-keepalive " + app.getSettings().getKeepaliveInterval() +
            " allowed-ips " + String.join(",", allowedIps)
        );
        for(String allowedIp :allowedIps){
            commands.add("ip -4 route add " + allowedIp + " dev wg0");
        }
        for ( String cmd: commands ) 
            this.wireguardCommand(cmd);
    }

    /**
     * Remove a tunnel
     * @param publicKey String of key to lookup in tunnel settings.
     */
    public void deleteTunnel(String publicKey)
    {
        if (!isValidWGKey(publicKey)) {
            logger.info("Invalid wireguard public key");
            return;

        }
        WireGuardVpnTunnel removeTunnel = null;
        String[] networks = null;
        for(WireGuardVpnTunnel tunnel : app.getSettings().getTunnels()){
            if(tunnel.getPublicKey().equals(publicKey)){
                removeTunnel = tunnel;
                networks = removeTunnel.getNetworks().split("\\n");
            }
        }
        ArrayList<String> commands = new ArrayList<String>();
        if(removeTunnel != null){
            for (int index = 0; index < networks.length; index++) {
                if (networks[index].trim().length() == 0) continue;
                commands.add("ip -4 route delete " + networks[index].trim() + " dev wg0");
            }
            commands.add("ip -4 route delete " + removeTunnel.getPeerAddress().getHostAddress() + "/32" + " dev wg0");
        }
        commands.add(WIREGUARD_APP + " set wg0 peer " + publicKey + " remove");
        for ( String cmd: commands ) 
            this.wireguardCommand(cmd);
    }

    /**
     * Validate a public key
     * @param publicKey String key to lookup in tunnel settings.
     * @return True if Public key is valid or not.
     */
    public boolean isValidWGKey(String publicKey) {
        if (publicKey == null || publicKey.length() != 44) {
            return false;
        }

        // 1. Regex check: Immediately rejects command injection characters
        if (!PATTERN.matcher(publicKey).matches()) {
            return false;
        }

        try {
            // 2. Base64 decode check
            byte[] decoded = Base64.getDecoder().decode(publicKey);
            
            // 3. Length check: WireGuard keys are exactly 32 raw bytes
            return decoded.length == 32;
        } catch (IllegalArgumentException e) {
            return false;
        }
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
        if (!isValidWGKey(privateKey)) {
            logger.info("Invalid wireguard private key");
            return "";

        } 
        String result = wireguardCommand(System.getProperty("uvm.bin.dir") + "/ut-wireguard-helpers.sh showPublicKey " + privateKey + " " + WIREGUARD_APP );
        return result;
    }

    /**
     * Return QR code image
     * @param publicKey Public key identifier for configuration.
     * @return Base 64 encoded string of image.
     */
    public String createQrCode(String publicKey){
        if (!isValidWGKey(publicKey)) {
            logger.info("Invalid wireguard public key");
            return "";

        }
        File file = getRemoteConfigFile(publicKey);
        return (file != null) ? wireguardCommand("/usr/bin/qrencode -t SVG -o - -r " + file.getAbsolutePath())  : "";
    }

    /**
     * Get remote configuration file.
     * @param publicKey Public key identifier for configuration.
     * @return Text of config.
     */
    public String getConfig(String publicKey){
        File file = getRemoteConfigFile(publicKey);
        return (file != null) ? wireguardCommand("/bin/cat " + file.getAbsolutePath()) : "";
    }

    /**
     * Build remote configuration filename and determine if it exists.
     * @param publicKey publicKey Public Key to lookup.
     * @return File of valid file.  If not found, null.
     */
    private File getRemoteConfigFile(String publicKey)
    {
        if (!isValidWGKey(publicKey)) {
            logger.info("Invalid wireguard public key");
            return null;

        }
        String filename = WIREGUARD_REMOTE_CONFIG_TEMPLATE.replaceAll(WIREGUARD_REMOTE_CONFIG_TEMPLATE_PUBLIC_KEY, publicKey);
        File file = new File(filename);
        return file.exists() ? file : null;
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
            logger.info("wireguardCommand exception:", e);
        }
        return wireguardResult.trim();
    }

}
