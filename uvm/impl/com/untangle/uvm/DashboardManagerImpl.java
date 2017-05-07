/**
 * $Id: DashboardManagerImpl.java,v 1.00 2015/11/10 14:31:00 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import org.apache.log4j.Logger;


/**
 * The Manager for the dashboard
 */
public class DashboardManagerImpl implements DashboardManager
{
    private static final Logger logger = Logger.getLogger(DashboardManagerImpl.class);

    private DashboardSettings settings = null; 
    
    protected DashboardManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        DashboardSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "dashboard.js";

        try {
            readSettings = settingsManager.load( DashboardSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");
            this.setSettings(defaultSettings());
        }
        else {
            this.settings = readSettings;
            logger.debug("Loading Settings: " + this.settings.toJSONString());
        }

        /**
         * 12.2 Conversion
         */
        if ( settings.getVersion() == null || settings.getVersion() < 2 ) {
            logger.info("Migrating dashboard settings to v2...");

            settings.setVersion(2);

            List<DashboardWidgetSettings> widgets = settings.getWidgets();
            for ( ListIterator<DashboardWidgetSettings> iterator = widgets.listIterator() ; iterator.hasNext() ; ) {
                DashboardWidgetSettings widget = iterator.next();

                // change Events widgets to Report widgets
                if ( "EventEntry".equals(widget.getType()) )
                    widget.setType("ReportEntry");

                // Hardware widget is gone, replace with Resources
                if ( "Hardware".equals(widget.getType()) ) {
                    iterator.remove();
                    iterator.add( new DashboardWidgetSettings("Resources") );
                }
                // Network widget is gone, 
                if ( "Network".equals(widget.getType()) ) {
                    iterator.remove();
                    iterator.add( new DashboardWidgetSettings("NetworkInformation") );
                    iterator.add( new DashboardWidgetSettings("NetworkLayout") );
                    iterator.add( new DashboardWidgetSettings("MapDistribution") );
                }
                if ( "HostsDevices".equals(widget.getType()) )
                    iterator.remove();
                if ( "Sessions".equals(widget.getType()) )
                    iterator.remove();
                if ( "Memory".equals(widget.getType()) )
                    iterator.remove();
                if ( "Server".equals(widget.getType()) )
                    iterator.remove();
            }

            DashboardWidgetSettings widgetSettings;

            widgetSettings = new DashboardWidgetSettings("ReportEntry");
            widgetSettings.setRefreshIntervalSec(60);
            widgetSettings.setTimeframe(3600);
            widgetSettings.setEntryId("bandwidth-control-BVOy539ahO"); // Bandwidth Control - Top Hostnames Usage
            settings.getWidgets().add( widgetSettings);

            widgetSettings = new DashboardWidgetSettings("ReportEntry");
            widgetSettings.setRefreshIntervalSec(60);
            widgetSettings.setTimeframe(3600);
            widgetSettings.setEntryId("application-control-OAI5zmhxOM"); // Application Control - Top Applications Usage
            settings.getWidgets().add( widgetSettings);
            
            this.setSettings( settings );
            logger.info("Migrating dashboard settings to v2... done.");
        }

        logger.info("Initialized DashboardManager");
    }

    public DashboardSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(final DashboardSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "dashboard.js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}
    }

    public void resetSettingsToDefault()
    {
        setSettings( defaultSettings() );
    }
    
    private DashboardSettings defaultSettings()
    {
        DashboardWidgetSettings widgetSettings;
        LinkedList<DashboardWidgetSettings> widgets = new LinkedList<DashboardWidgetSettings>();

        widgets.add( new DashboardWidgetSettings("Information"));
        widgets.add( new DashboardWidgetSettings("Resources"));
        widgets.add( new DashboardWidgetSettings("CPULoad"));
        widgets.add( new DashboardWidgetSettings("NetworkInformation"));
        widgets.add( new DashboardWidgetSettings("NetworkLayout"));
        widgets.add( new DashboardWidgetSettings("MapDistribution"));
        
        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("network-2nx8FA4VCB"); // Network - Interface Usage
        widgets.add( widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("web-filter-h0jelsttGp"); // Web Filter - Web Usage
        widgets.add( widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("web-filter-RxrICRqf6Bg"); // Web Filter - Top Categories
        widgets.add( widgetSettings);
        
        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("bandwidth-control-CRntw4hkHn"); // Bandwidth Control - Top Hostnames (by total bytes)
        widgets.add( widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("bandwidth-control-BVOy539ahO"); // Bandwidth Control - Top Hostnames Usage
        widgets.add( widgetSettings);
        
        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("application-control-lBxH9QZ8A8"); // Application Control - Top Applications (by size)
        widgets.add( widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("application-control-OAI5zmhxOM"); // Application Control - Top Applications Usage
        widgets.add( widgetSettings);
    
        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600);
        widgetSettings.setEntryId("network-biCUnFjuBr"); // Network - Session Per Minute
        widgets.add( widgetSettings);

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setTimeframe(3600*24);
        widgetSettings.setDisplayColumns(new String[]{"time_stamp","description","summary_text"});
        widgetSettings.setEntryId("reports-8XL9cbqQa9"); // Reports - Alert Events
        widgets.add( widgetSettings);
        
        DashboardSettings newSettings = new DashboardSettings();
        newSettings.setVersion(2);
        newSettings.setWidgets(widgets);
        return newSettings;
    }
}
