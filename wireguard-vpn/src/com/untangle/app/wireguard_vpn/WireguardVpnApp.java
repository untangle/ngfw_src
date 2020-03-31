/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.File;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetspaceManager;
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
 * The Wireguard VPN application connects to 3rd party VPN tunnel providers.
 */
public class WireguardVpnApp extends AppBase
{
    private final static String WIREGUARD_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/wireguard-status";

    private final Logger logger = Logger.getLogger(getClass());

    private final String SettingsDirectory = "/wireguard-vpn/";

    private static final String STAT_PASS = "pass";

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private WireguardVpnSettings settings = null;
    private WireguardVpnMonitor WireguardVpnMonitor = null;
    private WireguardVpnManager WireguardVpnManager = null;
    private final WireguardVpnEventHandler handler;

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
    public WireguardVpnApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
        WireguardVpnMonitor = new WireguardVpnMonitor(this);
        this.WireguardVpnManager = new WireguardVpnManager(this);

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));

        this.handler = new WireguardVpnEventHandler(this);
        this.connector = UvmContextFactory.context().pipelineFoundry().create("wireguard-vpn", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 10, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public WireguardVpnSettings getSettings()
    {
        return settings;
    }

    /**
     * Get the wireguard Vpn manager
     * 
     * @return wireguard manager
     */
    public WireguardVpnManager getWireguardVpnManager(){
        logger.warn(this.WireguardVpnManager);
        return this.WireguardVpnManager;
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
    public void setSettings(final WireguardVpnSettings newSettings, boolean restart)
    {
        /**
         * First we check for network address space conflicts
         */
        String conflict = checkNetworkReservations(newSettings.getAddressPool(), newSettings.getTunnels());
        if (conflict != null) {
            throw new RuntimeException(conflict);
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

        this.WireguardVpnManager.configure();
        if(restart == true){
            this.WireguardVpnManager.restart();
        }
    }

    /**
     * Set the application settings and restart.
     *
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final WireguardVpnSettings newSettings)
    {
        setSettings(newSettings, true);
    }

    /**
     * Add a tunnel by publicKey
     * @param publicKey String of public key to add.
     */
    public void addTunnel(String publicKey)
    {
        this.WireguardVpnManager.addTunnel(publicKey);
    }

    /**
     * Delete a tunnel by publicKey
     * @param publicKey String of public key to delete.
     */
    public void deleteTunnel(String publicKey)
    {
        this.WireguardVpnManager.deleteTunnel(publicKey);
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
            this.WireguardVpnManager.configure();
            this.WireguardVpnManager.start();
        } catch (Exception e) {
            logger.error("Error during startup", e);
            try {
                this.WireguardVpnManager.stop();
            } catch (Exception stopException) {
                logger.error("Unable to stop the wireguard process", stopException);
            }
            throw new RuntimeException(e);
        }

        this.WireguardVpnMonitor.start();
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
        this.WireguardVpnMonitor.stop();

        this.WireguardVpnManager.stop();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        WireguardVpnSettings readSettings = null;

        try {
            readSettings = settingsManager.load(WireguardVpnSettings.class, this.getSettingsFilename());
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

        WireguardVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private WireguardVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        WireguardVpnSettings settings = new WireguardVpnSettings();

        String privateKey = this.getWireguardVpnManager().createPrivateKey().trim();
        String publicKey = this.getWireguardVpnManager().getPublicKey(privateKey).trim();
        settings.setPrivateKey(privateKey);
        settings.setPublicKey(publicKey);

        IPMaskedAddress newSpace = UvmContextFactory.context().netspaceManager().getAvailableAddressSpace();

        settings.setAutoAddressAssignment(true);
        settings.setAddressPool(newSpace);

        settings.setTunnels(new LinkedList<WireguardVpnTunnel>());

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
     * Function to register all network address blocks configured in this application
     *
     * @param serverPool - server pool address space to validate against
     * @param tunnelPools - A list of WireguardVpnTunnels to validate address spaces against
     */
    private void updateNetworkReservations(IPMaskedAddress serverPool, List<WireguardVpnTunnel> tunnelPools)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        // start by clearing all existing registrations
        nsmgr.clearOwnerRegistrationAll(NETSPACE_OWNER);

        // add registration for the configured address pool
        nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_SERVER, serverPool);

        // add reservation for all networks of all configured tunnels 
        for (WireguardVpnTunnel tunnel : tunnelPools) {
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
     * @param tunnelPools - A list of WireguardVpnTunnels to validate address spaces against     
     * @return A string describing the conflict or null if no conflicts are detected
     */
    private String checkNetworkReservations(IPMaskedAddress serverPool, List<WireguardVpnTunnel> tunnelPools)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();
        NetworkSpace space = null;

        // check the address pool for conflicts
        space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, serverPool);
        if (space != null) {
            return new String("Address Pool conflicts with " + space.ownerName + ":" + space.ownerPurpose);
        }

        // check all tunnel networks for conflicts
        for (WireguardVpnTunnel tunnel : tunnelPools) {
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
