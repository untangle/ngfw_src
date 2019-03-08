/**
 * $Id$
 */

package com.untangle.app.spam_blocker;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.io.File;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppMetric;

/**
 * Spam Blocker Implementation
 */
public abstract class SpamBlockerBaseApp extends AppBase
{
    private static final Logger logger = Logger.getLogger(SpamBlockerBaseApp.class);

    private static final String STAT_RECEIVED = "email-received";
    private static final String STAT_SPAM = "spam-detected";
    private static final String STAT_PASS = "pass";
    private static final String STAT_DROP = "drop";
    private static final String STAT_MARK = "mark";
    private static final String STAT_QUARANTINE = "quarantine";

    private final TarpitEventHandler tarpitHandler = new TarpitEventHandler(this);

    private final PipelineConnector smtpConnector;
    private final PipelineConnector tarpitConnector;
    private final PipelineConnector[] connectors;

    private final SpamScanner scanner;

    protected volatile SpamSettings spamSettings;

    private String signatureVersion;
    private Date lastUpdate = new Date();
    private Date lastUpdateCheck = new Date();

    private static final String GREYLIST_SAVE_FILENAME = System.getProperty("uvm.conf.dir") + "/greylist.js";
    private static final long GREYLIST_SAVE_FREQUENCY = 7 * 24 * 60 * 60 * 1000L; // weekly
    private static Map<GreyListKey, Boolean> greylist = Collections.synchronizedMap(new GreyListMap<GreyListKey, Boolean>());
    private volatile static boolean greyListLoaded = false;
    private volatile static long greyListLastSave = System.currentTimeMillis();
    private static Pulse greyListSaverPulse = null;

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     * @param scanner
     *        The scanner
     */
    @SuppressWarnings("unchecked")
    public SpamBlockerBaseApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties, SpamScanner scanner)
    {
        super(appSettings, appProperties);

        this.scanner = scanner;

        String vendor = scanner.getVendorName();
        String vendorTag = vendor;
        String badEmailName = "Spam";

        if (vendor.equals("SpamAssassin")) vendorTag = "spam_blocker_lite";
        else if (vendor.equals("SpamBlockerLite")) vendorTag = "spam_blocker_lite";
        else if (vendor.equals("SpamBlocker")) vendorTag = "spam_blocker";
        else if (vendor.equals("Clam")) {
            vendorTag = "phish_blocker";
            badEmailName = "Phish";
        } else if (vendor.equals("PhishBlocker")) {
            vendorTag = "phish_blocker";
            badEmailName = "Phish";
        }

        this.addMetric(new AppMetric(STAT_RECEIVED, I18nUtil.marktr("Messages received")));
        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Messages passed")));
        this.addMetric(new AppMetric(STAT_DROP, I18nUtil.marktr("Messages dropped")));
        this.addMetric(new AppMetric(STAT_MARK, I18nUtil.marktr("Messages marked")));
        this.addMetric(new AppMetric(STAT_QUARANTINE, I18nUtil.marktr("Messages quarantined")));
        this.addMetric(new AppMetric(STAT_SPAM, I18nUtil.marktr("Spam detected")));

        // We want to make sure that spam is before virus in the pipeline (towards the client for smtp)
        // Would want the tarpit event handler before the casing, this way if it blocks a session the casing doesn't have to be initialized.
        this.smtpConnector = UvmContextFactory.context().pipelineFoundry().create("spam-smtp", this, null, new SpamSmtpHandler(this), Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, 10, isPremium());
        this.tarpitConnector = UvmContextFactory.context().pipelineFoundry().create("spam-tarpit-smtp", this, null, this.tarpitHandler, Fitting.SMTP_STREAM, Fitting.SMTP_STREAM, Affinity.CLIENT, 11, isPremium());
        this.connectors = new PipelineConnector[] { smtpConnector, tarpitConnector };

        loadGreyList();

        synchronized (SpamBlockerBaseApp.class) {
            if (greyListSaverPulse == null) {
                greyListSaverPulse = new Pulse("GreyListSaver", new GreyListSaver(), GREYLIST_SAVE_FREQUENCY);
                greyListSaverPulse.start();
            }
        }
    }

    /**
     * Increment the counter for blocked
     */
    public void incrementBlockCount()
    {
        this.incrementMetric(STAT_RECEIVED);
        this.incrementMetric(STAT_SPAM);
        this.incrementMetric(STAT_DROP);
    }

    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCount()
    {
        this.incrementMetric(STAT_RECEIVED);
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Increment the counter for messages marked
     */
    public void incrementMarkCount()
    {
        this.incrementMetric(STAT_RECEIVED);
        this.incrementMetric(STAT_SPAM);
        this.incrementMetric(STAT_MARK);
    }

    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount()
    {
        this.incrementMetric(STAT_RECEIVED);
        this.incrementMetric(STAT_SPAM);
        this.incrementMetric(STAT_QUARANTINE);
    }

    /**
     * Initialize the DNS blacklist
     * 
     * @param tmpSpamSettings
     *        The spam settings
     */
    protected void initSpamDnsblList(SpamSettings tmpSpamSettings)
    {
        initSpamDnsblList(tmpSpamSettings.getSpamDnsblList());
    }

    /**
     * Initialize the DNS blacklist
     * 
     * @param spamDnsblList
     *        The DNS blacklist
     */
    protected void initSpamDnsblList(List<SpamDnsbl> spamDnsblList)
    {
        if ((null == spamDnsblList) || (false == spamDnsblList.isEmpty())) {
            // if already initialized,
            // use list as-is (e.g., database contains final word)
            //
            // if need be, update existing list here
            //
            // on return, save to database
            return;
        } // else initialize list now (e.g., upgrade has just occurred)

        spamDnsblList.add(new SpamDnsbl("zen.spamhaus.org", "Spamhaus SBL, XBL and PBL lists.", true));
        // spamDnsblList.add(new SpamDnsbl("list.dsbl.org", "Distributed Sender Blackhole List", true));
        // spamDnsblList.add(new SpamDnsbl("sbl-xbl.spamhaus.org", "Spamhaus Block and Exploits Block Lists", true));
        // spamDnsblList.add(new SpamDnsbl("bl.spamcop.net", "SpamCop Blocking List", true));

        return;
    }

    /**
     * Configure the spam settings
     * 
     * @param tmpSpamSettings
     *        The settings
     */
    protected void configureSpamSettings(SpamSettings tmpSpamSettings)
    {
        tmpSpamSettings.setSmtpConfig(new SpamSmtpConfig(true, SpamMessageAction.QUARANTINE, SpamSmtpConfig.DEFAULT_STRENGTH, SpamSmtpConfig.DEFAULT_ADD_SPAM_HEADERS, SpamSmtpConfig.DEFAULT_BLOCK_SUPER_SPAM, SpamSmtpConfig.DEFAULT_SUPER_STRENGTH, SpamSmtpConfig.DEFAULT_FAIL_CLOSED, SpamSmtpConfig.DEFAULT_HEADER_NAME, SpamSmtpConfig.DEFAULT_TARPIT, SpamSmtpConfig.DEFAULT_TARPIT_TIMEOUT, SpamSmtpConfig.DEFAULT_LIMIT_LOAD, SpamSmtpConfig.DEFAULT_LIMIT_SCANS, SpamSmtpConfig.DEFAULT_SCAN_WAN_MAIL, SpamSmtpConfig.DEFAULT_ALLOW_TLS));
    }

    /**
     * Get the settings
     * 
     * @return The settings
     */
    public SpamSettings getSettings()
    {
        if (this.spamSettings == null) {
            logger.error("Settings not yet initialized. State: " + this.getRunState());
        }

        return this.spamSettings;
    }

    /**
     * Set the settings
     * 
     * @param newSpamSettings
     *        The settings
     */
    public void setSettings(final SpamSettings newSpamSettings)
    {
        // set lists if not already set
        initSpamDnsblList(newSpamSettings);
        SpamBlockerBaseApp.this.spamSettings = newSpamSettings;
    }

    /**
     * Enable SMTP spam headers
     * 
     * @param enableHeaders
     *        Enable flag
     */
    public void enableSmtpSpamHeaders(boolean enableHeaders)
    {
        SpamSettings ss = getSettings();
        ss.getSmtpConfig().setAddSpamHeaders(enableHeaders);
        setSettings(ss);
    }

    /**
     * Enable SMTP fail closed
     * 
     * @param failClosed
     *        Enable flag
     */
    public void enableSmtpFailClosed(boolean failClosed)
    {
        SpamSettings ss = getSettings();
        ss.getSmtpConfig().setFailClosed(failClosed);
        setSettings(ss);
    }

    /**
     * Update the scanner information
     */
    public void updateScannerInfo()
    {
        Date lastSignatureUpdate = scanner.getLastSignatureUpdate();
        if (lastSignatureUpdate != null) lastUpdate = lastSignatureUpdate;

        Date lastSignatureUpdateCheck = scanner.getLastSignatureUpdateCheck();
        if (lastSignatureUpdateCheck != null) this.lastUpdateCheck = lastSignatureUpdateCheck;

        String signatureVersion = scanner.getSignatureVersion();
        if (signatureVersion != null) this.signatureVersion = signatureVersion;
    }

    /**
     * Initialize the application
     */
    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings tmpSpamSettings = new SpamSettings();
        configureSpamSettings(tmpSpamSettings);
        setSettings(tmpSpamSettings);
    }

    /**
     * Get the vendor
     * 
     * @return The vendor
     */
    public abstract String getVendor();

    /**
     * Get the premium flag
     * 
     * @return the premium flag
     */
    public abstract boolean isPremium();

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
        initializeSettings();
        initSpamDnsblList(spamSettings);
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
        saveGreyList();
    }

    /**
     * Get the scanner
     * 
     * @return The scanner
     */
    public SpamScanner getScanner()
    {
        return scanner;
    }

    /**
     * Get the date of the last update
     * 
     * @return The date of the last update
     */
    public Date getLastUpdate()
    {
        updateScannerInfo();
        return lastUpdate;
    }

    /**
     * Set the date of the last update
     * 
     * @param newValue
     *        The date of the last update
     */
    public void setLastUpdate(Date newValue)
    {
        lastUpdate = newValue;
    }

    /**
     * Get the date of the last update check
     * 
     * @return The date of the last update check
     */
    public Date getLastUpdateCheck()
    {
        updateScannerInfo();
        return lastUpdateCheck;
    }

    /**
     * Set the date of the last update check
     * 
     * @param newValue
     *        The date of the last update check
     */
    public void setLastUpdateCheck(Date newValue)
    {
        lastUpdateCheck = newValue;
    }

    /**
     * Get the signature version
     * 
     * @return The signature version
     */
    public String getSignatureVersion()
    {
        updateScannerInfo();
        return signatureVersion;
    }

    /**
     * Set the signature version
     * 
     * @param newValue
     *        The signature version
     */
    public void setSignatureVersion(String newValue)
    {
        signatureVersion = newValue;
    }

    /**
     * Get the gray list
     * 
     * @return The gray list
     */
    protected static Map<GreyListKey, Boolean> getGreylist()
    {
        return SpamBlockerBaseApp.greylist;
    }

    /**
     * Load the grey list
     */
    @SuppressWarnings("unchecked")
    private static void loadGreyList()
    {
        /**
         * Need to load any saved values in the greylist Do this asynchronously
         * because it can take a long time if the greylist is big
         */
        Runnable loadGreylist = new Runnable()
        {
            /**
             * The main run function
             */
            public void run()
            {
                /**
                 * If already loaded (by another instance of this app) just
                 * return
                 */
                if (greyListLoaded) return;
                greyListLoaded = true;

                /**
                 * If there is no save file, just return
                 */
                if (!(new File(GREYLIST_SAVE_FILENAME)).exists()) return;

                try {
                    logger.info("Loading greylist from file...");
                    LinkedList<GreyListKey> savedEntries = UvmContextFactory.context().settingsManager().load(LinkedList.class, GREYLIST_SAVE_FILENAME);
                    for (GreyListKey key : savedEntries) {
                        greylist.put(key, Boolean.TRUE);
                    }
                    logger.info("Loading greylist from file... done (" + greylist.size() + " entries)");
                } catch (Exception e) {
                    logger.warn("Exception", e);
                }
            }
        };
        Thread t = UvmContextFactory.context().newThread(loadGreylist, "GREYLIST_LOADER");
        t.start();
    }

    /**
     * Save the grey list
     */
    private static void saveGreyList()
    {
        /**
         * If saved less than 30 seconds ago, do not save again
         */
        long currentTime = System.currentTimeMillis();
        if (currentTime - SpamBlockerBaseApp.greyListLastSave < 30000) return;
        SpamBlockerBaseApp.greyListLastSave = currentTime;

        try {
            Set<GreyListKey> keys = SpamBlockerBaseApp.greylist.keySet();
            logger.info("Saving greylist to file... (" + keys.size() + " entries)");
            LinkedList<GreyListKey> list = new LinkedList<GreyListKey>();
            for (GreyListKey key : keys) {
                list.add(key);
            }
            UvmContextFactory.context().settingsManager().save(GREYLIST_SAVE_FILENAME, list, false, false);
            logger.info("Saving greylist to file... done");
        } catch (Exception e) {
            logger.warn("Exception", e);
        }

    }

    /**
     * Grey list saver
     */
    private static class GreyListSaver implements Runnable
    {
        /**
         * The main run function
         */
        public void run()
        {
            SpamBlockerBaseApp.saveGreyList();
        }
    }
}
