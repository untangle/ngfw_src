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
    private final Logger logger = Logger.getLogger(ShieldNodeImpl.class);

    private final EventHandler handler;
    private final SoloPipeSpec pipeSpec;
    private final SoloPipeSpec[] pipeSpecs;

    private ShieldSettings settings;

    private EventLogQuery scannedEventsQuery;
    private EventLogQuery blockedEventsQuery;

    public ShieldNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler = new EventHandler( this );

        this.pipeSpec = new SoloPipeSpec("shield", this, handler, Fitting.OCTET_STREAM, Affinity.CLIENT, SoloPipeSpec.MAX_STRENGTH - 1);
        this.pipeSpecs = new SoloPipeSpec[] { pipeSpec };
        
        this.scannedEventsQuery = new EventLogQuery(I18nUtil.marktr("Scanned Sessions"),
                                                    "SELECT * FROM reports.sessions " + 
                                                    "ORDER BY time_stamp DESC");
        this.blockedEventsQuery = new EventLogQuery(I18nUtil.marktr("Blocked Sessions"),
                                                    "SELECT * FROM reports.sessions " + 
                                                    "WHERE shield_blocked IS TRUE " +
                                                    "ORDER BY time_stamp DESC");
    }

    public void setSettings(final ShieldSettings newSettings)
    {
        /**
         * Set the Rule IDs
         */
        int idx = 0;
        for (ShieldRule rule : newSettings.getRules()) {
            rule.setRuleId(++idx);
        }

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
        return new EventLogQuery[] { this.scannedEventsQuery, this.blockedEventsQuery };
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
