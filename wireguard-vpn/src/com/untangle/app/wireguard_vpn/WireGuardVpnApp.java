/**
 * $Id$
 */

package com.untangle.app.wireguard_vpn;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.HookCallback;
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
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.network.NatRule;
import com.untangle.uvm.network.NatRuleCondition;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * The WireGuard VPN application connects to 3rd party VPN tunnel providers.
 */
public class WireGuardVpnApp extends AppBase
{
    private final static String WIREGUARD_STATUS_SCRIPT = System.getProperty("uvm.home") + "/bin/wireguard-status";

    private final Logger logger = LogManager.getLogger(getClass());

    private final String SettingsDirectory = "/wireguard-vpn/";

    private static final String STAT_PASS = "pass";

    private static final String WIREGUARD_AUTO_NAT_RULE_DESCRIPTION_DST = "AUTO: NAT WAN-bound wireguard vpn traffic";
    private static final String WIREGUARD_AUTO_NAT_RULE_DESCRIPTION_SRC = "AUTO: nat wireguard vpn traffic to the server";

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private WireGuardVpnSettings settings = null;
    private WireGuardVpnMonitor WireGuardVpnMonitor = null;
    private WireGuardVpnManager WireGuardVpnManager = null;
    private final WireGuardVpnEventHandler handler;

    private final WireguardVpnHookCallback wireguardVpnHookCallback;
    private final WireguardVpnPreHookCallback wireguardVpnPreHookCallback;

    private InetAddress localDnsResolver = null;
    private String defaultDnsSerachDomain = null;
    private List<InterfaceStatus> lanStatuses = null;
    private Hashtable<InterfaceStatus, WireGuardVpnNetwork> settingsLink = null;

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

        this.wireguardVpnHookCallback = new WireguardVpnHookCallback();
        this.wireguardVpnPreHookCallback= new WireguardVpnPreHookCallback();

