/**
 * $Id$
 */
package com.untangle.node.spamassassin;

import org.apache.log4j.Logger;

import com.untangle.node.spam.SpamNodeImpl;
import com.untangle.node.spam.SpamSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;

public class SpamAssassinNode extends SpamNodeImpl
{
    private final Logger logger = Logger.getLogger(getClass());

    public SpamAssassinNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new SpamAssassinScanner() );
    }

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-spamassassin/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";
        SpamSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  settingsManager.load( SpamSettings.class, settingsBase);
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
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-spamassassin/settings_" + nodeID;

        try {
            settingsManager.save( SpamSettings.class, settingsBase, newSettings);
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

    public String getVendor()
    {
        return "sa";
    }
}
