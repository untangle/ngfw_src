/**
 * $Id$
 */
package com.untangle.app.application_control_lite;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * The Application Control Lite application
 * It uses signatures (regex) on network traffic to identify applications
 */
public class ApplicationControlLiteApp extends AppBase 
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_DETECT = "detect";
    private static final String STAT_BLOCK = "block";

    private final EventHandler handler = new EventHandler( this );

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final Logger logger = Logger.getLogger(ApplicationControlLiteApp.class);

    private ApplicationControlLiteSettings appSettings = null;

    /**
     * Instantiate an application control lite app with the provided appSettings and appProperties
     * @param appSettings the application settings
     * @param appProperties the applicaiton properties
     */
    public ApplicationControlLiteApp( AppSettings appSettings, AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.addMetric(new AppMetric(STAT_SCAN, I18nUtil.marktr("Chunks scanned")));
        this.addMetric(new AppMetric(STAT_DETECT, I18nUtil.marktr("Sessions logged")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        
        this.connector = UvmContextFactory.context().pipelineFoundry().create("application-control-lite", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 0, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    /**
     * Get the current settings
     * @return the current settings
     */
    public ApplicationControlLiteSettings getSettings()
    {
        return this.appSettings;
    }

    /**
     * Set the settings
     * @param newSettings The new settings
     */
    public void setSettings(final ApplicationControlLiteSettings newSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/application-control-lite/settings_" + appID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save ApplicationControlLite settings", exn);
            return;
        }

        this.appSettings = newSettings;
        
        reconfigure();
    }

    /**
     * Get the pipeline connectors for this ad blocker
     * @return the pipelineconnectors array
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * The postInit() hook
     * This loads the settings from file or initializes them if necessary
     */
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        String appID = this.getAppSettings().getId().toString();

        String settingsFile = System.getProperty("uvm.settings.dir") + "/application-control-lite/settings_" + appID + ".js";
        ApplicationControlLiteSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( ApplicationControlLiteSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read app settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                ApplicationControlLiteSettings settings = new ApplicationControlLiteSettings();
                settings.setPatterns(new LinkedList<ApplicationControlLitePattern>());
                setSettings(settings);
            } else {
                appSettings = readSettings;
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply app settings", exn);
        }
    }

    /**
     * This initalizes all the in-memory data structures for the current settings.
     * It should be called after changing and/or saving settings.
     */
    public void reconfigure()
    {
        HashSet<ApplicationControlLitePattern> enabledPatternsSet = new HashSet<>();

        logger.info("Reconfigure()");

        if (appSettings == null) {
            throw new RuntimeException("Failed to get ApplicationControlLite settings: " + appSettings);
        }

        LinkedList<ApplicationControlLitePattern> curPatterns = appSettings.getPatterns();
        if (curPatterns == null)
            logger.error("NULL pattern list. Continuing anyway...");
        else {
            for(int x = 0;x < curPatterns.size();x++) {
                ApplicationControlLitePattern pat = curPatterns.get(x);

                if ( pat.getLog() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsSet.add(pat);
                }
            }
        }

        handler.setPatternSet(enabledPatternsSet);
        handler.setByteLimit(appSettings.getByteLimit());
        handler.setChunkLimit(appSettings.getChunkLimit());
        handler.setStripZeros(appSettings.isStripZeros());
    }

    /**
     * Increment the scan count metric
     */
    void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    /**
     * Increment the block count metric
     */
    void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    /**
     * Increment the detect count metric
     */
    void incrementDetectCount()
    {
        this.incrementMetric(STAT_DETECT);
    }
}
