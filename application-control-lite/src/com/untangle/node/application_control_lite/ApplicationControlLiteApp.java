/*
 * $Id$
 */
package com.untangle.node.application_control_lite;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.NodeProperties;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

public class ApplicationControlLiteApp extends NodeBase 
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_DETECT = "detect";
    private static final String STAT_BLOCK = "block";

    private final EventHandler handler = new EventHandler( this );

    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private final Logger logger = Logger.getLogger(ApplicationControlLiteApp.class);

    private ApplicationControlLiteSettings nodeSettings = null;

    // constructors -----------------------------------------------------------

    public ApplicationControlLiteApp( NodeSettings nodeSettings, NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Chunks scanned")));
        this.addMetric(new NodeMetric(STAT_DETECT, I18nUtil.marktr("Sessions logged")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
        
        this.connector = UvmContextFactory.context().pipelineFoundry().create("application-control-lite", this, null, handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 0, false);
        this.connectors = new PipelineConnector[] { connector };
    }

    // ApplicationControlLite methods ----------------------------------------------------

    public ApplicationControlLiteSettings getSettings()
    {
        if( this.nodeSettings == null )
            logger.error("Settings not yet initialized. State: " + this.getRunState() );
        return this.nodeSettings;
    }

    public void setSettings(final ApplicationControlLiteSettings newSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/application-control-lite/settings_" + nodeID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save ApplicationControlLite settings", exn);
            return;
        }

        this.nodeSettings = newSettings;
        
        reconfigure();
    }

    public int getPatternsTotal()
    {
        return(nodeSettings.getPatterns().size());
    }

    public int getPatternsLogged()
    {
        LinkedList<ApplicationControlLitePattern>list = nodeSettings.getPatterns();
        int count = 0;
        
            for(int x = 0;x < list.size();x++)
            {
            ApplicationControlLitePattern curr = list.get(x);
            if (curr.getLog()) count++;
            }
        
        return(count);
    }

    public int getPatternsBlocked()
    {
        LinkedList<ApplicationControlLitePattern>list = nodeSettings.getPatterns();
        int count = 0;
        
            for(int x = 0;x < list.size();x++)
            {
            ApplicationControlLitePattern curr = list.get(x);
            if (curr.isBlocked()) count++;
            }
        
        return(count);
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /*
     * First time initialization
     */
    @Override
    public void initializeSettings()
    {
        ApplicationControlLiteSettings settings = new ApplicationControlLiteSettings();
        settings.setPatterns(new LinkedList<ApplicationControlLitePattern>());
        setSettings(settings);
    }

    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();

        String nodeID = this.getNodeSettings().getId().toString();

        String settingsFile = System.getProperty("uvm.settings.dir") + "/application-control-lite/settings_" + nodeID + ".js";
        ApplicationControlLiteSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( ApplicationControlLiteSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSettings();
            } else {
                nodeSettings = readSettings;
                reconfigure();
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    public void reconfigure()
    {
        HashSet<ApplicationControlLitePattern> enabledPatternsSet = new HashSet<ApplicationControlLitePattern>();

        logger.info("Reconfigure()");

        if (nodeSettings == null) {
            throw new RuntimeException("Failed to get ApplicationControlLite settings: " + nodeSettings);
        }

        LinkedList<ApplicationControlLitePattern> curPatterns = nodeSettings.getPatterns();
        if (curPatterns == null)
            logger.error("NULL pattern list. Continuing anyway...");
        else {
            for(int x = 0;x < curPatterns.size();x++) {
                ApplicationControlLitePattern pat = curPatterns.get(x);

                if ( pat.getLog() || pat.getAlert() || pat.isBlocked() ) {
                    logger.info("Matching on pattern \"" + pat.getProtocol() + "\"");
                    enabledPatternsSet.add(pat);
                }
            }
        }

        handler.patternSet(enabledPatternsSet);
        handler.byteLimit(nodeSettings.getByteLimit());
        handler.chunkLimit(nodeSettings.getChunkLimit());
        handler.stripZeros(nodeSettings.isStripZeros());
    }

    void incrementScanCount()
    {
        this.incrementMetric(STAT_SCAN);
    }

    void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    void incrementDetectCount()
    {
        this.incrementMetric(STAT_DETECT);
    }
}
