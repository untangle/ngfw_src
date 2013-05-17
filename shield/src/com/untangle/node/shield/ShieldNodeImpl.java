/*
 * $Id$
 */
package com.untangle.node.shield;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;

public class ShieldNodeImpl extends NodeBase  implements ShieldNode
{
    protected static final String STAT_ACCEPT = "accept";
    protected static final String STAT_LIMIT = "limit";
    protected static final String STAT_DROP = "drop";
    protected static final String STAT_REJECT = "reject";

    private static final int CREATE_DATE_IDX =  1;
    private static final int CLIENT_ADDR_IDX =  2;
    private static final int CLIENT_INTF_IDX =  3;
    private static final int REPUTATION_IDX  =  4;
    private static final int LIMITED_IDX     =  5;
    private static final int DROPPED_IDX     =  6;
    private static final int REJECTED_IDX    =  7;

    private final Logger logger = Logger.getLogger(ShieldNodeImpl.class);

    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private ShieldSettings settings;

    private EventLogQuery eventQuery;

    public ShieldNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler = new EventHandler( this );

        this.pipeSpec = new SoloPipeSpec("shield", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 1);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
        
        this.eventQuery = new EventLogQuery(I18nUtil.marktr("Events"),"SELECT * FROM reports.shield_rejection_totals ORDER BY time_stamp DESC");                                                 
        this.addMetric(new NodeMetric(STAT_ACCEPT, I18nUtil.marktr("Sessions accepted")));
        this.addMetric(new NodeMetric(STAT_LIMIT, I18nUtil.marktr("Sessions limited")));
        this.addMetric(new NodeMetric(STAT_DROP, I18nUtil.marktr("Sessions dropped")));
        this.addMetric(new NodeMetric(STAT_REJECT, I18nUtil.marktr("Sessions rejected")));
    }

    public void setSettings(final ShieldSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(ShieldSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-shield/" + "settings_"  + nodeID, newSettings);
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

    public ShieldSettings getSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + this.getRunState() );

        return settings;
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return this.pipeSpecs;
    }

    public void initializeSettings()
    {
        ShieldSettings settings = new ShieldSettings();
        logger.info("Initializing Settings...");
        setSettings( settings);
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.eventQuery };
    }
    
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        ShieldSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-shield/" + "settings_" + nodeID;

        try {
            readSettings = settingsManager.load( ShieldSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }
        
        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            ShieldSettings settings = new ShieldSettings();

            this.initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }
}
