/**
 * $Id: DashboardManagerImpl.java,v 1.00 2015/11/10 14:31:00 dmorris Exp $
 */

package com.untangle.uvm;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

/**
 * The Manager for the dashboard
 */
public class DashboardManagerImpl implements DashboardManager
{
    private static final Logger logger = Logger.getLogger(DashboardManagerImpl.class);

    private DashboardSettings settings = null;

    /**
     * Constructor
     */
    protected DashboardManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        DashboardSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "dashboard.js";

        try {
            readSettings = settingsManager.load(DashboardSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings());
        } else {
            this.settings = readSettings;
            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * 13.2 Conversion
         */
        if (settings.getVersion() == null || settings.getVersion() < 3) {
            logger.info("Migrating dashboard settings to v3...");

            settings.setVersion(3);

            try {
                List<DashboardWidgetSettings> widgets = settings.getWidgets();
                widgets.add(5, new DashboardWidgetSettings("PolicyOverview"));
                widgets.add(new DashboardWidgetSettings("Notifications"));
            } catch (Exception e) {
                logger.warn("Exception converting to v3", e);
            }

            this.setSettings(settings);
            logger.info("Migrating dashboard settings to v3... done.");
        }

        logger.info("Initialized DashboardManager");
    }

    /**
     * Get the dashboard settings
     * 
     * @return The dashboard settings
     */
    public DashboardSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the dashboard settings
     * 
     * @param newSettings
     *        The new dashboard settings
     */
    public void setSettings(final DashboardSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "dashboard.js", newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }
    }

    /**
     * Reset the settings to default
     */
    public void resetSettingsToDefault()
    {
        setSettings(defaultSettings());
    }

    /**
     * Create the default settings
     * 
     * @return The default settings
     */
    private DashboardSettings defaultSettings()
    {
        DashboardWidgetSettings widgetSettings;
        LinkedList<DashboardWidgetSettings> widgets = new LinkedList<>();

        widgets.add(new DashboardWidgetSettings("Information"));
        widgets.add(new DashboardWidgetSettings("Resources"));
        widgets.add(new DashboardWidgetSettings("CPULoad"));
        widgets.add(new DashboardWidgetSettings("NetworkInformation"));
        widgets.add(new DashboardWidgetSettings("NetworkLayout"));
        widgets.add(new DashboardWidgetSettings("MapDistribution"));
        widgets.add(new DashboardWidgetSettings("PolicyOverview"));

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("network-2nx8FA4VCB"); // Network - Interface Usage
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("web-filter-h0jelsttGp"); // Web Filter - Web Usage
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("web-filter-RxrICRqf6Bg"); // Web Filter - Top Categories
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("bandwidth-control-CRntw4hkHn"); // Bandwidth Control - Top Hostnames (by total bytes)
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("bandwidth-control-BVOy539ahO"); // Bandwidth Control - Top Hostnames Usage
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("application-control-lBxH9QZ8A8"); // Application Control - Top Applications (by size)
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("application-control-OAI5zmhxOM"); // Application Control - Top Applications Usage
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("network-biCUnFjuBr"); // Network - Session Per Minute
        widgets.add(widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600 * 24);
        widgetSettings.setDisplayColumns(new String[] { "time_stamp", "description", "summary_text" });
        widgetSettings.setEntryId("reports-8XL9cbqQa9"); // Reports - Alert Events
        widgets.add(widgetSettings);

        widgets.add(new DashboardWidgetSettings("Notifications"));

        DashboardSettings newSettings = new DashboardSettings();
        newSettings.setVersion(3);
        newSettings.setWidgets(widgets);
        return newSettings;
    }
}
