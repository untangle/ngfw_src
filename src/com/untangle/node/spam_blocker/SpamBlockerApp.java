/*
 * $Id: SpamBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.node.spam_blocker;

import org.apache.log4j.Logger;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import com.untangle.uvm.node.License;
import com.untangle.node.spam_blocker.SpamBlockerBaseApp;
import com.untangle.node.spam_blocker.SpamSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.DaemonManager;

public class SpamBlockerApp extends SpamBlockerBaseApp
{
    private final Logger logger = Logger.getLogger(getClass());

    public SpamBlockerApp(com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties)
    {
        super(nodeSettings, nodeProperties, new SpamBlockerScanner());
    }

    private void readNodeSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-spam-blocker/settings_" + nodeID + ".js";
        SpamSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings = settingsManager.load(SpamSettings.class, settingsFile);
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
            } else {
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
        String settingsFile = System.getProperty("uvm.settings.dir") + "/untangle-node-spam-blocker/settings_" + nodeID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
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
		String transmit;
		String search;

        // for both of these we only need to enable the monitoring since it
		// will be disabled automatically when the daemon counts reach zero

        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-spamcatd");
        transmit = "PING SPAMC/1.0\r\n";
        search = "SPAMD/1.5 0 PONG";
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("untangle-spamcatd", 300, "127.0.0.1", 1784, transmit, search);

        UvmContextFactory.context().daemonManager().incrementUsageCount("spamassassin");
        transmit = "PING SPAMC/1.0\r\n";
        search = "SPAMD/1.5 0 PONG";
        UvmContextFactory.context().daemonManager().enableRequestMonitoring("spamassassin", 300, "127.0.0.1", 783, transmit, search);

        super.preStart();
    }

    @Override
    protected void postStop()
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-spamcatd");
        UvmContextFactory.context().daemonManager().decrementUsageCount("spamassassin");
        super.postStop();
    }

    protected boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SPAM_BLOCKER))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.SPAM_BLOCKER_OLDNAME))
            return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.COMMTOUCHAS))
            return true;
        return false;
    }

    public String getVendor()
    {
        return "spam_blocker";
    }
}
