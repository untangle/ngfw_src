/**
 * $Id$
 */

package com.untangle.app.dynamic_blocklists;

import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;


import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;


/**
 * The DynamicBlockListsApp VPN application manages Dynamic Blocklists.
 */
public class DynamicBlockListsApp extends AppBase
{
 
    private final Logger logger = LogManager.getLogger(getClass());
    private final String SettingsDirectory = "/dynamic-blocklists/";

    private final PipelineConnector[] connectors = new PipelineConnector[] {};

    private DynamicBlockListsManager dynamicBlockListsManager;
    private DynamicBlockListsSettings settings = null;

    public static final String REGEX_89AB = "[89ab]";
    public static final String PARSING_REGEX_1 = "^\\S{2,256}";
    public static final String PARSING_REGEX_2 = "((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)+|(?:[a-f0-9:]+:+)+(?:[a-f0-9](?:(::)?))+)(?:\\/{1}\\d+|-((?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)+|(?:[a-f0-9:]+:+)+(?:[a-f0-9](?:(::)?))+))?";

    public enum DBLPollingUnit {
        MINUTES("Minutes"),
        HOURS("Hours"),
        DAYS("Days");

        private final String value;

        /**
         * Constructor
         * @param value value of enum
         */
        DBLPollingUnit(String value) { this.value = value; }

        /**
         * returns the string value of enum
         * @return
         */
        public String getValue() { return value; }
    }

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public DynamicBlockListsApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
        //Manager for managing ipset logic
        this.dynamicBlockListsManager = new  DynamicBlockListsManager(this);

    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public DynamicBlockListsSettings getSettings() { return settings; }

    /**
     * Return the settings filename
     * @return String of filename
     */
    public String getSettingsFilename() {
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
    public void setSettings(final DynamicBlockListsSettings newSettings, boolean restart) {
        logger.info("Saving the settings. Restart: {}", restart);
        // Set id for new blocklists
        newSettings.getDynamicBlockList().stream()
                .filter(blockList -> StringUtil.isEmpty(blockList.getId()))
                .forEach(blockList -> {
                    blockList.setId(generateUniqueId());
                });

        // Save the settings
        try {
            UvmContextFactory.context().settingsManager().save( this.getSettingsFilename(), newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        // Change current settings
        this.settings = newSettings;

        // reconfigure only if settings have changed
        if (restart)
            dynamicBlockListsManager.configure();
        try {
            if(logger.isDebugEnabled())
                logger.debug("New Settings: \n{}", new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
            logger.error("Exception while logging new settings ", e);
        }
    }

    /**
     * Get the list of pipeline connectors
     * 
     * @return List of pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors() { return this.connectors; }

    /**
     * Called after the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition) {
        dynamicBlockListsManager.start();
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition) {

    }

    /**
     * Called before the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStop(boolean isPermanentTransition) {
        dynamicBlockListsManager.stop();
    }

    /**
     * Called after application initialization
     */
    @Override
    protected void postInit() {
        // Load the settings from settings file
        DynamicBlockListsSettings readSettings = loadSettings();

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.initializeSettings();
        }
    }

    /**
     * load the settings from settings file
     * 
     * @return DynamicBlockListsSettings loaded settings
     */
    public DynamicBlockListsSettings loadSettings() {
        logger.info("Loading Settings...");
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        DynamicBlockListsSettings readSettings = null;
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/dynamic-blocklists/" + "settings_" + appID + ".js";

        try {
            readSettings = settingsManager.load(DynamicBlockListsSettings.class, settingsFilename);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        if(null != readSettings)
            this.settings = readSettings;
        return readSettings;
    }

    /**
     * Called to uninitialize application settings
     */
    @Override
    protected void uninstall() {

    }

    /**
     * Called to initialize application settings
     */
    public void initializeSettings() {
        DynamicBlockListsSettings settings = getDefaultSettings();
        setSettings(settings, true);
    }

    /**
     * Create default application settings
     * 
     * @return Default settings
     */
    private DynamicBlockListsSettings getDefaultSettings()
    {
        logger.info("Creating the default settings...");

        DynamicBlockListsSettings settings = new DynamicBlockListsSettings();
        List<DynamicBlockList> list = new LinkedList<>();

        DynamicBlockList dynamicBlockList = new DynamicBlockList();
        dynamicBlockList.setId(generateUniqueId());
        dynamicBlockList.setEnabled(false);
        dynamicBlockList.setName("Emerging Threats");
        dynamicBlockList.setSource("http://opendbl.net/lists/etknown.list");
        dynamicBlockList.setParsingMethod(PARSING_REGEX_1);
        dynamicBlockList.setPollingTime(30);
        dynamicBlockList.setPollingUnit(DBLPollingUnit.MINUTES.getValue());
        dynamicBlockList.setSkipCertCheck(false);
        dynamicBlockList.setType("IPList");
        list.add(dynamicBlockList);

        dynamicBlockList = new DynamicBlockList();
        dynamicBlockList.setId(generateUniqueId());
        dynamicBlockList.setEnabled(false);
        dynamicBlockList.setName("DShield Blocklist");
        dynamicBlockList.setSource("http://opendbl.net/lists/dshield.list");
        dynamicBlockList.setParsingMethod(PARSING_REGEX_2);
        dynamicBlockList.setPollingTime(1);
        dynamicBlockList.setPollingUnit(DBLPollingUnit.HOURS.getValue());
        dynamicBlockList.setSkipCertCheck(false);
        dynamicBlockList.setType("IPList");
        list.add(dynamicBlockList);

        settings.setDynamicBlockList(list);
        return settings;
    }

    /**
     * Creates and return unique id
     * @return unique id
     */
    private String generateUniqueId() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString()
                .replaceFirst(Constants.FOUR, Constants.SEVEN)
                .replaceFirst(REGEX_89AB, Constants.SEVEN)
                .replace(Constants.HYPHEN, StringUtils.EMPTY)
                .substring(0,28);
    }
}
