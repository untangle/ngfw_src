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
import com.untangle.uvm.node.SqlCondition;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.node.EventEntry;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;

public class ShieldNodeImpl extends NodeBase  implements ShieldNode
{
    private final Logger logger = Logger.getLogger(ShieldNodeImpl.class);

    private final EventHandler handler;
    private final PipelineConnector connector;
    private final PipelineConnector[] connectors;

    private ShieldSettings settings;

    private EventEntry scannedEventsQuery;
    private EventEntry blockedEventsQuery;

    public ShieldNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        this.handler = new EventHandler( this );

        this.connector = UvmContextFactory.context().pipelineFoundry().create("shield", this, null, this.handler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, 32 - 1);
        this.connectors = new PipelineConnector[] { connector };
        
        this.scannedEventsQuery = new EventEntry(I18nUtil.marktr("Scanned Sessions"), "sessions", new SqlCondition[]{});
        this.blockedEventsQuery = new EventEntry(I18nUtil.marktr("Blocked Sessions"), "sessions", new SqlCondition[]{ new SqlCondition("shield_blocked","is","true") });
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
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "untangle-node-shield/" + "settings_"  + nodeID + ".js", newSettings );
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
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public void initializeSettings()
    {
        ShieldSettings settings = new ShieldSettings();
        logger.info("Initializing Settings...");
        setSettings( settings);
    }

    public EventEntry[] getEventQueries()
    {
        return new EventEntry[] { this.scannedEventsQuery, this.blockedEventsQuery };
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        ShieldSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-shield/" + "settings_" + nodeID + ".js";

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
