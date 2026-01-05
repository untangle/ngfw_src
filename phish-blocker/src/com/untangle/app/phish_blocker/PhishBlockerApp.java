/**
 * $Id$
 */

package com.untangle.app.phish_blocker;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.untangle.uvm.DaemonManager;
import com.untangle.uvm.ExecManagerResultReader;

import com.untangle.app.spam_blocker.SpamBlockerBaseApp;
import com.untangle.app.spam_blocker.SpamSettings;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;

/**
 * The phish blocker application
 */
public class PhishBlockerApp extends SpamBlockerBaseApp
{
    private final Logger logger = LogManager.getLogger(getClass());

    // We want to make sure that phish is before spam,
    // before virus in the pipeline (towards the client for smtp).
    protected final PipelineConnector connector = UvmContextFactory.context().pipelineFoundry().create("phish-smtp", this, null, new PhishBlockerSmtpHandler(this), Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, 20, false);
    protected final PipelineConnector[] connectors = new PipelineConnector[] { connector };
    final String STOP_SOCKET = "systemctl stop clamav-daemon.socket";
    DaemonManager daemonManager = UvmContextFactory.context().daemonManager();

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public PhishBlockerApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties, new PhishBlockerScanner());
    }

    /**
     * Load the application settings
     */
    private void readAppSettings()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/phish-blocker/settings_" + appID + ".js";
        PhishBlockerSettings readSettings = null;

        logger.info("Loading settings from " + settingsFile);

        try {
            readSettings = settingsManager.load(PhishBlockerSettings.class, settingsFile);
        } catch (Exception exn) {
            logger.error("Could not read app settings", exn);
        }

        try {
            if (readSettings == null) {
                logger.warn("No settings found... initializing with defaults");
                initializeSettings();
            } else {
                this.spamSettings = readSettings;
            }
        } catch (Exception exn) {
            logger.error("Could not apply app settings", exn);
        }
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public PhishBlockerSettings getSettings()
    {
        return (PhishBlockerSettings) super.getSettings();
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The settings
     */
    public void setSettings(PhishBlockerSettings newSettings)
    {
        logger.info("setSettings()");

        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFile = System.getProperty("uvm.settings.dir") + "/phish-blocker/settings_" + appID + ".js";

        try {
            settingsManager.save(settingsFile, newSettings);
        } catch (Exception exn) {
            logger.error("Could not save Phish Blocker settings", exn);
            return;
        }

        super.setSettings(newSettings);
    }

    /**
     * Initialize the application settings
     */
    public void initializeSettings()
    {
        logger.info("Initializing Settings");

        PhishBlockerSettings tmpSpamSettings = new PhishBlockerSettings();
        configureSpamSettings(tmpSpamSettings);
        tmpSpamSettings.getSmtpConfig().setBlockSuperSpam(false); // no such thing as 'super' phish
        tmpSpamSettings.getSmtpConfig().setAllowTls(true); // allow TLS in phishing by default

        setSettings(tmpSpamSettings);
        initSpamDnsblList(tmpSpamSettings);
    }

    /**
     * Check the premium flag
     * 
     * @return True if premium, otherwise false
     */
    @Override
    public boolean isPremium()
    {
        return false;
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    @Override
    public String getVendor()
    {
        return "Clam";
    }

    /**
     * Get the pipeline connectors
     * 
     * @return The pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called before the application is initialized
     */
    @Override
    protected void preInit()
    {
        readAppSettings();
        SpamSettings ps = getSettings();
        ps.getSmtpConfig().setBlockSuperSpam(false);
        initSpamDnsblList(ps);
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        daemonManager.incrementUsageCount(Constants.CLAMAV_DAEMON);
        daemonManager.incrementUsageCount(Constants.FRESHCLAM);
        daemonManager.enableDaemonMonitoring(Constants.CLAMAV_DAEMON, 300, "clamd");
        daemonManager.enableDaemonMonitoring(Constants.FRESHCLAM, 3600, "freshclam");
        super.preStart(isPermanentTransition);
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        daemonManager.decrementUsageCount(Constants.CLAMAV_DAEMON);
        int count = daemonManager.getUsageCount(Constants.CLAMAV_DAEMON);
        // Disable socket service when daemon usage reaches zero
        if(count == 0){
            try {
                ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(STOP_SOCKET);
                reader.waitFor();
                logger.info("Stopped clamav-daemon.socket because usage count reached zero");
            } catch (Exception exn) {
                logger.warn("Failed to run command: " + STOP_SOCKET, exn);
            }
        }
        daemonManager.decrementUsageCount(Constants.FRESHCLAM);
        super.postStop(isPermanentTransition);
    }
}
