/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.File;
import java.net.InetAddress;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.NetspaceManager.IPVersion;
import com.untangle.uvm.NetspaceManager.NetworkSpace;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.util.I18nUtil;

/**
 * The WireGuard VPN application connects to 3rd party VPN tunnel providers.
 */
public class WireGuardVpnApp extends AppBase
{
    private final static String WIREGUARD_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/wireguard-status";

    private final Logger logger = Logger.getLogger(getClass());

    private final String SettingsDirectory = "/wireguard-vpn/";

    private static final String STAT_PASS = "pass";

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private WireGuardVpnSettings settings = null;
    private WireGuardVpnMonitor WireGuardVpnMonitor = null;
    private WireGuardVpnManager WireGuardVpnManager = null;
    private final WireGuardVpnEventHandler handler;

    private static final String NETSPACE_OWNER = "wireguard-vpn";
    private static final String NETSPACE_SERVER = "server-network";
    private static final String NETSPACE_TUNNEL = "server-tunnel";

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public WireGuardVpnApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
        WireGuardVpnMonitor = new WireGuardVpnMonitor(this);
        this.WireGuardVpnManager = new WireGuardVpnManager(this);

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));

        this.handler = new WireGuardVpnEventHandler(this);
        this.connector = UvmContextFactory.context().pipelineFoundry().create("wireguard-vpn", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 10, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public WireGuardVpnSettings getSettings()
    {
        return settings;
    }

    /**
     * Get the wireguard Vpn manager
     * 
     * @return wireguard manager
     */
    public WireGuardVpnManager getWireGuardVpnManager(){
        return this.WireGuardVpnManager;
    }

    /**
     * Return the settings filename
     * @return String of filename
     */
    public String getSettingsFilename()
    {
        return System.getProperty("uvm.settings.dir") + SettingsDirectory + "settings_"  + this.getAppSettings().getId().toString() + ".js";
    }

    /**
     * Set the application settings
     *
     * @param newSettings
     *      The new settings
     * @param restart
     *      If true, restart
     */
    public void setSettings(final WireGuardVpnSettings newSettings, boolean restart)
    {
        /**
         * First we check for network address space conflicts
         */
        String conflict = checkNetworkReservations(newSettings.getAddressPool(), newSettings.getTunnels());
        if (conflict != null) {
            throw new RuntimeException(conflict);
        }

        int idx = 0;
        for (WireGuardVpnTunnel tunnel : newSettings.getTunnels()) {
            tunnel.setId(++idx);
            if(tunnel.getPublicKey().equals("")){
                tunnel.setPrivateKey(this.WireGuardVpnManager.createPrivateKey());
                tunnel.setPublicKey(this.WireGuardVpnManager.getPublicKey(tunnel.getPrivateKey()));
            }
        }

        /**
         * Save the settings
         */
        try {
            UvmContextFactory.context().settingsManager().save( this.getSettingsFilename(), newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings and update network reservations
         * any time settings are saved.
         */
        this.settings = newSettings;
        updateNetworkReservations(newSettings.getAddressPool(), newSettings.getTunnels());

        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.WireGuardVpnManager.configure();

        // !!! only do this if we're running
        if(restart == true){
            this.WireGuardVpnManager.restart();
        }
    }

    /**
     * Set the application settings and restart.
     *
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final WireGuardVpnSettings newSettings)
    {
        setSettings(newSettings, true);
    }

    /**
     * Add a tunnel by publicKey
     * @param publicKey String of public key to add.
     */
    public void addTunnel(String publicKey)
    {
        this.WireGuardVpnManager.addTunnel(publicKey);
    }

    /**
     * Delete a tunnel by publicKey
     * @param publicKey String of public key to delete.
     */
    public void deleteTunnel(String publicKey)
    {
        this.WireGuardVpnManager.deleteTunnel(publicKey);
    }

    /**
     * Called to increment the pass count metric
     */
    public void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Get the list of pipeline connectors
     * 
     * @return List of pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called after the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        try {
            this.WireGuardVpnManager.configure();
            this.WireGuardVpnManager.start();
        } catch (Exception e) {
            logger.error("Error during startup", e);
            try {
                this.WireGuardVpnManager.stop();
            } catch (Exception stopException) {
                logger.error("Unable to stop the wireguard process", stopException);
            }
            throw new RuntimeException(e);
        }

        this.WireGuardVpnMonitor.start();
    }

    /**
     * Called before the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStop(boolean isPermanentTransition)
    {
        this.WireGuardVpnMonitor.stop();

        this.WireGuardVpnManager.stop();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        WireGuardVpnSettings readSettings = null;

        try {
            readSettings = settingsManager.load(WireGuardVpnSettings.class, this.getSettingsFilename());
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize. We only need to
         * register network reservations on successful load since it will
         * be handled in saveSettings during initialize. 
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.initializeSettings();
        } else {
            logger.info("Loading Settings...");
            this.settings = readSettings;
            updateNetworkReservations(readSettings.getAddressPool(), readSettings.getTunnels());
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    /**
     * Called to initialize application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        WireGuardVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private WireGuardVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        WireGuardVpnSettings settings = new WireGuardVpnSettings();

        String privateKey = this.getWireGuardVpnManager().createPrivateKey().trim();
        String publicKey = this.getWireGuardVpnManager().getPublicKey(privateKey).trim();
        settings.setPrivateKey(privateKey);
        settings.setPublicKey(publicKey);

        InetAddress dnsAddress = UvmContextFactory.context().networkManager().getFirstDnsResolverAddress();
        if(dnsAddress != null){
            logger.warn(dnsAddress.getHostAddress());
            settings.setDnsServer(dnsAddress.getHostAddress());
        }
        settings.setNetworks(UvmContextFactory.context().networkManager().getLocalNetworks().stream().map(Object::toString).collect(Collectors.joining("\r\n")));

        IPMaskedAddress newSpace = UvmContextFactory.context().netspaceManager().getAvailableAddressSpace(IPVersion.IPv4, 1, 24);

        settings.setAutoAddressAssignment(true);
        settings.setAddressPool(newSpace);

        settings.setTunnels(new LinkedList<WireGuardVpnTunnel>());

        return settings;
    }

    /**
     * Returns a list with the status of all wireguard tunnels
     *
     * @return A JSON string returned by the wireguard-status script
     */
    public String getTunnelStatus()
    {
        String result = UvmContextFactory.context().execManager().execOutput(WIREGUARD_STATUS_SCRIPT);
        return (result);
    }

    /**
     * Returns an address pool that is validated against the netspace manager to not be conflicting
     * 
     *
     * @return An unclaimed address space
     */
    public String getNewAddressPool()
    {
        IPMaskedAddress newSpace = UvmContextFactory.context().netspaceManager().getAvailableAddressSpace(IPVersion.IPv4, 1, 24);
        return newSpace.toString();
    }

    /**
     * Return QR code image
     * @param publicKey publickKey for tunnel.
     * @return Base 64 encoded string of image.
     */
    public String createRemoteQrCode(String publicKey){
        return this.getWireGuardVpnManager().createQrCode(publicKey);
    }

    /**
     * Return configuration
     * @param publicKey publickKey for tunnel.
     * @return String of configuration.
     */
    public String getRemoteConfig(String publicKey){
        return this.getWireGuardVpnManager().getConfig(publicKey);
    }

    /**
     * Function to register all network address blocks configured in this application
     *
     * @param serverPool - server pool address space to validate against
     * @param tunnelPools - A list of WireGuardVpnTunnels to validate address spaces against
     */
    private void updateNetworkReservations(IPMaskedAddress serverPool, List<WireGuardVpnTunnel> tunnelPools)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        // start by clearing all existing registrations
        nsmgr.clearOwnerRegistrationAll(NETSPACE_OWNER);

        // add registration for the configured address pool
        nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_SERVER, serverPool);

        // add reservation for all networks of all configured tunnels 
        for (WireGuardVpnTunnel tunnel : tunnelPools) {
            String[] networks = tunnel.getNetworks().split("\\n");
            for (int x = 0;x < networks.length;x++) {
                String item = networks[x].trim();
                if (item.length() == 0) continue;
                nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_TUNNEL, networks[x].trim());
            }
        }
    }

    /**
     * Function to check all configured network address blocks for conflicts
     * @param serverPool - server pool address space to validate against
     * @param tunnelPools - A list of WireGuardVpnTunnels to validate address spaces against     
     * @return A string describing the conflict or null if no conflicts are detected
     */
    private String checkNetworkReservations(IPMaskedAddress serverPool, List<WireGuardVpnTunnel> tunnelPools)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();
        NetworkSpace space = null;

        // check the address pool for conflicts
        space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, serverPool);
        if (space != null) {
            return new String("Address Pool conflicts with " + space.ownerName + ":" + space.ownerPurpose);
        }

        // check all tunnel networks for conflicts
        for (WireGuardVpnTunnel tunnel : tunnelPools) {
            String[] networks = tunnel.getNetworks().split("\\n");
            for (int x = 0;x < networks.length;x++) {
                String item = networks[x].trim();
                if (item.length() == 0) continue;
                IPMaskedAddress maskaddr = new IPMaskedAddress(item);
                // see if the tunnel network conflicts with our configured address space
                if (maskaddr.isIntersecting(serverPool)) {
                    return new String("Tunnel:" + tunnel.getDescription() + " Network:" + item + " conflicts with configured Address Space");
                }
                // see if the tunnel network conflicts with any registered networks
                space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, maskaddr);
                if (space != null) {
                    return new String("Tunnel:" + tunnel.getDescription() + " Network:" + item + " conflicts with " + space.ownerName + ":" + space.ownerPurpose);
                }
            }
        }

        return null;
    }
}
