/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.io.File;
import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.SqlCondition;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;

public class SpamNodeImpl extends NodeBase implements SpamNode
{
    private static final Logger logger = Logger.getLogger( SpamNodeImpl.class );

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

    private EventLogQuery allEventQuery;
    private EventLogQuery spamEventQuery;
    private EventLogQuery quarantinedEventQuery;
    private EventLogQuery tarpitEventQuery;
    
    private String signatureVersion;
    private Date lastUpdate = new Date();
    private Date lastUpdateCheck = new Date();

    private static final String GREYLIST_SAVE_FILENAME = System.getProperty("uvm.conf.dir") + "/greylist.js";
    private static final long GREYLIST_SAVE_FREQUENCY = 7*24*60*60*1000L; // weekly
    private static Map<GreyListKey,Boolean> greylist = Collections.synchronizedMap(new GreyListMap<GreyListKey,Boolean>());
    private volatile static boolean greyListLoaded = false;
    private volatile static long greyListLastSave = System.currentTimeMillis();
    private static Pulse greyListSaverPulse = null;
    
    @SuppressWarnings("unchecked")
    public SpamNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties, SpamScanner scanner )
    {
        super( nodeSettings, nodeProperties );
        
        this.scanner = scanner;

        String vendor = scanner.getVendorName();
        String vendorTag = vendor;
        String badEmailName = "Spam";
        
        if (vendor.equals("SpamAssassin")) 
            vendorTag = "spamassassin";
        else if (vendor.equals("SpamBlocker"))
            vendorTag = "spamblocker";
        else if (vendor.equals("Clam")) {
            vendorTag = "phish";
            badEmailName = "Phish";
        }

        this.addMetric(new NodeMetric(STAT_RECEIVED, I18nUtil.marktr("Messages received")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Messages passed")));
        this.addMetric(new NodeMetric(STAT_DROP, I18nUtil.marktr("Messages dropped")));
        this.addMetric(new NodeMetric(STAT_MARK, I18nUtil.marktr("Messages marked")));
        this.addMetric(new NodeMetric(STAT_QUARANTINE, I18nUtil.marktr("Messages quarantined")));
        this.addMetric(new NodeMetric(STAT_SPAM, I18nUtil.marktr("Spam detected")));

        // We want to make sure that spam is before virus in the pipeline (towards the client for smtp)
        // Would want the tarpit event handler before the casing, this way if it blocks a session the casing doesn't have to be initialized.
        this.smtpConnector = UvmContextFactory.context().pipelineFoundry().create("spam-smtp", this, null, new SpamSmtpHandler(this), Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, 10);
        this.tarpitConnector = UvmContextFactory.context().pipelineFoundry().create("spam-smtp", this, null, this.tarpitHandler, Fitting.SMTP_STREAM, Fitting.SMTP_STREAM, Affinity.CLIENT, 11);
        this.connectors = new PipelineConnector[] { smtpConnector, tarpitConnector };
        
        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Email Events"), "mail_addrs",
                                               new SqlCondition[]{ new SqlCondition("addr_kind","in","('T', 'C')"),
                                                                   new SqlCondition(vendorTag + "_action","is","NOT NULL"),
                                                                   new SqlCondition("policy_id","=",":policyId") });
        this.spamEventQuery = new EventLogQuery(I18nUtil.marktr("All") + " " + I18nUtil.marktr(badEmailName) + " " + I18nUtil.marktr("Events"), "mail_addrs",
                                                new SqlCondition[]{ new SqlCondition("addr_kind","in","('T', 'C')"),
                                                                    new SqlCondition(vendorTag + "_is_spam","is","TRUE"),
                                                                    new SqlCondition("policy_id","=",":policyId") });
        this.quarantinedEventQuery = new EventLogQuery(I18nUtil.marktr("Quarantined Events"), "mail_addrs",
                                                       new SqlCondition[]{ new SqlCondition("addr_kind","in","('T', 'C')"),
                                                                           new SqlCondition(vendorTag + "_action","=","'Q'"),
                                                                           new SqlCondition("policy_id","=",":policyId") });
        this.tarpitEventQuery = new EventLogQuery(I18nUtil.marktr("Tarpit Events"), "spam_smtp_tarpit_events",
                                                  new SqlCondition[]{ new SqlCondition("vendor_name","=","'"+vendorTag+"'"),
                                                                      new SqlCondition("policy_id","=",":policyId") });
        loadGreyList();

        synchronized( this ) {
            if ( greyListSaverPulse == null ) {
                greyListSaverPulse = new Pulse("GreyListSaver", true, new GreyListSaver());
                greyListSaverPulse.start( GREYLIST_SAVE_FREQUENCY );
            }
        }
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventQuery, this.spamEventQuery, this.quarantinedEventQuery };
    }
    
    public EventLogQuery[] getTarpitEventQueries()
    {
        return new EventLogQuery[] { this.tarpitEventQuery };
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

    protected void initSpamDnsblList(SpamSettings tmpSpamSettings)
    {
        initSpamDnsblList(tmpSpamSettings.getSpamDnsblList());
    }

    protected void initSpamDnsblList(List<SpamDnsbl> spamDnsblList)
    {
        if (( null == spamDnsblList) || ( false == spamDnsblList.isEmpty())) {
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

    protected void configureSpamSettings(SpamSettings tmpSpamSettings)
    {
        tmpSpamSettings.setSmtpConfig(new SpamSmtpConfig(true,
                                                         SpamMessageAction.QUARANTINE,
                                                         SpamSmtpConfig.DEFAULT_STRENGTH,
                                                         SpamSmtpConfig.DEFAULT_ADD_SPAM_HEADERS,
                                                         SpamSmtpConfig.DEFAULT_BLOCK_SUPER_SPAM,
                                                         SpamSmtpConfig.DEFAULT_SUPER_STRENGTH,
                                                         SpamSmtpConfig.DEFAULT_FAIL_CLOSED,
                                                         SpamSmtpConfig.DEFAULT_HEADER_NAME,
                                                         SpamSmtpConfig.DEFAULT_TARPIT,
                                                         SpamSmtpConfig.DEFAULT_TARPIT_TIMEOUT,
                                                         SpamSmtpConfig.DEFAULT_LIMIT_LOAD,
                                                         SpamSmtpConfig.DEFAULT_LIMIT_SCANS,
                                                         SpamSmtpConfig.DEFAULT_SCAN_WAN_MAIL,
                                                         SpamSmtpConfig.DEFAULT_ALLOW_TLS));
    }

    public SpamSettings getSettings()
    {
        if( this.spamSettings == null ) {
            logger.error("Settings not yet initialized. State: " + this.getRunState() );
        }

        return this.spamSettings;
    }

    public void setSettings(final SpamSettings newSpamSettings)
    {
        // set lists if not already set
        initSpamDnsblList(newSpamSettings);
        SpamNodeImpl.this.spamSettings = newSpamSettings;
    }

    public void enableSmtpSpamHeaders(boolean enableHeaders)
    {
        SpamSettings ss = getSettings();
        ss.getSmtpConfig().setAddSpamHeaders(enableHeaders);
        setSettings(ss);
    }

    public void enableSmtpFailClosed(boolean failClosed)
    {
        SpamSettings ss = getSettings();
        ss.getSmtpConfig().setFailClosed(failClosed);
        setSettings(ss);
    }

    public void updateScannerInfo()
    {
        Date lastSignatureUpdate = scanner.getLastSignatureUpdate();
        if (lastSignatureUpdate != null) lastUpdate = lastSignatureUpdate;
        
        Date lastSignatureUpdateCheck = scanner.getLastSignatureUpdateCheck();
        if (lastSignatureUpdateCheck != null) this.lastUpdateCheck = lastSignatureUpdateCheck;
        
        String signatureVersion = scanner.getSignatureVersion();
        if (signatureVersion != null) this.signatureVersion = signatureVersion;
    }

    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings tmpSpamSettings = new SpamSettings();
        configureSpamSettings(tmpSpamSettings);
        setSettings(tmpSpamSettings);
    }

    // NodeBase methods ----------------------------------------------
    public String getVendor()
    {
        return "sa";
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preInit()
    {
        initializeSettings();
        initSpamDnsblList(spamSettings);
    }

    @Override
    protected void postStop()
    {
        saveGreyList();
    }
    
    public SpamScanner getScanner()
    {
        return scanner;
    }

    public Date getLastUpdate()
    {
        updateScannerInfo();
        return lastUpdate;
    }

    public void setLastUpdate(Date newValue)
    {
        lastUpdate = newValue;
    }

    public Date getLastUpdateCheck()
    {
        updateScannerInfo();
        return lastUpdateCheck;
    }

    public void setLastUpdateCheck(Date newValue)
    {
        lastUpdateCheck = newValue;
    }

    public String getSignatureVersion()
    {
        updateScannerInfo();
        return signatureVersion;
    }

    public void setSignatureVersion(String newValue)
    {
        signatureVersion = newValue;
    }

    protected static Map<GreyListKey,Boolean> getGreylist()
    {
        return SpamNodeImpl.greylist;
    }

    @SuppressWarnings("unchecked")
    private static void loadGreyList()
    {
        /**
         * Need to load any saved values in the greylist
         * Do this asynchronously because it can take a long time if the greylist is big
         */
        Runnable loadGreylist = new Runnable() {
                public void run()
                {
                    /**
                     * If already loaded (by another instance of this node) just return
                     */
                    if ( greyListLoaded )
                        return;
                    greyListLoaded = true;
                    
                    /**
                     * If there is no save file, just return
                     */
                    if ( ! (new File(GREYLIST_SAVE_FILENAME)).exists() )
                        return;

                    try {
                        logger.info("Loading greylist from file...");
                        LinkedList<GreyListKey> savedEntries = UvmContextFactory.context().settingsManager().load( LinkedList.class, GREYLIST_SAVE_FILENAME );
                        for ( GreyListKey key : savedEntries ) {
                            greylist.put( key, Boolean.TRUE );
                        }
                        logger.info("Loading greylist from file... done (" + greylist.size() + " entries)");
                    } catch (Exception e) {
                        logger.warn("Exception",e);
                    }
                }
            };
        Thread t = UvmContextFactory.context().newThread( loadGreylist, "GREYLIST_LOADER" );
        t.start();
    }

    private static void saveGreyList()
    {
        /**
         * If saved less than 30 seconds ago, do not save again
         */
        long currentTime = System.currentTimeMillis();
        if ( currentTime - SpamNodeImpl.greyListLastSave < 30000 )
            return;
        SpamNodeImpl.greyListLastSave = currentTime;
        
        try {
            Set<GreyListKey> keys = SpamNodeImpl.greylist.keySet();
            logger.info("Saving greylist to file... (" + keys.size() + " entries)");
            LinkedList<GreyListKey> list = new LinkedList<GreyListKey>();
            for ( GreyListKey key : keys ) {
                list.add(key);
            }
            UvmContextFactory.context().settingsManager().save( GREYLIST_SAVE_FILENAME, list, false, false );
            logger.info("Saving greylist to file... done");
        } catch (Exception e) {
            logger.warn("Exception",e);
        }

    }

    private static class GreyListSaver implements Runnable
    {
        public void run()
        {
            SpamNodeImpl.saveGreyList();
        }
    }
    
}
