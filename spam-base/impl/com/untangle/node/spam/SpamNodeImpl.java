/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;

public class SpamNodeImpl extends NodeBase implements SpamNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String STAT_RECEIVED = "email-received";
    private static final String STAT_SPAM = "spam-detected";
    private static final String STAT_PASS = "pass";
    private static final String STAT_DROP = "drop";
    private static final String STAT_MARK = "mark";
    private static final String STAT_QUARANTINE = "quarantine";
    
    private final TarpitEventHandler tarpitHandler = new TarpitEventHandler(this);

    // We want to make sure that spam is before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    // Would want the DNSBL to get evaluated before the casing, this way if it blocks a session
    // the casing doesn't have to be initialized.
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(this, new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 10),
        new SoloPipeSpec("spam-smtp-tarpit", this, this.tarpitHandler, Fitting.SMTP_STREAM, Affinity.CLIENT, 11),
        new SoloPipeSpec("spam-pop", this, new TokenAdaptor(this, new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 10),
        new SoloPipeSpec("spam-imap", this, new TokenAdaptor(this, new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 10)
    };

    private final SpamScanner scanner;
    private final SpamAssassinDaemon saDaemon;

    protected volatile SpamSettings spamSettings;

    private EventLogQuery allEventQuery;
    private EventLogQuery spamEventQuery;
    private EventLogQuery quarantinedEventQuery;
    private EventLogQuery tarpitEventQuery;
    
    private String signatureVersion;
    private Date lastUpdate = new Date();
    private Date lastUpdateCheck = new Date();

    @SuppressWarnings("unchecked")
	public SpamNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties, SpamScanner scanner )
    {
        super( nodeSettings, nodeProperties );
        
        this.scanner = scanner;
        saDaemon = new SpamAssassinDaemon();

        String vendor = scanner.getVendorName();
        String vendorTag = vendor;
        String badEmailName = "Spam";
        
        if (vendor.equals("SpamAssassin")) 
            vendorTag = "sa";
        else if (vendor.equals("CommtouchAs"))
            vendorTag = "ct";
        else if (vendor.equals("Clam")) {
            vendorTag = "phish";
            badEmailName = "Phish";
        }
        
        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Email Events"),
                                               "SELECT * FROM reports.n_mail_addrs " +
                                               " WHERE addr_kind IN ('T', 'C')" +
                                               " AND " + vendorTag + "_action IS NOT NULL" +
                                               " AND policy_id = :policyId" + 
                                               " ORDER BY time_stamp DESC");
        this.spamEventQuery = new EventLogQuery(I18nUtil.marktr("All") + " " + I18nUtil.marktr(badEmailName) + " " + I18nUtil.marktr("Events"),
                                                "SELECT * FROM reports.n_mail_addrs " +
                                                " WHERE " + vendorTag + "_is_spam IS TRUE" + 
                                                " AND addr_kind IN ('T', 'C')" +
                                                " AND policy_id = :policyId" + 
                                                " ORDER BY time_stamp DESC");
        this.quarantinedEventQuery = new EventLogQuery(I18nUtil.marktr("Quarantined Events"),
                                                       "SELECT * FROM reports.n_mail_addrs " +
                                                       " WHERE " + vendorTag + "_action = 'Q'" + 
                                                       " AND addr_kind IN ('T', 'C')" +
                                                       " AND policy_id = :policyId" + 
                                                       " ORDER BY time_stamp DESC");
        this.tarpitEventQuery = new EventLogQuery(I18nUtil.marktr("Tarpit Events"),
                                                  "SELECT * FROM reports.n_spam_smtp_tarpit_events " +
                                                  "WHERE vendor_name = '" + vendorTag + "' " +
                                                  "AND policy_id = :policyId " +
                                                  "ORDER BY time_stamp DESC");
        
        this.addMetric(new NodeMetric(STAT_RECEIVED, I18nUtil.marktr("Messages received")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Messages passed")));
        this.addMetric(new NodeMetric(STAT_DROP, I18nUtil.marktr("Messages dropped")));
        this.addMetric(new NodeMetric(STAT_MARK, I18nUtil.marktr("Messages marked")));
        this.addMetric(new NodeMetric(STAT_QUARANTINE, I18nUtil.marktr("Messages quarantined")));
        this.addMetric(new NodeMetric(STAT_SPAM, I18nUtil.marktr("Spam detected")));
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
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCount()
    {
        this.incrementMetric(STAT_RECEIVED);
        this.incrementMetric(STAT_DROP);
        this.incrementMetric(STAT_SPAM);
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
        this.incrementMetric(STAT_MARK);
        this.incrementMetric(STAT_SPAM);
    }

    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount()
    {
        this.incrementMetric(STAT_QUARANTINE);
        this.incrementMetric(STAT_SPAM);
        this.incrementMetric(STAT_RECEIVED);
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
                SpamProtoConfig.DEFAULT_STRENGTH,
                SpamProtoConfig.DEFAULT_ADD_SPAM_HEADERS,
                SpamSmtpConfig.DEFAULT_BLOCK_SUPER_SPAM,
                SpamSmtpConfig.DEFAULT_SUPER_STRENGTH,
                SpamSmtpConfig.DEFAULT_FAIL_CLOSED,
                SpamProtoConfig.DEFAULT_HEADER_NAME,
                SpamSmtpConfig.DEFAULT_TARPIT,
                SpamSmtpConfig.DEFAULT_TARPIT_TIMEOUT,
                SpamSmtpConfig.DEFAULT_LIMIT_LOAD,
                SpamSmtpConfig.DEFAULT_LIMIT_SCANS,
                SpamSmtpConfig.DEFAULT_SCAN_WAN_MAIL ));

        tmpSpamSettings.setPopConfig(new SpamPopConfig(true,
                SpamMessageAction.MARK,
                SpamProtoConfig.DEFAULT_STRENGTH,
                SpamProtoConfig.DEFAULT_ADD_SPAM_HEADERS,
                SpamProtoConfig.DEFAULT_HEADER_NAME ));

        tmpSpamSettings.setImapConfig(new SpamImapConfig(true,
                SpamMessageAction.MARK,
                SpamProtoConfig.DEFAULT_STRENGTH,
                SpamProtoConfig.DEFAULT_ADD_SPAM_HEADERS,
                SpamProtoConfig.DEFAULT_HEADER_NAME ));
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

    public void enablePopSpamHeaders(boolean enableHeaders)
    {
        SpamSettings ss = getSettings();
        ss.getPopConfig().setAddSpamHeaders(enableHeaders);
        setSettings(ss);
    }

    public void enableImapSpamHeaders(boolean enableHeaders)
    {
        SpamSettings ss = getSettings();
        ss.getImapConfig().setAddSpamHeaders(enableHeaders);
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

    // convenience method for GUI
    public boolean startSpamAssassinDaemon() {
        return saDaemon.start();
    }

    // convenience method for GUI
    public boolean stopSpamAssassinDaemon() {
        return saDaemon.stop();
    }

    // convenience method for GUI
    public boolean restartSpamAssassinDaemon() {
        return saDaemon.restart();
    }

    // NodeBase methods ----------------------------------------------
    public String getVendor()
    {
        return "sa";
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void preInit()
    {
        initializeSettings();
        initSpamDnsblList(spamSettings);
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
}
