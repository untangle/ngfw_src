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
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/spamassassin-convert-settings.py";
    private final Logger logger = Logger.getLogger(getClass());

    public SpamAssassinNode( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties, new SpamAssassinScanner() );
    }

    private void readNodeSettings()
    {
        SettingsManager setman = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-spamassassin/settings_" + nodeID;
        String settingsFile = settingsBase + ".js";
        SpamSettings readSettings = null;
        
        logger.info("Loading settings from " + settingsFile);
        
        try {
            readSettings =  setman.load( SpamSettings.class, settingsBase);
        }

        catch (Exception exn) {
            logger.error("Could not read node settings", exn);
        }

        // if no settings found try getting them from the database
        if (readSettings == null) {
            logger.warn("No json settings found... attempting to import from database");
            
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " +  settingsFile;
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            }

            catch (Exception exn) {
                logger.error("Conversion script failed", exn);
            }

            try {
                readSettings = setman.load( SpamSettings.class, settingsBase);
            }

            catch (Exception exn) {
                logger.error("Could not read node settings", exn);
            }
            
            if (readSettings != null) logger.warn("Database settings successfully imported");
        }

        try {
            if (readSettings == null) {
                logger.warn("No database or json settings found... initializing with defaults");
                initializeSettings();
                SpamSettings ps = getSettings();
                initSpamDnsblList(ps);
                writeNodeSettings(getSettings());
            }
            else {
                this.spamSettings = readSettings;
                initSpamDnsblList(this.spamSettings);
            }
        }
        catch (Exception exn) {
            logger.error("Could not apply node settings", exn);
        }
    }

    private void writeNodeSettings(SpamSettings argSettings)
    {
        SettingsManager setman = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsBase = System.getProperty("uvm.settings.dir") + "/untangle-node-spamassassin/settings_" + nodeID;

        try {
            setman.save( SpamSettings.class, settingsBase, argSettings);
        }

        catch (Exception exn) {
            logger.error("Could not save node settings", exn);
        }
    }

    @Override
    public void setSettings(SpamSettings spamSettings)
    {
        super.setSettings(spamSettings);
        writeNodeSettings(spamSettings);
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
