/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.util.List;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.Collections;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;
import org.apache.commons.fileupload.FileItem;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.servlet.UploadHandler;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * The Tunnel VPN application connects to 3rd party VPN tunnel providers.
 * 
 * @author mahotz
 * 
 */
public class TunnelVpnApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    public static final String TUNNEL_LOG = "/var/log/uvm/tunnel.log";
    public static final int BASE_MGMT_PORT = 2000;

    private TunnelVpnSettings settings = null;
    private TunnelVpnManager tunnelVpnManager = new TunnelVpnManager(this);
    private TunnelVpnMonitor tunnelVpnMonitor;

    private final TunnelVpnNetworkHookCallback networkHookCallback = new TunnelVpnNetworkHookCallback();

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public TunnelVpnApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);

        UvmContextFactory.context().servletFileManager().registerUploadHandler(new TunnelUploadHandler());
        tunnelVpnMonitor = new TunnelVpnMonitor(this, tunnelVpnManager);
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public TunnelVpnSettings getSettings()
    {
        return settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(final TunnelVpnSettings newSettings)
    {
        /**
         * Number the rules
         */
        int idx = 0;
        for (TunnelVpnRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);
        }

        /**
         * Save the settings
         */
        String appID = this.getAppSettings().getId().toString();
        try {
            UvmContextFactory.context().settingsManager().save( System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/" + "settings_"  + appID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Clean up stuff from tunnels that have been removed
         */
        cleanTunnelSettings();

        /**
         * Synchronize settings with NetworkSettings
         * 1) Any tunnel interfaces that exists that aren't in network settings should be added
         * 2) Any tunnel interfaces that exist in network settings but not in tunnel VPN
         *    because they have been removed should be removed from network settins
         */
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        boolean networkSettingsChanged = false;
        List<TunnelVpnTunnelSettings> missing = findTunnelsMissingFromNetworkSettings();
        if (missing.size() > 0) {
            for( TunnelVpnTunnelSettings tunnelSettings : missing ) {
                /**
                 * Set Network Settings (add new virtual interface)
                 */
                InterfaceSettings virtualIntf = new InterfaceSettings(tunnelSettings.getTunnelId(),tunnelSettings.getName());
                virtualIntf.setIsVirtualInterface(true);
                virtualIntf.setIsWan(true);
                virtualIntf.setConfigType(null);
                virtualIntf.setV4ConfigType(null);
                virtualIntf.setV4Aliases(null);
                virtualIntf.setV6ConfigType(null);
                virtualIntf.setV6Aliases(null);
                virtualIntf.setVrrpAliases(null);
                virtualInterfaces.add(virtualIntf);
                logger.info("Adding new virtual interface: " + tunnelSettings.getTunnelId() + " " + tunnelSettings.getName());

            }
            networkSettingsChanged = true;
        }
        List<InterfaceSettings> extra = findExtraVirtualInterfaces();
        if (extra.size() > 0) {
            for (Iterator<InterfaceSettings> i = virtualInterfaces.iterator(); i.hasNext();) {
                InterfaceSettings virtualIntf = i.next();
                Optional<InterfaceSettings> is = extra.stream().filter(x -> x.getInterfaceId() == virtualIntf.getInterfaceId()).findFirst();
                if(is.isPresent()) {
                    logger.info("Removing unused virtual interface: " + virtualIntf.getInterfaceId() + " " + virtualIntf.getName());
                    i.remove();
                }
            }
            networkSettingsChanged = true;
        }

        /**
         * sync these settings to the filesystem
         */
        syncToSystem((this.getRunState() == AppSettings.AppState.RUNNING));

        if (networkSettingsChanged) {
            try {
                UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
            } catch (Exception e) {
                logger.warn("Failed to save network settings",e);
            }
            // processes will be automatically restarted after this is complete by the network settings hook
        } else {
            // restart tunnels
            if(this.getRunState() == AppSettings.AppState.RUNNING)
                this.tunnelVpnManager.restartProcesses();
        }
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
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);
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
        insertIptablesRules();
        this.tunnelVpnManager.launchProcesses();
        this.tunnelVpnMonitor.start();
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
        this.tunnelVpnMonitor.stop();
        this.tunnelVpnManager.killProcesses();
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.networkHookCallback);
    }

    /**
     * Called after the application is destroyed
     */
    @Override
    protected void postDestroy()
    {
        syncToSystem(false);
        removeAllTunnelVirtualInterfaces();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        TunnelVpnSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/tunnel-vpn/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(TunnelVpnSettings.class, settingsFilename);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            this.initializeSettings();
        } else {
            logger.info("Loading Settings...");

            /**
             * If the settings file date is newer than the system files, re-sync
             * them
             */
            if (!UvmContextFactory.context().isDevel()) {
                File settingsFile = new File(settingsFilename);
                File outputFile = new File("/etc/untangle/iptables-rules.d/350-tunnel-vpn");
                if (settingsFile.lastModified() > outputFile.lastModified()) {
                    logger.warn("Settings file newer than interfaces files, Syncing...");
                    this.setSettings(readSettings);
                }
            }

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    /**
     * Called to initialize application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        TunnelVpnSettings settings = getDefaultSettings();

        setSettings(settings);
    }

    /**
     * Called to get a new tunnel ID value when creating a new tunnel
     * 
     * @return A tunnel ID value
     */
    public int getNewTunnelId()
    {
        return this.tunnelVpnManager.getNewTunnelId();
    }

    /**
     * Get the application log file
     * 
     * @return The application log file
     */
    public String getLogFile()
    {
        File f = new File(TUNNEL_LOG);
        if (f.exists()) {
            String output = UvmContextFactory.context().execManager().execOutput("tail -n 200 " + TUNNEL_LOG);
            if (output == null) return null;

            // remove strings that we don't want to show the users
            // these are strings/errors that are normal but scary to the user
            String[] lines = output.split("\\n");
            List<String> list = new LinkedList<>();
            Collections.addAll(list, lines);
            for (Iterator<String> i = list.iterator(); i.hasNext();) {
                String str = i.next();
                if (str.contains("unable to redirect default gateway -- Cannot read current default gateway from system")) i.remove();
                if (str.contains("MANAGEMENT:")) i.remove();
            }
            String finalstr = list.stream().collect(Collectors.joining("\n"));
            return finalstr;
        } else {
            return null;
        }
    }

    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private TunnelVpnSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        TunnelVpnSettings settings = new TunnelVpnSettings();

        List<TunnelVpnRule> rules = new LinkedList<>();
        TunnelVpnRule rule;
        List<TunnelVpnRuleCondition> conditions;
        TunnelVpnRuleCondition condition1;

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Route all traffic over any available Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<>();
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route all hosts tagged with \"tunnel\" over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.CLIENT_TAGGED, "tunnel");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route all hosts tagged with \"bittorrent-usage\" over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.CLIENT_TAGGED, "bittorrent-usage");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);

        rule = new TunnelVpnRule();
        rule.setEnabled(false);
        rule.setDescription("Example: Route TCP port 80 and port 443 over any Tunnel.");
        rule.setTunnelId(-1); //any tunnel
        conditions = new LinkedList<>();
        condition1 = new TunnelVpnRuleCondition(TunnelVpnRuleCondition.ConditionType.DST_PORT, "80,443");
        conditions.add(condition1);
        rule.setConditions(conditions);
        rules.add(rule);

        settings.setRules(rules);
        return settings;
    }

    /**
     * Imports a new tunnel configuration
     * 
     * @param filename
     *        The name of the file
     * @param provider
     *        The tunnel provider
     * @param tunnelId
     *        The tunnel ID
     */
    public void importTunnelConfig(String filename, String provider, int tunnelId)
    {
        this.tunnelVpnManager.importTunnelConfig(filename, provider, tunnelId);

        if (this.getRunState() == AppSettings.AppState.RUNNING) this.tunnelVpnManager.restartProcesses();
    }

    /**
     * Called by the UI to get the status of all configured tunnels
     * 
     * @return The tunnel status list
     */
    public LinkedList<TunnelVpnTunnelStatus> getTunnelStatusList()
    {
        return this.tunnelVpnMonitor.getTunnelStatusList();
    }

    /**
     * Called by the UI to stop and restart a tunnel
     * 
     * @param tunnelId
     *        The tunnel to be restarted
     */
    public void recycleTunnel(int tunnelId)
    {
        tunnelVpnManager.recycleTunnel(tunnelId);
        tunnelVpnMonitor.recycleTunnel(tunnelId);
    }

    /**
     * This finds all the tunnels that do not have corresponding virtual
     * interfaces in the current network settings.
     * 
     * @return a list of the tunnels missing virtual interfaces (never null)
     */
    private List<TunnelVpnTunnelSettings> findTunnelsMissingFromNetworkSettings()
    {
        List<TunnelVpnTunnelSettings> missing = new LinkedList<>();

        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        for(TunnelVpnTunnelSettings tunnelSettings : settings.getTunnels()) {
            Optional<InterfaceSettings> is = virtualInterfaces.stream().filter(x -> x.getInterfaceId() == tunnelSettings.getTunnelId()).findFirst();
            if(!is.isPresent()) {
                missing.add(tunnelSettings);
            }
        }
        return missing;
    }

    /**
     * This finds all the tunnel virtual interfaces in the current network
     * settings that do not have corresponding tunnel settings in the tunnel VPN
     * settings
     * 
     * @return a list of the extra virtual interfaces (never null)
     */
    private List<InterfaceSettings> findExtraVirtualInterfaces()
    {
        List<InterfaceSettings> extra = new LinkedList<>();

        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> virtualInterfaces = networkSettings.getVirtualInterfaces();

        for(InterfaceSettings interfaceSetings : virtualInterfaces) {
            if ( !interfaceSetings.getIsWan() ) //ignore all the non-wan virtual interfaces (openvpn, ipsec, etc)
                continue;
            Optional<TunnelVpnTunnelSettings> is = settings.getTunnels().stream().filter(x -> x.getTunnelId() == interfaceSetings.getInterfaceId()).findFirst();
            if(!is.isPresent()) {
                extra.add(interfaceSetings);
            }
        }

        return extra;
    }

    /**
     * This removes all tunnels from network settings
     */
    private void removeAllTunnelVirtualInterfaces()
    {
        NetworkSettings networkSettings = UvmContextFactory.context().networkManager().getNetworkSettings();
        List<InterfaceSettings> nonTunnelInterfaces = networkSettings.getVirtualInterfaces().stream().filter(x -> !x.getIsWan()).collect(Collectors.toList());
        networkSettings.setVirtualInterfaces(nonTunnelInterfaces);
        UvmContextFactory.context().networkManager().setNetworkSettings(networkSettings);
    }

    /**
     * Sync the settings to the filesystem
     * 
     * @param enabled
     *        The application enabled flag
     */
    private void syncToSystem(boolean enabled)
    {
        logger.info("syncToSystem()...");

        /**
         * First we write a new 350-tunnel-vpn iptables script with the current
         * settings
         */
        String appID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "tunnel-vpn/" + "settings_" + appID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/tunnel-vpn-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String cmd = scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename;
        if (!enabled)
            cmd += " -d";
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        String lines[] = output.split("\\r?\\n");
        for (String line : lines)
            logger.info("Sync Settings: " + line);

        if (enabled) insertIptablesRules();
    }

    /**
     * Looks for and removes files from tunnels that no longer exist
     */
    private void cleanTunnelSettings()
    {
        String directory = System.getProperty("uvm.settings.dir") + "/tunnel-vpn";
        File file = new File(directory);
        String list[] = file.list();
        boolean found;

        for (String name : list) {
            // check for a name that starts with our directory prefix
            if (!name.startsWith("tunnel-")) continue;

            // make sure the file is a directory
            File target = new File(directory + "/" + name);
            if (!target.isDirectory()) continue;

            String path = (directory + "/" + name);
            logger.info("Cleanup checking: " + path);

            // extract the tunnel id from the directory name
            String idString = name.substring(name.lastIndexOf("-") + 1);
            int idValue = Integer.valueOf(idString);

            // check the settings to see if this is a valid tunnel
            found = false;

            for (TunnelVpnTunnelSettings tunnelSettings : getSettings().getTunnels()) {
                if (tunnelSettings.getTunnelId() != idValue) continue;
                found = true;
                break;
            }

            if (found == true) continue;

            // no matching tunnel so get rid of the directory
            logger.info("Cleanup removing: " + path);
            UvmContextFactory.context().execManager().exec("rm -r -f " + path);
        }
    }

    /**
     * Inserts the iptables rules
     */
    private void insertIptablesRules()
    {
        /**
         * Run the iptables script
         */
        String output = UvmContextFactory.context().execManager().execOutput("/etc/untangle/iptables-rules.d/350-tunnel-vpn");
        String lines[] = output.split("\\r?\\n");
        for (String line : lines)
            logger.info("Adding tunnel-vpn iptables: " + line);

    }

    /**
     * Called when network settings have changed
     * 
     * @param settings
     *        The new network settings
     * @throws Exception
     */
    private void networkSettingsEvent(NetworkSettings settings) throws Exception
    {
        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Restarting tunnels...");

        if (this.getRunState() == AppSettings.AppState.RUNNING) this.tunnelVpnManager.restartProcesses();
    }

    /**
     * Handler for uploaded tunnel configuration files
     */
    private class TunnelUploadHandler implements UploadHandler
    {
        /**
         * Get the name of the upload handler
         * 
         * @return The name of the upload handler
         */
        @Override
        public String getName()
        {
            return "tunnel_vpn";
        }

        /**
         * Handler for uploaded files
         * 
         * @param fileItem
         *        The uploaded file
         * @param argument
         *        Upload argument
         * @return The upload result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            if (fileItem == null) {
                logger.info("UploadTunnel is missing the file.");
                return new ExecManagerResult(1, "Tunnel VPN is missing the file" + ": " + fileItem.getName());
            }

            InputStream inputStream = fileItem.getInputStream();
            if (inputStream == null) {
                logger.info("UploadTunnel is missing the file.");
                return new ExecManagerResult(1, "Tunnel VPN is missing the file" + ": " + fileItem.getName());
            }

            logger.info("Uploaded new tunnel config: " + fileItem.getName() + " " + argument);

            /* Write out the file. */
            File temp = null;
            OutputStream outputStream = null;
            try {
                String filename = fileItem.getName();
                if (filename.endsWith(".zip")) {
                    temp = File.createTempFile("tunnel-vpn-newconfig-", ".zip");
                } else if (filename.endsWith(".conf")) {
                    temp = File.createTempFile("tunnel-vpn-newconfig-", ".conf");
                } else if (filename.endsWith(".ovpn")) {
                    temp = File.createTempFile("tunnel-vpn-newconfig-", ".ovpn");
                } else {
                    return new ExecManagerResult(1, "Unknown file extension for Tunnel VPN" + ": " + fileItem.getName());
                }

                temp.deleteOnExit();
                outputStream = new FileOutputStream(temp);

                byte[] data = new byte[1024];
                int len = 0;
                while ((len = inputStream.read(data)) > 0)
                    outputStream.write(data, 0, len);
            } catch (IOException e) {
                logger.warn("Unable to validate client file.", e);
                return new ExecManagerResult(1, e.getMessage() + ": " + (fileItem != null ? fileItem.getName() : ""));
            } finally {
                try {
                    if (outputStream != null) outputStream.close();
                } catch (Exception e) {
                    logger.warn("Error closing output stream", e);
                }

                try {
                    if (inputStream != null) inputStream.close();
                } catch (Exception e) {
                    logger.warn("Error closing input stream", e);
                }
            }

            try {
                tunnelVpnManager.validateTunnelConfig(temp.getPath(), argument);
            } catch (Exception e) {
                logger.warn("Unable to validate the client configuration", e);
                return new ExecManagerResult(1, e.getMessage() + ": " + fileItem.getName());
            }

            return new ExecManagerResult(0, temp.getPath() + '&' + "Validated" + ": " + (fileItem != null ? fileItem.getName(): "" ));
        }
    }

    /**
     * Callback hook for changes to network settings
     */
    private class TunnelVpnNetworkHookCallback implements HookCallback
    {
        /**
         * Gets the name of the callback hook
         * 
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "tunnel-vpn-network-settings-change-hook";
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

            try {
                NetworkSettings settings = (NetworkSettings) o;
                networkSettingsEvent(settings);
            } catch (Exception e) {
                logger.error("Unable to reconfigure the NAT app");
            }
        }
    }
}
