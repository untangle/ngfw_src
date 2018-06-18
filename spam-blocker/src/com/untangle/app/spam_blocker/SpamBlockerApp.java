/**
 * $Id: SpamBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.spam_blocker;

import org.apache.log4j.Logger;

import com.untangle.app.spam_blocker.SpamBlockerBaseApp;
import com.untangle.app.spam_blocker.SpamSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;

/**
 * The spam blocker application
 */
public class SpamBlockerApp extends SpamBlockerBaseApp
{
    private final Logger logger = Logger.getLogger(getClass());

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public SpamBlockerApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties, new SpamBlockerScanner());
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The settings
     */
    @Override
    public void setSettings(SpamSettings newSettings)
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/spam-blocker/settings_" + appID + ".js";

        try {
            settingsManager.save(settingsFile, newSettings);
        } catch (Exception exn) {
            logger.error("Could not save app settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }

    /**
     * Called before the application is initialized
     */
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
            if (!(new java.io.File("/var/lib/spamassassin/3.004001/updates_spamassassin_org.cf")).exists()) {
                logger.info("Signatures not found! Forcing Asynchronous update...");
                UvmContextFactory.context().execManager().execEvilProcess("/etc/cron.daily/spamassassin");
                // Do not wait on process to finish. It can take a long time. Just continue
            }
        } catch (Exception e) {
            logger.warn("Exception", e);
        }

        initSpamDnsblList(getSettings());
    }

    /**
     * Called before the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
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

        super.preStart(isPermanentTransition);
    }

    /**
     * Called after the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag.
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-spamcatd");
        UvmContextFactory.context().daemonManager().decrementUsageCount("spamassassin");

        // disable CRON job if permanent and no one else using SA
        if (isPermanentTransition && UvmContextFactory.context().daemonManager().getUsageCount("spamassassin") == 0) UvmContextFactory.context().execManager().exec("sed -i -e 's/^CRON=.*/CRON=0/' /etc/default/spamassassin");
    }

    /**
     * Get the premium flag
     * 
     * @return True if premium, otherwise false
     */
    @Override
    public boolean isPremium()
    {
        return true;
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    @Override
    public String getVendor()
    {
        return "spam_blocker";
    }
}
