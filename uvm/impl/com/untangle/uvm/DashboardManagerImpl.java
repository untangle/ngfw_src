/**
 * $Id: DashboardManagerImpl.java,v 1.00 2015/11/10 14:31:00 dmorris Exp $
 */
package com.untangle.uvm;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.node.reports.EventEntry;
import com.untangle.node.reports.ReportEntry;
import com.untangle.node.reports.ReportsApp;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.DashboardSettings;
import com.untangle.uvm.node.NodeSettings.NodeState;


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

    public List<DashboardWidgetInfo> getAvailableWidgets() {
        List<DashboardWidgetInfo> activeWidgets = new LinkedList<DashboardWidgetInfo>();
        List<DashboardWidgetSettings> allWidgets = getSettings().getWidgets();
        
        ReportsApp reportsApp =  (ReportsApp)UvmContextFactory.context().nodeManager().node("untangle-node-reports");
        boolean reportingEnabled = reportsApp != null && NodeState.RUNNING.equals(reportsApp.getRunState());
        
        Map<String, ReportEntry> reportsMap = null;
        Map<String, EventEntry> eventsMap = null;
        Map<String, String> unavailableApplicationsMap = null;
        if(reportingEnabled) {
            reportsMap = new HashMap<String, ReportEntry>();
            List<ReportEntry> reportEntries = reportsApp.getSettings().getReportEntries();
            for(ReportEntry entry : reportEntries) {
                reportsMap.put(entry.getUniqueId(), entry);
            }
            
            eventsMap = new HashMap<String, EventEntry>();
            List<EventEntry> eventEntries = reportsApp.getSettings().getEventEntries();
            for(EventEntry entry : eventEntries) {
                eventsMap.put(entry.getUniqueId(), entry);
            }
            
            unavailableApplicationsMap = reportsApp.getReportsManager().getUnavailableApplicationsMap();
        }
        for(DashboardWidgetSettings widgetSettings: allWidgets) {
            if(!"ReportEntry".equals(widgetSettings.getType()) && !"EventEntry".equals(widgetSettings.getType())) {
                activeWidgets.add(new DashboardWidgetInfo(widgetSettings));
            } else {
                if(reportingEnabled) {
                    if("ReportEntry".equals(widgetSettings.getType())) {
                        ReportEntry entry  = reportsMap.get(widgetSettings.getEntryId());
                        if(entry != null && !unavailableApplicationsMap.containsKey(entry.getCategory())) {
                            DashboardWidgetInfo widgetInfo = new DashboardWidgetInfo(widgetSettings);
                            widgetInfo.setReportEntry(entry);
                            activeWidgets.add(widgetInfo);
                        }
                    }
                    
                    if("EventEntry".equals(widgetSettings.getType())) {
                        EventEntry entry  = eventsMap.get(widgetSettings.getEntryId());
                        if(entry != null && !unavailableApplicationsMap.containsKey(entry.getCategory())) {
                            DashboardWidgetInfo widgetInfo = new DashboardWidgetInfo(widgetSettings);
                            widgetInfo.setEventEntry(entry);
                            activeWidgets.add(widgetInfo);
                        }
                    }
                    
                }
            }
        }
        return activeWidgets;
    }

    private DashboardSettings defaultSettings()
    {
        DashboardWidgetSettings widgetSettings;
        LinkedList<DashboardWidgetSettings> widgets = new LinkedList<DashboardWidgetSettings>();

        widgets.add( new DashboardWidgetSettings("Information"));
        widgets.add( new DashboardWidgetSettings("Server"));
        widgets.add( new DashboardWidgetSettings("Sessions"));
        widgets.add( new DashboardWidgetSettings("HostsDevices"));
        widgets.add( new DashboardWidgetSettings("Hardware"));
        widgets.add( new DashboardWidgetSettings("Memory"));

        widgetSettings = new DashboardWidgetSettings("ReportEntry");
        widgetSettings.setRefreshIntervalSec(60);
        widgetSettings.setEntryId("network-8bTqxKxxUK");
        
        widgets.add( widgetSettings);

        DashboardSettings newSettings = new DashboardSettings();
        newSettings.setWidgets(widgets);
        return newSettings;
    }
}
