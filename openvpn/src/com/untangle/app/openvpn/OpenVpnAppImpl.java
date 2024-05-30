/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.net.URLEncoder;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.NetspaceManager;
import com.untangle.uvm.NetspaceManager.NetworkSpace;
import com.untangle.uvm.NetspaceManager.IPVersion;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.network.NetworkSettings;
import com.untangle.uvm.network.InterfaceSettings;
import com.untangle.uvm.network.InterfaceStatus;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.DirectoryConnector;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * The OpenVPN application
 * 
 * @author mahotz
 * 
 */
public class OpenVpnAppImpl extends AppBase
{

    private static final Integer SETTINGS_CURRENT_VERSION = 1;

    private final Logger logger = Logger.getLogger(getClass());

    private static final String GENERATE_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-certs";
    private static final String GENERATE_CLIENT_CERTS_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-generate-client-certs";
    private static final String IMPORT_CLIENT_SCRIPT = System.getProperty("uvm.bin.dir") + "/openvpn-import-config";

    private static final String STAT_PASS = "pass";
    private static final String STAT_CONNECT = "connect";

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final EventHandler handler;

    private final OpenVpnMonitor openVpnMonitor;
    private final OpenVpnManager openVpnManager;

    private final OpenVpnHookCallback openVpnHookCallback;
    private final OpenVpnPreHookCallback openVpnPreHookCallback;

    private List<OpenVpnExport> localExports = null;
    private String localHostName = "";

    private OpenVpnSettings settings;

    private boolean isWebAppDeployed = false;

    private static final String NETSPACE_OWNER = "openvpn";
    private static final String NETSPACE_SERVER = "server-network";
    private static final String NETSPACE_REMOTE = "remote-network";

    /**
     * Constructor
     * 
     * @param appSettings
     *        Application settings
     * @param appProperties
     *        Application properties
     */
    public OpenVpnAppImpl(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.handler = new EventHandler(this);
        this.openVpnMonitor = new OpenVpnMonitor(this);
        this.openVpnManager = new OpenVpnManager(this);
        this.openVpnHookCallback = new OpenVpnHookCallback();
        this.openVpnPreHookCallback = new OpenVpnPreHookCallback();

        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new AppMetric(STAT_CONNECT, I18nUtil.marktr("Clients Connected")));

