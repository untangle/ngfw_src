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
import java.util.Iterator;
import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.AppSettings;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.PipelineConnector;

public class ShieldApp extends NodeBase
{
    private final Logger logger = Logger.getLogger(ShieldApp.class);

    private static final String SHIELD_RULES_FILE = "/etc/untangle-netd/iptables-rules.d/600-shield";

    private final PipelineConnector[] connectors;

    private ShieldSettings settings;

    public ShieldApp( com.untangle.uvm.node.AppSettings appSettings, com.untangle.uvm.node.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        this.connectors = new PipelineConnector[] { };
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
        String nodeID = this.getAppSettings().getId().toString();
        try {
            settingsManager.save( System.getProperty("uvm.settings.dir") + "/" + "shield/" + "settings_"  + nodeID + ".js", newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Sync the settings to the system
         */
        syncToSystem( true );
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
        settings.setVersion( Integer.valueOf(1) ); /* Current version */
        setSettings( settings );
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        ShieldSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/shield/" + "settings_" + nodeID + ".js";

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

        if ( settings.getVersion() == null || settings.getVersion() < 1 ) {
            convertSettingsToV1( settings );
            setSettings( settings );
        }

        /**
         * If the settings file date is newer than the system files, re-sync them
         */
        if ( ! UvmContextFactory.context().isDevel() ) {
            File settingsFile = new File( settingsFileName );
            File interfacesFile = new File( SHIELD_RULES_FILE );
            if (settingsFile.lastModified() > interfacesFile.lastModified() ) {
                logger.warn("Settings file newer than rules files, Syncing...");
                syncToSystem( true );
            }
        }
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        if ( isPermanentTransition ) {
            // if its a permanent transiiton - disable the shield permanently
            syncToSystem( false );
        }
    }

    @Override
    protected void postDestroy()
    {
        syncToSystem( false );
    }

    private void syncToSystem( boolean enabled )
    {
        /**
         * First we write a new SHIELD_RULES_FILE iptables script with the current settings
         */
        String nodeID = this.getAppSettings().getId().toString();
        String settingsFilename = System.getProperty("uvm.settings.dir") + "/" + "shield/" + "settings_"  + nodeID + ".js";
        String scriptFilename = System.getProperty("uvm.bin.dir") + "/shield-sync-settings.py";
        String networkSettingFilename = System.getProperty("uvm.settings.dir") + "/" + "untangle-vm/" + "network.js";
        String cmd = scriptFilename + " -f " + settingsFilename + " -v -n " + networkSettingFilename;
        if ( !enabled || this.settings.isShieldEnabled() != true )
            cmd += " -d"; // disable
        String output = UvmContextFactory.context().execManager().execOutput(cmd);
        String lines[] = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info("Sync Settings: " + line);

        /**
         * Run the iptables script
         */
        output = UvmContextFactory.context().execManager().execOutput(SHIELD_RULES_FILE);
        lines = output.split("\\r?\\n");
        for ( String line : lines )
            logger.info(SHIELD_RULES_FILE + ": " + line);
    }

    private void convertSettingsToV1( ShieldSettings settings )
    {
        logger.warn("Converting Shield settings from to V1...");

        settings.setVersion(1);

        if (settings.getRules() != null ) {

            /**
             * Remove any rules with unsupported conditions
             */
            for (Iterator<ShieldRule> it = settings.getRules().iterator(); it.hasNext() ;) {
                ShieldRule rule = it.next();

                for ( ShieldRuleCondition condition : rule.getConditions() ) {
                    boolean removed = false;
                    switch( condition.getConditionType() ) {
                    case USERNAME: removed = true; it.remove(); break;
                    case CLIENT_HOSTNAME: removed = true; it.remove(); break;
                    case SERVER_HOSTNAME: removed = true; it.remove(); break;
                    case SRC_MAC: removed = true; it.remove(); break;
                    case DST_MAC: removed = true; it.remove(); break;
                    case CLIENT_MAC_VENDOR: removed = true; it.remove(); break;
                    case SERVER_MAC_VENDOR: removed = true; it.remove(); break;
                    case CLIENT_IN_PENALTY_BOX: removed = true; it.remove(); break;
                    case SERVER_IN_PENALTY_BOX: removed = true; it.remove(); break;
                    case CLIENT_HAS_NO_QUOTA: removed = true; it.remove(); break;
                    case SERVER_HAS_NO_QUOTA: removed = true; it.remove(); break;
                    case CLIENT_QUOTA_EXCEEDED: removed = true; it.remove(); break;
                    case SERVER_QUOTA_EXCEEDED: removed = true; it.remove(); break;
                    case DIRECTORY_CONNECTOR_GROUP: removed = true; it.remove(); break;
                    case HTTP_USER_AGENT: removed = true; it.remove(); break;
                    case HTTP_USER_AGENT_OS: removed = true; it.remove(); break;
                    case CLIENT_COUNTRY: removed = true; it.remove(); break;
                    case SERVER_COUNTRY: removed = true; it.remove(); break;
                    default: break;
                    }
                    if (removed) {
                        logger.warn("Removing shield rule (unsupported condition): " + rule);
                        break;
                    }
                }
            }

            /**
             * Change the action to the new format as best is possible
             */
            for( ShieldRule rule : settings.getRules() ) {
                int multiplier = rule.getMultiplier();

                // if unlimited - set unlimited (pass)
                if ( multiplier < 0 ) {
                    logger.warn("Setting unlimited rule to unlimited: " + rule);
                    rule.setAction(ShieldRule.ShieldRuleAction.PASS);
                    continue;
                }
                // if a high multiplier - set unlimited (pass)
                if ( multiplier > 20 ) {
                    logger.warn("Setting high multiplier rule to unlimited: " + rule);
                    rule.setAction(ShieldRule.ShieldRuleAction.PASS);
                    continue;
                }
                logger.warn("Setting normal rule to scan: " + rule);
                rule.setAction(ShieldRule.ShieldRuleAction.SCAN);
            }
        }

        logger.warn("Converting Shield settings to V1...done");
    }
}