        this.lanStatuses = UvmContextFactory.context().networkManager().getLocalInterfaceStatuses();
        this.localDnsResolver = UvmContextFactory.context().networkManager().getFirstDnsResolverAddress();
        this.defaultDnsSerachDomain = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
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
            } else {
                if (!this.WireGuardVpnManager.isValidWGKey(tunnel.getPublicKey())) {
                    logger.warn("Tunnel addition failed invalid publc key.");
                    throw new RuntimeException("Invalid Tunnel public key");

                }
            }
            // For tunnels pushed from ETM save the networks in NGFW format i.e. line seperated
            if(tunnel.getDescription().startsWith("CCTunnel") && tunnel.getNetworks().contains(Constants.COMMA_STRING)) {
                tunnel.setNetworks(tunnel.getNetworks().replaceAll(Constants.COMMA_STRING, Constants.NEW_LINE));
            }
        }

        /*
        * Fix up the WGN network ids
        */
        int wgnIdx = 0;
        for(WireGuardVpnNetwork localNets : newSettings.getNetworks()) {
            localNets.setId(++wgnIdx);
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

        Integer oldListenPort = 51820;
        if(this.settings != null){
            oldListenPort = this.settings.getListenPort();
        }

        /**
         * Change current settings and update network reservations
         * any time settings are saved.
         */
        this.settings = newSettings;
        updateNetworkReservations(newSettings.getAddressPool(), newSettings.getTunnels());

        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        this.WireGuardVpnManager.configure();

        if(!oldListenPort.equals(this.settings.getListenPort())){
            /**
             * Listen port changed; update reserved access rule.
             */
            UvmContextFactory.context().networkManager().updateReservedAccessRulePort( String.valueOf(oldListenPort), String.valueOf(this.settings.getListenPort()));
            restart = true;
        }


        if( ( this.getRunState() == AppSettings.AppState.RUNNING ) &&
            ( restart == true ) ){
            this.WireGuardVpnManager.restart();
        } else if (this.getRunState() != AppSettings.AppState.RUNNING) {
            // Make sure /etc/wireguard/wg.conf is removed 
            this.WireGuardVpnManager.removeConf();
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
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.PRE_NETWORK_SETTINGS_CHANGE, this.wireguardVpnPreHookCallback);
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.wireguardVpnHookCallback);
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
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.PRE_NETWORK_SETTINGS_CHANGE, this.wireguardVpnPreHookCallback);
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.wireguardVpnHookCallback);

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
            boolean writeFlag = false;

            /* 16.3 - convert endpointAddress to endpointHostname */
            for (WireGuardVpnTunnel tunnel : readSettings.getTunnels()) {
                if (tunnel.getEndpointAddress() != null) {
                    try {
                        tunnel.setEndpointHostname(tunnel.getEndpointAddress().getHostAddress());
                    } catch (Exception e) {
                        logger.warn("Failed to load settings because of wireguard tunnel: ", e);
                    }
                    writeFlag = true;
                }
            }

            // 17.3 - add dns search domain by default
            if(readSettings.getVersion() <= 4) {
                readSettings.setDnsSearchDomain(this.defaultDnsSerachDomain);
                writeFlag = true;
                readSettings.setVersion(5);
            }

            if (writeFlag == true) {
                // if any changes were made we need to write the updated settings
                this.setSettings( readSettings );
            } else {
                // no changes made so use the settings but don't write the file
                this.settings = readSettings;
            }

            updateNetworkReservations(readSettings.getAddressPool(), readSettings.getTunnels());
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    /**
     * Uninstall wireguard, remove the added destination rule if it still exists
     */
    @Override
    protected void uninstall() {
        removeAddedNatRules();
    }

    /**
     * Called to initialize application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        createNatRules();

        WireGuardVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    /**
     * called to add a destination NAT rule 
     */
    private void createNatRules() {
        logger.info("Adding nat rules from wireguard");

        // remove rules first
        removeAddedNatRules();

        // add nat rules to network settings
        List<NatRule> natRules = UvmContextFactory.context().networkManager().getNetworkSettings().getNatRules();

        /* Destination nat rule */
        List<NatRuleCondition> natRuleConditionsDst = new LinkedList<NatRuleCondition>();
        NatRuleCondition natRuleConditionDst = new NatRuleCondition();
        natRuleConditionDst.setConditionType(NatRuleCondition.ConditionType.DST_INTF);
        natRuleConditionDst.setValue(String.valueOf(InterfaceSettings.WIREGUARD_INTERFACE_ID));
        natRuleConditionsDst.add(natRuleConditionDst);

        NatRule natRuleDst = new NatRule();
        natRuleDst.setConditions(natRuleConditionsDst);
        natRuleDst.setEnabled(true);
        natRuleDst.setDescription(WIREGUARD_AUTO_NAT_RULE_DESCRIPTION_DST);
        natRuleDst.setAuto(true);
        natRuleDst.setNgfwAdded(true);
        natRuleDst.setAddedBy(getAppProperties().getClassName());
        natRules.add(natRuleDst);

        /* Src Nat Rule */
        List<NatRuleCondition> natRuleConditionsSrc = new LinkedList<NatRuleCondition>();
        NatRuleCondition natRuleConditionSrc = new NatRuleCondition();
        natRuleConditionSrc.setConditionType(NatRuleCondition.ConditionType.SRC_INTF);
        natRuleConditionSrc.setValue(String.valueOf(InterfaceSettings.WIREGUARD_INTERFACE_ID));
        natRuleConditionsSrc.add(natRuleConditionSrc);

        NatRule natRuleSrc = new NatRule();
        natRuleSrc.setConditions(natRuleConditionsSrc);
        natRuleSrc.setEnabled(true);
        natRuleSrc.setDescription(WIREGUARD_AUTO_NAT_RULE_DESCRIPTION_SRC);
        natRuleSrc.setAuto(true);
        natRuleSrc.setNgfwAdded(true);
        natRuleSrc.setAddedBy(getAppProperties().getClassName());
        natRules.add(natRuleSrc);

        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        networkSettings.setNatRules(natRules);
        UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
    }

    /**
     * Remove wireguard added rules from network settings
     */
    private void removeAddedNatRules() {
        List<NatRule> natRules = UvmContextFactory.context().networkManager().getNetworkSettings().getNatRules();   
        List<NatRule> toRemove = null;
        for (NatRule rule : natRules) {
            if (rule.getNgfwAdded() && rule.getAddedBy().equals(getAppProperties().getClassName())) {
                if(toRemove == null) {
                    toRemove = new LinkedList<NatRule>();
                }
                toRemove.add(rule);
            }
        }

        if (toRemove != null) {
            natRules.removeAll(toRemove);
            NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
            networkSettings.setNatRules(natRules);
            UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
        }
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

        InetAddress dnsAddress = this.localDnsResolver;
        if(dnsAddress != null){
            logger.warn(dnsAddress);
            settings.setDnsServer(dnsAddress);
        }
        String defaultSearchDomain = this.defaultDnsSerachDomain;
        if(defaultSearchDomain != null){
            logger.warn(defaultSearchDomain);
            settings.setDnsSearchDomain(defaultSearchDomain);
        }
        settings.setNetworks(buildNetworkList(lanStatuses));

        IPMaskedAddress newSpace = UvmContextFactory.context().netspaceManager().getAvailableAddressSpace(IPVersion.IPv4, 1);

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
        //Set rateLimit to true when calling execOutput as this log message can be chatty in logs
        String result = UvmContextFactory.context().execManager().execOutput(WIREGUARD_STATUS_SCRIPT, true);
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
        IPMaskedAddress newSpace = UvmContextFactory.context().netspaceManager().getAvailableAddressSpace(IPVersion.IPv4, 1);
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

    /**
     * Creates a list of WireGuardVpnNetwork objects from the passed list of
     * InterfaceStatus objects.
     *
     * @param intfStatuses
     *        - The list of InterfaceStatus objects
     * @return The list of WireGuardVpnNetwork objects
     */
    protected List<WireGuardVpnNetwork> buildNetworkList(List<InterfaceStatus> intfStatuses)
    {
        LinkedList<WireGuardVpnNetwork> networkList = new LinkedList<WireGuardVpnNetwork>();

        if (intfStatuses == null) {
            return networkList;
        }

        for (InterfaceStatus intfStatus : intfStatuses) {
            if (intfStatus.getV4MaskedAddress() != null) {
                networkList.add(new WireGuardVpnNetwork(intfStatus.getV4MaskedAddress().getIPMaskedAddress()));
            }
        }

        return networkList;
    }

    /**
     * Called when network settings have changed
     * @throws Exception
     */
    private void networkSettingsEvent() throws Exception
    {
        logger.info("Network Settings have changed. Syncing new settings...");

        // Check if the old settings (settings.getdnsserver) were being used previously (this.localdnsresolver)
        // if so and the old settings do not match the new settings, we should update them using the new data
        if(settings != null) {          
            InetAddress newDnsResolver = UvmContextFactory.context().networkManager().getFirstDnsResolverAddress();
            List<InterfaceStatus> newLanStatuses = UvmContextFactory.context().networkManager().getLocalInterfaceStatuses();
            boolean setNewSettings = false;

            if(settings.getDnsServer().equals(this.localDnsResolver) && !this.localDnsResolver.equals(newDnsResolver)) {
                // Set newDnsResolver in the settings and also the local variable
                settings.setDnsServer(newDnsResolver);
                this.localDnsResolver = newDnsResolver;
                setNewSettings = true;
            }

            // Check the newNetworks against our hash table to see if any of the networks have changed
            for(InterfaceStatus oldIntf : this.settingsLink.keySet())
            {
                for(InterfaceStatus intf : newLanStatuses) {
                    // Found this interface in the interface lookup
                    if(intf.getInterfaceId() == oldIntf.getInterfaceId()){
                        //pull old wireguard settings out for this network
                        WireGuardVpnNetwork oldWgn =  this.settingsLink.get(oldIntf);
                        // Check if the wireguard network is configured for this IP family and has changed
                        if(intf.getV4MaskedAddress() != null && oldWgn.getMaskedAddress().getAddress() instanceof Inet4Address && ! oldWgn.getMaskedAddress().getIPMaskedAddress().equals(intf.getV4MaskedAddress().getIPMaskedAddress())){
                            // This interface has changed, find the settings in new settings and fix it
                            for(WireGuardVpnNetwork wvn : settings.getNetworks()) {
                                if( wvn.getId() ==  oldWgn.getId()) {
                                    wvn.setAddress(intf.getV4MaskedAddress().getIPMaskedAddress());
                                    setNewSettings = true;
                                }
                            }
                        }

                        // Check if the wireguard network is configured for this IP family and has changed
                        if(intf.getV6MaskedAddress() != null &&  oldWgn.getMaskedAddress().getAddress() instanceof Inet6Address && ! oldWgn.getMaskedAddress().getIPMaskedAddress().equals(intf.getV6MaskedAddress().getIPMaskedAddress())){
                            // This interface has changed, find the settings in new settings and fix it
                            for(WireGuardVpnNetwork wvn : settings.getNetworks()) {
                                if( wvn.getId() ==  oldWgn.getId()) {
                                    wvn.setAddress(intf.getV6MaskedAddress().getIPMaskedAddress());
                                    setNewSettings = true;
                                }
                            }
                        }
                    }
                }
            }

            if(setNewSettings) {
                setSettings(settings);
            }
        }
    }

    /**
     * preNetworkSettingsEvent stores the localNetworks and localDnsResolver settings prior to the network settings change
     * 
     */
    private void preNetworkSettingsEvent()
    {
        logger.info("Network Settings will change, storing local settings...");
        //get the localnetworks and dnsresolver into the local variables before the settings change
        this.lanStatuses = UvmContextFactory.context().networkManager().getLocalInterfaceStatuses();
        this.localDnsResolver = UvmContextFactory.context().networkManager().getFirstDnsResolverAddress();
        this.settingsLink = new Hashtable<InterfaceStatus, WireGuardVpnNetwork>();

        // Store the WG settings and the interface status to watch through the network settings change
        for(var wgNet : settings.getNetworks() ) {
            for(InterfaceStatus intfStatus : this.lanStatuses) {
                if(wgNet.getMaskedAddress().getIPMaskedAddress().equals(intfStatus.getV4MaskedAddress().getIPMaskedAddress()) || wgNet.getMaskedAddress().getIPMaskedAddress().equals(intfStatus.getV6MaskedAddress().getIPMaskedAddress())) {
                    settingsLink.put(intfStatus, wgNet);
                }
            }
        }
    }

     /**
     * Callback hook for changes to network settings
     * 
     * @author mahotz
     * 
     */
    private class WireguardVpnHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         * 
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "wireguard-network-settings-change-hook";
        }

        /**
         * Callback handler
         * 
         * @param args
         *        The callback arguments
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof NetworkSettings)) {
                logger.warn("Invalid network settings: " + o);
                return;
            }

            NetworkSettings settings = (NetworkSettings) o;

            if (logger.isDebugEnabled()) logger.debug("network settings changed:" + settings);

            try {
                networkSettingsEvent();
            } catch (Exception e) {
                logger.error("Unable to reconfigure the NAT app");
            }
        }
    }

    /**
     * WireguardVpnPreHookCallback is used to load the old network settings locally before the network settings have changed
     */
    private class WireguardVpnPreHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         * 
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "wireguard-pre-network-settings-change-hook";
        }

        /**
         * Callback handler
         * 
         * @param args
         *        The callback arguments
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof NetworkSettings)) {
                logger.warn("Invalid network settings: " + o);
                return;
            }

            NetworkSettings settings = (NetworkSettings) o;

            if (logger.isDebugEnabled()) logger.debug("network settings changed:" + settings);

            try {
                preNetworkSettingsEvent();
            } catch (Exception e) {
                logger.error("Unable to reconfigure the Wireguard VPN app");
            }
        }
    }
}