        this.connector = UvmContextFactory.context().pipelineFoundry().create("openvpn", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 10, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Called to get our pipeline connectors
     * 
     * @return Pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called after application initialization.
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        OpenVpnSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/openvpn/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(OpenVpnSettings.class, settingsFileName);
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
            updateSettings(readSettings);
            logger.info("Loading Settings...");
            this.settings = readSettings;
            updateNetworkReservations(readSettings.getAddressSpace(), readSettings.getRemoteClients());
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        /**
         * In OpenVPN deploy the webapp on init instead of start because the
         * webapp is needed for configuration while openvpn is off
         */
        deployWebApp();
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
        try {
            this.openVpnManager.configure(settings);
            this.openVpnManager.start();
        } catch (Exception e) {
            logger.error("Error during startup", e);
            try {
                this.openVpnManager.stop();
            } catch (Exception stopException) {
                logger.error("Unable to stop the openvpn process", stopException);
            }
            throw new RuntimeException(e);
        }

        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.PRE_NETWORK_SETTINGS_CHANGE, this.openVpnPreHookCallback);
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.openVpnHookCallback);

        this.openVpnMonitor.start();
        this.openVpnMonitor.enable();
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
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.PRE_NETWORK_SETTINGS_CHANGE, this.openVpnPreHookCallback);
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.NETWORK_SETTINGS_CHANGE, this.openVpnHookCallback);

        try {
            this.openVpnMonitor.disable();
            this.openVpnMonitor.stop();
        } catch (Exception e) {
            logger.warn("Error disabling openvpn monitor", e);
        }

        try {
            this.openVpnManager.stop();
        } catch (Exception e) {
            logger.warn("Error stopping openvpn manager", e);
        }
    }

    /**
     * Called after the application is destroyed
     */
    @Override
    protected void postDestroy()
    {
        unDeployWebApp();

        // purge all settings files (but not the actual settings json file)
        UvmContextFactory.context().execManager().exec("rm -f " + "/etc/openvpn/address-pool-assignments.txt");
        UvmContextFactory.context().execManager().exec("rm -f " + "/etc/openvpn/keys/*");
        UvmContextFactory.context().execManager().exec("rm -rf " + "/etc/openvpn/untangle-vpn");
        UvmContextFactory.context().execManager().exec("rm -rf " + "/etc/openvpn/data/*");
        UvmContextFactory.context().execManager().exec("rm -rf " + "/etc/openvpn/ccd/*");
        UvmContextFactory.context().execManager().exec("rm -f " + "/etc/openvpn/*.conf");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/remote-clients/*");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.key");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.pem");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/*.crt");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/index*");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/serial*");
        UvmContextFactory.context().execManager().exec("rm -f " + System.getProperty("uvm.settings.dir") + "/openvpn/server*");
        UvmContextFactory.context().execManager().exec("rm -rf " + System.getProperty("uvm.settings.dir") + "/openvpn/remote-servers/*");
    }

    /**
     * Initialize new application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings...");

        setSettings(getDefaultSettings());

        ExecManagerResult result = UvmContextFactory.context().execManager().exec(GENERATE_CERTS_SCRIPT);

        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(GENERATE_CERTS_SCRIPT + ": ");
            for (String line : lines)
                logger.info(GENERATE_CERTS_SCRIPT + ": " + line);
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to generate CA (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to generate CA");
        }
    }

    /**
     * Get the application settings
     * 
     * @return The settings for the application instance
     */
    public OpenVpnSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new application settings
     */
    public void setSettings(OpenVpnSettings newSettings)
    {
        /**
         * Verify Settings
         */
        sanityCheckSettings(newSettings);

        /**
         * Sanitize Settings
         */
        sanitizeSettings(newSettings);

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/" + "openvpn/" + "settings_" + appID + ".js", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        boolean restartRequired = true;
        
        if(this.settings != null) {
            restartRequired = isRestartRequired(this.settings, newSettings);
        }

        /**
         * Change current settings and update network reservations
         * any time settings are saved
         */
        this.settings = newSettings;
        updateNetworkReservations(newSettings.getAddressSpace(), newSettings.getRemoteClients());
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        /**
         * Sync those settings
         */
        this.openVpnManager.configure(this.settings);

        /**
         * Restart the daemon if we changed a setting that requires OpenVPN service restart
         * 
         * */
        if(restartRequired) {
            logger.debug("Restarting openvpn service...");
            try {
                if (getRunState() == AppSettings.AppState.RUNNING) {
                    this.openVpnManager.restart();
                }
            } catch (Exception exn) {
                logger.error("Could not save VPN settings", exn);
            }
        }
        
        /**
         * Clean up stuff from clients and servers that have been removed
         */
        try {
            cleanClientSettings();
            cleanClientPackages();
            cleanServerSettings();
        } catch (Exception exn) {
            logger.warn("Exception during client/server cleanup", exn);
        }
    }

    /**
     * isRestartRequired checks for field changes that would require a service restart
     * 
     * @param oldSettings - The previous settings before the save
     * @param newSettings - The new settings to be saved
     * @return boolean - True if a restart of the OpenVpn service is required, False if not
     * 
     */
    private boolean isRestartRequired(OpenVpnSettings oldSettings, OpenVpnSettings newSettings) {

        // Server enabled/disabled
        if (oldSettings.getServerEnabled() != newSettings.getServerEnabled()) { logger.debug("Server is Enabled has changed."); return true;}

        // Address space changes
        if (!oldSettings.getAddressSpace().equals(newSettings.getAddressSpace())) { logger.debug("Server Address space has changed."); return true;}

        // Authentication type changes
        if (oldSettings.getAuthenticationType() != newSettings.getAuthenticationType()) { logger.debug("Server Authentication Type has changed."); return true;}

        // AuthUserPass changes
        if (oldSettings.getAuthUserPass() != newSettings.getAuthUserPass()) { logger.debug("Server Authentication User Password Data has changed."); return true;}

        // Protocol changes
        if (!oldSettings.getProtocol().equals(newSettings.getProtocol())) { logger.debug("Server Protocol has changed."); return true;}

        // Remote Server changes
        if (oldSettings.getRemoteServers() == null ||
                newSettings.getRemoteServers() == null ||
                oldSettings.getRemoteServers().size() != newSettings.getRemoteServers().size() ||
                !oldSettings.getRemoteServers().containsAll(newSettings.getRemoteServers())) {
            logger.debug("Remote servers settings have changed.");
            return true;
        }

        // Listening Port changes
        if (oldSettings.getPort() != newSettings.getPort()) { logger.debug("Server Port has changed."); return true;}

        // Cipher changes
        if (!oldSettings.getCipher().equals(newSettings.getCipher())) { logger.debug("Server Cipher has changed."); return true;}

        // Client to client enabled/disabled
        if (oldSettings.getClientToClient() != newSettings.getClientToClient()) { logger.debug("Server Client to Client has changed."); return true;}

        // Server config settings (Use data stream to compare a json string of the entire list)
        if(oldSettings.getServerConfiguration().stream().map(u -> u.toJSONString()).collect(Collectors.joining("")).compareTo(newSettings.getServerConfiguration().stream().map(u -> u.toJSONString()).collect(Collectors.joining(""))) != 0) { logger.debug("Custom Server Configuration Items have changed."); return true; }

        // Exported networks (Use data stream to compare a json string of the entire list)
        if(oldSettings.getExports().stream().map(u -> u.toJSONString()).collect(Collectors.joining("")).compareTo(newSettings.getExports().stream().map(u -> u.toJSONString()).collect(Collectors.joining(""))) != 0) {logger.debug("Exported network items have changed."); return true;}

        return false;
    }
   
    /**
     * Verifies current OpenVpnSettings version, and updates to latest verson.
     *
     * @param  settings Current OpenVpnSettings
     * @return          Nothing
     */
    private void updateSettings(OpenVpnSettings settings){
        if(settings.getVersion() < SETTINGS_CURRENT_VERSION){
            logger.info("OpenVPN Settings require an update...");

            /**
             * Fix up the "compress lz4" compression settings for the server
             */

            for (OpenVpnConfigItem serverConfig : settings.getServerConfiguration()) {
                if ( serverConfig.getOptionName() != null && Objects.equals(serverConfig.getOptionName(), "compress lz4")) {
                    serverConfig.setOptionName("compress");
                    serverConfig.setOptionValue("lz4");
                }
            }

            /**
             * Fix up the "compress lz4" compression settings for the client
             */
            for (OpenVpnConfigItem clientConfig : settings.getClientConfiguration()) {
                if ( clientConfig.getOptionName() != null && Objects.equals(clientConfig.getOptionName(), "compress lz4")) {
                    clientConfig.setOptionName("compress");
                    clientConfig.setOptionValue("lz4");
                }
            }

            settings.setVersion(SETTINGS_CURRENT_VERSION);
            this.setSettings( settings );
        }
    }

    /**
     * 
     * @param username
     *        The username for authentication
     * @param password
     *        The password for authentication
     * @return True for authentication success, otherwise false
     */
    public int userAuthenticate(String username, String password) 
    {
        return userAuthenticate(username, password, false, 0);
    }

    /**
     * 
     * @param username
     *        The username for authentication
     * @param password
     *        The password for authentication
     * @param otpcode
     *        The otp code to use
     * @return True for authentication success, otherwise false
     */
    public int userAuthenticate(String username, String password, long otpcode) 
    {
        return userAuthenticate(username, password, true, otpcode);
    }

    /**
     * Called to handle username/password authentication
     * 
     * @param username
     *        The username for authentication
     * @param password
     *        The password for authentication
     * @param isMFA
     *        if isMfa should be authenticated       
     * @param otpcode
     *        The OTP code for 2 factor authentication.
     * @return True for authentication success, otherwise false
     */
    public int userAuthenticate(String username, String password, boolean isMFA, long otpcode)
    {
        boolean isAuthenticated = false;
    
        switch (getSettings().getAuthenticationType())
        {
        case ACTIVE_DIRECTORY:
            try {
                // first create a copy of the original username and another
                // that is stripped of all the Active Directory foo:
                // domain*backslash*user -> user
                // user@domain -> user
                // We'll always use the stripped version internally but
                // well try both for authentication. See bug #7951
                String originalUsername = username;
                String strippedUsername = username;
                strippedUsername = strippedUsername.replaceAll(".*\\\\", "");
                strippedUsername = strippedUsername.replaceAll("@.*", "");

                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector == null) break;

                // try the original first and then the stripped version
                isAuthenticated = directoryConnector.activeDirectoryAuthenticate(originalUsername, password);
                if (isAuthenticated == false) isAuthenticated = directoryConnector.activeDirectoryAuthenticate(strippedUsername, password);
            } catch (Exception e) {
                logger.warn("Active Directory authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case LOCAL_DIRECTORY:
            try {
		if (isMFA) {
			isAuthenticated = UvmContextFactory.context().localDirectory().authenticate(username, password, otpcode);
		} else {
			isAuthenticated = UvmContextFactory.context().localDirectory().authenticate(username, password);
		}
            } catch (Exception e) {
                logger.warn("Local Directory authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case RADIUS:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.radiusAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Radius authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case ANY_DIRCON:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.anyAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Any authentication failure", e);
                isAuthenticated = false;
            }
            break;
        default:
            logger.error("Unknown Authenticate Method: " + getSettings().getAuthenticationType());

        }

        if (!isAuthenticated) {
            logger.info("Authenticate failure: " + username + " (" + getSettings().getAuthenticationType() + ")");
            return (1);
        }

        logger.info("Authenticate success: " + username + " (" + getSettings().getAuthenticationType() + ")");
        return (0);
    }

    /**
     * Called to increment the pass count metric
     */
    public void incrementPassCount()
    {
        this.incrementMetric(OpenVpnAppImpl.STAT_PASS);
    }

    /**
     * Called to increment the connect count metric
     */
    public void incrementConnectCount()
    {
        this.incrementMetric(OpenVpnAppImpl.STAT_CONNECT);
    }

    /**
     * Called to get the list of active clients
     * 
     * @return The list of active clients
     */
    public List<OpenVpnStatusEvent> getActiveClients()
    {
        return this.openVpnMonitor.getOpenConnectionsAsEvents();
    }

    /**
     * Called to get the list of active remote servers
     * 
     * @return The list of active remote servers
     */
    public List<JSONObject> getRemoteServersStatus()
    {
        return _getRemoteServersStatus();
    }

    /**
     * Called to get a client distribution download link
     * 
     * @param clientName
     *        The name of the client to download
     * @param format
     *        The download format requested (zip, exe, ovpn, onc)
     * @return A link to the client download file
     */
    public String getClientDistributionDownloadLink(String clientName, String format)
    {
        /**
         * Find the client by that name
         */
        OpenVpnRemoteClient client = null;
        for (final OpenVpnRemoteClient iclient : this.settings.getRemoteClients()) {
            if (iclient.getName().equals(clientName)) client = iclient;
        }
        if (client == null) {
            throw new RuntimeException("Client \"" + clientName + "\" not found.");
        }

        /**
         * Generate the certs ( if they already exist it will just return )
         */
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(GENERATE_CLIENT_CERTS_SCRIPT + " \"" + client.getName() + "\"");
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(GENERATE_CLIENT_CERTS_SCRIPT + ": ");
            for (String line : lines)
                logger.info(GENERATE_CLIENT_CERTS_SCRIPT + ": " + line);
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to generate client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to generate client config");
        }

        /**
         * Return the proper link for the requested format and Generate the zip
         * and exec files
         */
        String fileName = null;
        if ("zip".equals(format)) {
            this.openVpnManager.createClientDistributionZip(settings, client);
            fileName = "config.zip";
        } else if ("ovpn".equals(format)) {
            this.openVpnManager.createClientDistributionOvpn(settings, client);
            fileName = "inline.ovpn";
        } else if ("onc".equals(format)) {
            this.openVpnManager.createClientDistributionOnc(settings, client);
            fileName = "chrome.onc";
        } else {
            throw new RuntimeException("Unknown format: " + format);
        }

        String key = "";
        String clientNameStr = "";
        try {
            clientNameStr = URLEncoder.encode(clientName, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            logger.warn("Unsupported Encoding:", e);
        }

        return "/openvpn" + "/" + fileName + "?" + "client" + "=" + clientNameStr;
    }

    /**
     * Get the client distribution upload link
     * 
     * @return The upload link
     */
    public String getClientDistributionUploadLink()
    {
        return "/openvpn" + "/uploadConfig?";
    }

    /**
     * Import a client configuration
     * 
     * @param filename
     *        The file to be imported
     */
    public void importClientConfig(String filename)
    {
        ExecManagerResult result = UvmContextFactory.context().execManager().exec(IMPORT_CLIENT_SCRIPT + " \"" + filename + "\"");

        String sitename = "siteName-" + (new Random().nextInt(10000));
        try {
            String lines[] = result.getOutput().split("\\r?\\n");
            logger.info(IMPORT_CLIENT_SCRIPT + ": ");
            for (String line : lines) {
                logger.info(IMPORT_CLIENT_SCRIPT + ": " + line);

                if (line.contains("SiteName: ")) {
                    String[] tokens = line.split(" ");
                    if (tokens.length > 1) sitename = tokens[1];
                }
            }
        } catch (Exception e) {
        }

        if (result.getResult() != 0) {
            logger.error("Failed to import client config (return code: " + result.getResult() + ")");
            throw new RuntimeException("Failed to import client config");
        }

        /**
         * Add a new server in settings, if it does not exist
         */
        OpenVpnSettings settings = getSettings();
        List<OpenVpnRemoteServer> servers = settings.getRemoteServers();
        for (OpenVpnRemoteServer server : servers) {
            if (sitename.equals(server.getName())) return;
        }
        OpenVpnRemoteServer server = new OpenVpnRemoteServer();
        server.setName(sitename);
        server.setEnabled(true);
        servers.add(server);
        settings.setRemoteServers(servers);
        setSettings(settings);

        /**
         * Restart the daemon
         */
        this.openVpnManager.configure(this.settings);
        this.openVpnManager.restart();

        return;
    }

    /**
     * Create exported networks list from current network settings
     *
     * @return a list of exported networks
     */
    private List<OpenVpnExport> getCurrentExportList()
    {
        /**
         * create a list of default exports - use all static non-WANs by default
         */
        List<OpenVpnExport> exports = new LinkedList<>();
        for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getEnabledInterfaces()) {
            if (intfSettings.getConfigType() != InterfaceSettings.ConfigType.ADDRESSED) continue;
            if (intfSettings.getV4ConfigType() != InterfaceSettings.V4ConfigType.STATIC) continue;
            if (intfSettings.getIsWan()) continue;
            if (intfSettings.getV4StaticAddress() == null || intfSettings.getV4StaticNetmask() == null) continue;

            OpenVpnExport export = new OpenVpnExport();
            export.setEnabled(true);
            export.setName(intfSettings.getName() + " " + I18nUtil.marktr("primary network"));
            export.setNetwork(new IPMaskedAddress(intfSettings.getV4StaticAddress(), intfSettings.getV4StaticNetmask()));
            exports.add(export);
        }

        return exports;
    }

    /**
     * Get the default application settings
     *
     * @return The default settings
     */
    private OpenVpnSettings getDefaultSettings()
    {
        OpenVpnSettings newSettings = new OpenVpnSettings();

        newSettings.setSiteName(UvmContextFactory.context().networkManager().getNetworkSettings().getHostName() + "-" + (new Random().nextInt(10000)));

        newSettings.setExports(getCurrentExportList());

        /**
         * create a list of default groups (just one)
         */
        List<OpenVpnGroup> groups = new LinkedList<>();
        OpenVpnGroup group = new OpenVpnGroup();
        group.setGroupId(1);
        group.setName(I18nUtil.marktr("Default Group"));
        group.setFullTunnel(false);
        group.setPushDns(true);
        group.setPushDnsSelf(true);
        groups.add(group);
        newSettings.setGroups(groups);

        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        IPMaskedAddress newAddrPool = nsmgr.getAvailableAddressSpace(IPVersion.IPv4, 0);

        newSettings.setAddressSpace(newAddrPool);


        return newSettings;
    }

    /**
     * Sanitize application settings
     * 
     * @param newSettings
     *        The settings to sanitize
     */
    private void sanitizeSettings(OpenVpnSettings newSettings)
    {
        /**
         * Set group IDs for any new groups New groups have ID of -1, set them
         * to unused IDs
         */
        int highestKnownGroupId = 1;
        for (OpenVpnGroup group : newSettings.getGroups()) {
            if (group.getGroupId() > highestKnownGroupId) highestKnownGroupId = group.getGroupId();
        }
        for (OpenVpnGroup group : newSettings.getGroups()) {
            if (group.getGroupId() < 0) {
                group.setGroupId(highestKnownGroupId + 1);
                highestKnownGroupId++;
            }
        }
    }

    /**
     * Sanity check application settings
     * 
     * @param newSettings
     *        The settings to sanity check
     */
    private void sanityCheckSettings(OpenVpnSettings newSettings)
    {
        /**
         * Verify no lists are null
         */
        if (newSettings.getGroups() == null) throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": null groups list");
        if (newSettings.getRemoteClients() == null) throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": null remote clients list");

        /**
         * Check each client. Check that it has a good name, and the mapped
         * group exist
         */
        List<IPMaskedAddress> exportedNetworks = new LinkedList<>();
        exportedNetworks.add(newSettings.getAddressSpace());
        for (OpenVpnRemoteClient client : newSettings.getRemoteClients()) {
            if (client.getName() == null || client.getName().contains(" ")) throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": Illegal client name: " + client.getName());
            boolean foundGroup = false;
            for (OpenVpnGroup group : newSettings.getGroups()) {
                if (group.getGroupId() == client.getGroupId()) foundGroup = true;
            }
            if (!foundGroup) throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": Missing Group " + client.getGroupId() + " for client: " + client.getName());

            if (client.getExport()) {
                String networks = client.getExportNetwork();
                for (String network : networks.split(",")) {
                    exportedNetworks.add(new IPMaskedAddress(network));
                }
            }
        }

        /**
         * Check that exported remote networks do not conflict with any other
         * registered addresses or other exports
         */
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();
        NetworkSpace space = null;

        for (IPMaskedAddress export : exportedNetworks) {
            space = nsmgr.isNetworkAvailable(NETSPACE_OWNER, export);
            if (space != null) {
                throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": " + export + " " + I18nUtil.marktr("conflicts with") + " " + space.ownerName + ":" + space.ownerPurpose);
            }

            for (IPMaskedAddress export2 : exportedNetworks) {
                if (export == export2) continue;
                if (export.isIntersecting(export2)) {
                    throw new RuntimeException(I18nUtil.marktr("Invalid Settings") + ": " + export + " " + I18nUtil.marktr("conflicts with address") + " " + export2);
                }
            }
        }
    }

    /**
     * Clean up files left over from clients that have been removed
     * 
     * @throws Exception
     */
    private void cleanClientSettings() throws Exception
    {
        String directory = System.getProperty("uvm.settings.dir") + "/openvpn/remote-clients";
        File file = new File(directory);
        String list[] = file.list();
        boolean found;

        if (list == null) return;
        if (list.length == 0) return;

        for (String name : list) {
            // check for a name that starts with the client prefix
            if (!name.startsWith("client-")) continue;

            String target = (directory + "/" + name);
            logger.info("Cleanup checking: " + target);

            // extract the client name from the directory name
            String clientName = name.substring(name.lastIndexOf("-") + 1);

            // check the settings to see if this is a valid client
            found = false;

            for (OpenVpnRemoteClient client : getSettings().getRemoteClients()) {
                if (!clientName.startsWith(client.getName() + ".")) continue;
                found = true;
                break;
            }

            if (found == true) continue;

            // no matching client so get rid of the file
            logger.info("Cleanup removing: " + target);
            File trash = new File(target);
            trash.delete();
        }
    }

    /**
     * Clean up downloads left over from clients that have been removed
     * 
     * @throws Exception
     */
    private void cleanClientPackages() throws Exception
    {
        String directory = "/tmp/openvpn/client-packages";
        File file = new File(directory);
        String list[] = file.list();
        boolean found;

        if (list == null) return;
        if (list.length == 0) return;

        for (String name : list) {
            String target = (directory + "/" + name);
            logger.info("Cleanup checking: " + target);

            // check the settings to see if this is a valid client
            found = false;

            for (OpenVpnRemoteClient client : getSettings().getRemoteClients()) {
                if (!name.contains("-" + client.getName() + ".")) continue;
                found = true;
                break;
            }

            if (found == true) continue;

            // no matching client so get rid of the file
            logger.info("Cleanup removing: " + target);
            File trash = new File(target);
            trash.delete();
        }
    }

    /**
     * Clean up files left over from remote servers that have been removed
     * 
     * @throws Exception
     */
    private void cleanServerSettings() throws Exception
    {
        String directory = System.getProperty("uvm.settings.dir") + "/openvpn/remote-servers";
        File file = new File(directory);
        String list[] = file.list();
        boolean found;
        boolean enabled = false;

        if (list == null) return;
        if (list.length == 0) return;

        BufferedReader br;
        for (String name : list) {
            // check for a name that ends with the server config extension
            if (!name.endsWith(".conf")) continue;

            String target = (directory + "/" + name);
            logger.info("Cleanup checking: " + target);

            // extract the server name from the config file name
            String serverName = name.substring(0, name.lastIndexOf("."));

            // check the settings to see if this is a valid server
            found = false;

            for (OpenVpnRemoteServer server : getSettings().getRemoteServers()) {
                if (!serverName.equals(server.getName())) continue;
                found = true;
                enabled = server.getEnabled();
                break;
            }

            if (found == true) {
                if (enabled == false) {
                    logger.info("Stopping client OpenVPN process for disabled openvpn@" + serverName + ".service");
                    UvmContextFactory.context().execManager().exec("systemctl stop openvpn@" + serverName + ".service");
                }
                continue;
            }

            logger.info("Stopping client OpenVPN process for removed openvpn@" + serverName + ".service");
            UvmContextFactory.context().execManager().exec("systemctl stop openvpn@" + serverName + ".service");

            // no matching server so get rid of the config file and keys
            logger.info("Cleanup removing: " + target);

            br = null;
            try {
                File trash = new File(target);
                br = new BufferedReader(new FileReader(trash));
                String[] part;
                String junk;
                String line;

                while ((line = br.readLine()) != null) {

                    if ((line.startsWith("cert ")) || (line.startsWith("key ")) || (line.startsWith("ca "))) {
                        part = line.split(" ");
                        junk = (directory + "/" + part[1]);
                        logger.info("Cleanup removing: " + junk);
                        File wipe = new File(junk);
                        wipe.delete();
                    }
                }

                trash.delete();
            } catch (Exception e) {
                logger.warn("Unable to clear settings", e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (Exception e) {
                        logger.warn("Unable to close reader", e);
                    }
                }
            }
        }
    }

    /**
     * Called to deploy our web servlet
     */
    private synchronized void deployWebApp()
    {
        if (!isWebAppDeployed) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet("/openvpn", "openvpn", true)) {
                logger.debug("Deployed openvpn web app");
            } else logger.warn("Unable to deploy openvpn web app");
        }
        isWebAppDeployed = true;
    }

    /**
     * Called to un-deploy our web servlet
     */
    private synchronized void unDeployWebApp()
    {
        if (isWebAppDeployed) {
            if (UvmContextFactory.context().tomcatManager().unloadServlet("/openvpn")) {
                logger.debug("Unloaded openvpn web app");
            } else logger.warn("Unable to unload openvpn web app");
        }
        isWebAppDeployed = false;
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
        boolean setNewSettings = false;
        String currentSiteName = this.settings.getSiteName();
        String currentExportsString = this.settings.getExports().stream().map(u -> u.toJSONString()).collect(Collectors.joining(""));

        // refresh iptables rules in case WAN config has changed
        logger.info("Network Settings have changed. Syncing new settings...");

        /**
         * if the exported networks list matches the old local network list,
         * but doesn't match the new local network list, then update the
         * openvpn exported networks list to match the new network settings
         */
        if(this.localExports.stream().map(u -> u.toJSONString()).collect(Collectors.joining("")).compareTo(currentExportsString) == 0) {
            List<OpenVpnExport> newExports = getCurrentExportList();

            if(newExports.stream().map(u -> u.toJSONString()).collect(Collectors.joining("")).compareTo(currentExportsString) != 0) {
                this.settings.setExports(newExports);
                setNewSettings = true;
            }
        }

        /**
         * if the sitename matches the old hostname (plus -XXXXX),
         * but doesn't match the new hostname (plus -XXXXX),then update
         * the sitename to match the new hostname (plus -XXXXX)
         */
        if(currentSiteName.matches("^" + this.localHostName + "-[0-9]{1,5}$")) {
            String newHostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
            if(!currentSiteName.matches("^" + newHostName + "-[0-9]{1,5}$")) {
                this.settings.setSiteName(currentSiteName.replaceAll(this.localHostName, newHostName));
                setNewSettings = true;
            }
        }

        /**
         * If we updated any openvpn settings because of a network settings change
         * then set them here.  For network settings changes that don't result in
         * a change to openvpn settings, just call configure
         */
        if(setNewSettings) {
            this.setSettings(this.settings);
        } else {
            // Several openvpn settings rely on network settings.
            // As such when the network settings change, re-sync the openvpn settings
            // They aren't critical though so don't restart the server.
            this.openVpnManager.configure(this.settings);
        }
    }

    /**
     * Called before network settings are changed
     *
     * @param settings
     *        The current network settings
     * @throws Exception
     */
    private void preNetworkSettingsEvent(NetworkSettings settings) throws Exception
    {
        this.localExports = getCurrentExportList();
        this.localHostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
    }

    /**
     * Called to get the statos of all remote servers
     * 
     * @return The status of all remote servers
     */
    private List<JSONObject> _getRemoteServersStatus()
    {
        List<JSONObject> results = new LinkedList<>();

        BufferedReader reader;
        for (OpenVpnRemoteServer server : settings.getRemoteServers()) {
            reader = null;
            try {
                JSONObject result = new JSONObject();
                File statusFile = new File("/var/run/openvpn/" + server.getName() + ".status");

                result.put("name", server.getName());
                result.put("enabled", server.getEnabled());
                result.put("connected", false);
                result.put("bytesRead", 0);
                result.put("bytesWritten", 0);

                if (!statusFile.exists()) {
                    results.add(result);
                    continue;
                }

                reader = new BufferedReader(new FileReader(statusFile));
                String currentLine;

                while ((currentLine = reader.readLine()) != null) {

                    // Look for TCP/UDP read bytes line
                    if (currentLine.matches("^TCP/UDP read bytes,.*")) {
                        String[] parts = currentLine.split(",");
                        if (parts.length < 2) {
                            logger.warn("Malformed line in openvpn status: " + currentLine);
                            continue;
                        }

                        long i;
                        try {
                            i = Long.parseLong(parts[1]);
                        } catch (Exception e) {
                            logger.warn("Malformed int in openvpn status: " + currentLine);
                            continue;
                        }

                        if (i == 0) {
                            // not connected
                            continue;
                        } else {
                            result.put("connected", true);
                            result.put("bytesRead", i);
                        }
                    }

                    // Look for TCP/UDP read bytes line
                    if (currentLine.matches("^TCP/UDP write bytes,.*")) {
                        String[] parts = currentLine.split(",");
                        if (parts.length < 2) {
                            logger.warn("Malformed line in openvpn status: " + currentLine);
                            continue;
                        }

                        long i;
                        try {
                            i = Long.parseLong(parts[1]);
                        } catch (Exception e) {
                            logger.warn("Malformed int in openvpn status: " + currentLine);
                            continue;
                        }

                        result.put("bytesWritten", i);
                    }
                }

                results.add(result);
            } catch (Exception e) {
                logger.warn("Malformed openvpn status file: " + "/var/run/openvpn/" + server.getName() + ".status", e);
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (Exception e) {
                        logger.warn("Unable to close reader", e);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Function to register all network address blocks configured in this application
     *
     * @param serverAddressSpace - An IPMaskedAddress representing the server's address space
     * @param remoteClients - A List of OpenVpnRemoteClients indicating remote clients and designated exported clients
     */
    private void updateNetworkReservations(IPMaskedAddress serverAddressSpace, List<OpenVpnRemoteClient> remoteClients)
    {
        NetspaceManager nsmgr = UvmContextFactory.context().netspaceManager();

        // start by clearing all existing registrations
        nsmgr.clearOwnerRegistrationAll(NETSPACE_OWNER);

        // add registration for the configured address pool
        nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_SERVER, serverAddressSpace);

        // add reservation for all exported networks in configured remote clients        
        for (OpenVpnRemoteClient client : remoteClients) {
            if (client.getExport()) {
                String networksCsv = client.getExportNetwork();
                for (String network : networksCsv.split(",")) {
                    if (StringUtils.isBlank(network)) continue;
                    nsmgr.registerNetworkBlock(NETSPACE_OWNER, NETSPACE_REMOTE, network);
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
    private class OpenVpnHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         * 
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "openvpn-network-settings-change-hook";
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
                networkSettingsEvent(settings);
            } catch (Exception e) {
                logger.error("Unable to reconfigure the NAT app");
            }
        }
    }

    /**
     * Callback pre hook for changes to network settings
     */
    private class OpenVpnPreHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         *
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "openvpn-pre-network-settings-change-hook";
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

            if (logger.isDebugEnabled()) logger.debug("network settings are about to change:" + settings);

            try {
                preNetworkSettingsEvent(settings);
            } catch (Exception e) {
                logger.error("Unable to reconfigure the openvpn app");
            }
        }
    }
}
