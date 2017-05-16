/*
 * $Id: SpamBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.spam_blocker;

import org.apache.log4j.Logger;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import com.untangle.uvm.app.License;
import com.untangle.app.spam_blocker.SpamBlockerBaseApp;
import com.untangle.app.spam_blocker.SpamSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.DaemonManager;

public class SpamBlockerApp extends SpamBlockerBaseApp
{
    private final Logger logger = Logger.getLogger(getClass());

    public SpamBlockerApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties, new SpamBlockerScanner());
    }

    {
    }

    @Override
    public void setSettings(SpamSettings newSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/spam-blocker/settings_" + appID + ".js";

        try {
            settingsManager.save( settingsFile, newSettings );
        } catch (Exception exn) {
            logger.error("Could not save app settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }

    @Override
    protected void preInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/spam-blocker/settings_" + appID + ".js";
        SpamSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings = settingsManager.load(SpamSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read app settings", exn);
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
            logger.error("Could not apply app settings", exn);
        }

        // try to download spamassassin sigs if they do not exist
        try {
            if ( ! (new java.io.File("/var/lib/spamassassin/3.004000/updates_spamassassin_org.cf")).exists() ) {
                UvmContextFactory.context().execManager().exec("nohup sleep 120 && /etc/cron.daily/spamassassin >/dev/null 2>&1 &");
            }
        } catch (Exception e) {
            logger.warn("Exception",e);
        }

        initSpamDnsblList(getSettings());
    }

    @Override
    protected void preStart( boolean isPermanentTransition )
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

        // enable CRON job
        UvmContextFactory.context().execManager().exec("grep -q -F 'CRON=1' /etc/default/spamassassin || sed -i -e 's/^CRON=.*/CRON=1/' /etc/default/spamassassin");

        super.preStart( isPermanentTransition );
    }

    @Override
    protected void postStop( boolean isPermanentTransition )
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-spamcatd");
        UvmContextFactory.context().daemonManager().decrementUsageCount("spamassassin");

        // disable CRON job if permanent and no one else using SA
        if ( isPermanentTransition && UvmContextFactory.context().daemonManager().getUsageCount("spamassassin") == 0 )
            UvmContextFactory.context().execManager().exec("sed -i -e 's/^CRON=.*/CRON=0/' /etc/default/spamassassin");
    }

    @Override
    public boolean isPremium()
    {
        return true;
    }

    @Override
    public String getVendor()
    {
        return "spam_blocker";
    }
}
