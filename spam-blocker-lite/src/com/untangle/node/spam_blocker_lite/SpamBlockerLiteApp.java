/**
 * $Id$
 */
package com.untangle.node.spam_blocker_lite;

import org.apache.log4j.Logger;

import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.DaemonManager;

public class SpamBlockerLiteApp extends SpamNodeImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    public SpamBlockerLiteApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new SpamAssassinScanner() );
    }

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-spam-blocker-lite/settings_" + nodeID + ".js";
        SpamSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( SpamSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSettings();
                SpamSettings ps = getSettings();
                initSpamDnsblList(ps);
                this.setSettings(ps);
            }
            else {
                this.spamSettings = readSettings;
                initSpamDnsblList(this.spamSettings);
            }
        } catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    @Override
    public void setSettings(SpamSettings newSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-spam-blocker-lite/settings_" + nodeID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings);
        } catch (Exception exn) {
            logger.error("Could not save node settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }
    
    @Override
    protected void preInit()
    {
        readNodeSettings();
        SpamSettings ps = getSettings();
        initSpamDnsblList(ps);
    }

    @Override
    protected void preStart()
    {
        UvmContextFactory.context().daemonManager().incrementUsageCount( "spamassassin" );
        String transmit = "PING SPAMC/1.0\r\n";
        String search = "SPAMD/1.5 0 PONG";
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("spamassassin", 300, "127.0.0.1", 783, transmit, search);
        super.preStart();
    }
    
    @Override
    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount( "spamassassin" );
        super.postStop();
    }

    public String getVendor()
    {
        return "spam_blocker_lite";
    }
}
