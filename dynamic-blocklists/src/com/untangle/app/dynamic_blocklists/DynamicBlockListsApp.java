/**
 * $Id$
 */

package com.untangle.app.dynamic_blocklists;

import com.untangle.uvm.util.Constants;
import com.untangle.uvm.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;


import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.PipelineConnector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import com.untangle.uvm.ExecManagerResult;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.nio.file.Path;


/**
 * The DynamicBlockListsApp VPN application manages Dynamic Blocklists.
 */
public class DynamicBlockListsApp extends AppBase
{
 
    private final Logger logger = LogManager.getLogger(getClass());
    private final String SettingsDirectory = "/dynamic-blocklists/";

    private static final String CRON_FILE = "/etc/cron.d/dbl-crons";
    private static final String BLOCK_LISTS_DIR = "/etc/config/blocklists";
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
        newSettings.getConfigurations().stream()
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
     * Get the current application settings 
     * @return settings
     */
    public DynamicBlockListsSettings getSettingsV2(){
        return this.settings;
    }

    
    /**
     * Updates the dynamic block list settings based on the provided configuration.
     * This method compares the enabled state of the current settings with the new settings.
     * If the enabled state changes, it starts or stops the dynamic block lists manager accordingly.
     * Finally, it updates the settings to the new configuration.
     *
     * @param newSettings The new settings to apply. If null, no changes will be made.
     */
    public void setSettingsV2(DynamicBlockListsSettings newSettings) {
        // Return early if the new settings are null (no changes to apply)
        if (newSettings == null) {
            return;
        }
        
        // Store the enabled state of the new settings and the current settings (default to false if null)
        boolean newEnabled = newSettings.getEnabled();
        boolean currentEnabled = this.settings != null ? this.settings.getEnabled() : false;

        // Check if the enabled state has changed
        if (newEnabled != currentEnabled) {
            // If the new settings enable the app, start the dynamic block lists manager
            if (newEnabled) {
                dynamicBlockListsManager.start();
            } else {
                // If the new settings disable the app, stop the dynamic block lists manager
                dynamicBlockListsManager.stop();
            }
        }

        // Apply the new settings with the updated enabled state
        setSettings(newSettings, newEnabled);
    }

    /**
     * Set the application settings to the default settings
     * @return Default settings
     */
    public DynamicBlockListsSettings onResetDefaultsV2(){
        DynamicBlockListsSettings defaultSettings = getDefaultSettings();
        setSettings(defaultSettings, false);
        dynamicBlockListsManager.stop(); 
        return defaultSettings;
    }


    /**
        * Run the job for give configIds
        * @param configIds the list of configuration IDs to process
        * @throws IOException if an I/O error occurs during job execution
        * @throws InterruptedException if the job execution is interrupted
        * @return DynamicBlockListsSettings 
     */
    public DynamicBlockListsSettings runJobsByConfigIdsV2(LinkedList<String> configIds) throws IOException, InterruptedException {
        List<String> cronLines = Files.readAllLines(Paths.get(CRON_FILE));

        for (String configId : configIds) {
            boolean found = false;

            for (String line : cronLines) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.contains(configId)) {
                    String[] parts = line.split("\\s+", 7);
                    if (parts.length < 7) {
                        logger.error("Invalid cron line format: "+ line);
                        continue;
                    }
                    String command = parts[6];
                    logger.info("Running command for config ID " + configId + ": " + command);
                    ExecManagerResult result = UvmContextFactory.context().execManager().exec(command);
                    logger.info("Command finished with exit code result.getOutput(){} : result.getResult()  {}", result.getOutput(),result.getResult());
                    found = true;
                }
            }
            if (!found) {
                logger.info("No cron job found for config ID " + configId);
            }
        }
        return getSettingsV2();
    }


    /**
     * Get the ip lists for specific block-list
     * @param confId
     * @return String
     */
    public String exportCsvV2(String confId) {
        String filename = "dynamic_ip_addresses_list_" + confId + ".txt";
        Path filePath = Paths.get(BLOCK_LISTS_DIR, filename);

        try {
            return Files.readString(filePath);
        } catch (IOException e) {
            logger.error("Error reading file: " + e.getMessage());
            return "";
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
     * Generates a status report for each configured Dynamic Block List.
     *
     * @return JSONArray representing the status of all configured dynamic block lists.
     */
    public JSONArray status() {
        JSONArray statusArray = new JSONArray();

        if (this.settings == null || this.settings.getConfigurations() == null) {
            return statusArray;
        }

        for (DynamicBlockList item : this.settings.getConfigurations()) {
            try {
                //send status only for the entry update by cron job
                if (item.getLastUpdated() != 0) {
                    JSONObject obj = new JSONObject();
                    obj.put("uuid", item.getId());
                    obj.put("last_updated_time", item.getLastUpdated());
                    obj.put("num_entries", item.getCount());
                    obj.put("status", true); // Since lastUpdated != 0, status is always true
                    statusArray.put(obj);
                }
            } catch (JSONException e) {
                logger.warn("Failed to build status object for ID: {}", item.getId(), e);
            }
        }

        return statusArray;
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
        LinkedList<DynamicBlockList> list = new LinkedList<>();

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

        settings.setConfigurations(list);
        settings.setEnabled(false);
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
