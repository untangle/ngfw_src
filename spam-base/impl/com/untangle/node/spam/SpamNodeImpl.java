/**
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.node.EventLogQuery;

public class SpamNodeImpl extends AbstractNode implements SpamNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private final RBLEventHandler rblHandler = new RBLEventHandler(this);

    private final SpamRBLHandler spamRblHandler = new SpamRBLHandler();

    // We want to make sure that spam is before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    // Would want the RBL to get evaluated before the casing, this way if it blocks a session
    // the casing doesn't have to be initialized.
    private final PipeSpec[] pipeSpecs = new PipeSpec[]
        {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(this, new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 10),
        new SoloPipeSpec("spam-smtp-rbl", this, this.rblHandler, Fitting.SMTP_STREAM, Affinity.CLIENT, 11),
        new SoloPipeSpec("spam-pop", this, new TokenAdaptor(this, new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 10),
        new SoloPipeSpec("spam-imap", this, new TokenAdaptor(this, new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 10)
    };

    private final SpamScanner scanner;
    private final SpamAssassinDaemon saDaemon;

    private final PartialListUtil listUtil = new PartialListUtil();

    protected volatile SpamSettings spamSettings;

    private final BlingBlinger emailReceivedBlinger;
    private final BlingBlinger spamDetectedBlinger;
    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;
    private final BlingBlinger markBlinger;
    private final BlingBlinger quarantineBlinger;

    private EventLogQuery allEventQuery;
    private EventLogQuery spamEventQuery;
    private EventLogQuery quarantinedEventQuery;
    private EventLogQuery rblEventQuery;
    private EventLogQuery rblSkippedEventQuery;
    
    private String signatureVersion;
    private Date lastUpdate = new Date();
    private Date lastUpdateCheck = new Date();
    private int rblListLength;

    @SuppressWarnings("unchecked")
	public SpamNodeImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
        saDaemon = new SpamAssassinDaemon();

        NodeContext tctx = getNodeContext();

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
                                               "FROM MailLogEventFromReports AS evt" +
                                               " WHERE evt.addrKind IN ('T', 'C')" +
                                               " AND evt." + vendorTag + "Action IS NOT NULL" +
                                               " AND evt.policyId = :policyId" + 
                                               " ORDER BY evt.timeStamp DESC");

        this.spamEventQuery = new EventLogQuery(I18nUtil.marktr("All") + " " + I18nUtil.marktr(badEmailName) + " " + I18nUtil.marktr("Events"),
                                                "FROM MailLogEventFromReports evt" +
                                                " WHERE evt." + vendorTag + "IsSpam IS TRUE" + 
                                                " AND evt.addrKind IN ('T', 'C')" +
                                                " AND evt.policyId = :policyId" + 
                                                " ORDER BY evt.timeStamp DESC");

        this.quarantinedEventQuery = new EventLogQuery(I18nUtil.marktr("Quarantined Events"),
                                                       "FROM MailLogEventFromReports evt" +
                                                       " WHERE evt." + vendorTag + "Action = 'Q'" + 
                                                       " AND evt.addrKind IN ('T', 'C')" +
                                                       " AND evt.policyId = :policyId" + 
                                                       " ORDER BY evt.timeStamp DESC");
                                                       
        
        /* FIXME */
        /* This query comes from events schema not reports as it should! */
        this.rblEventQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                                               "FROM SpamSmtpRblEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC");

        /* FIXME */
        /* This query comes from events schema not reports as it should! */
        this.rblSkippedEventQuery = new EventLogQuery(I18nUtil.marktr("Skipped Events"),
                                                      "FROM SpamSmtpRblEvent evt WHERE evt.skipped = true AND evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp DESC");
        
        MessageManager lmm = UvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Messages passed"), null, I18nUtil.marktr("PASS"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Messages dropped"), null, I18nUtil.marktr("DROP"));
        markBlinger = c.addActivity("mark", I18nUtil.marktr("Messages marked"), null, I18nUtil.marktr("MARK"));
        quarantineBlinger = c.addActivity("quarantine", I18nUtil.marktr("Messages quarantined"), null, I18nUtil.marktr("QUARANTINE"));
        spamDetectedBlinger = c.addMetric("spam", I18nUtil.marktr("Spam detected"), null);
        emailReceivedBlinger = c.addMetric("email", I18nUtil.marktr("Messages received"), null);
        lmm.setActiveMetricsIfNotSet(getNodeId(), passBlinger, blockBlinger, markBlinger, quarantineBlinger);
    }

    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventQuery, this.spamEventQuery, this.quarantinedEventQuery };
    }
    
    public EventLogQuery[] getRBLEventQueries()
    {
        return new EventLogQuery[] { this.rblEventQuery, this.rblSkippedEventQuery };
    }

    /**
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCount() {
        blockBlinger.increment();
        spamDetectedBlinger.increment();
        emailReceivedBlinger.increment();
    }

    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCount() {
        passBlinger.increment();
        emailReceivedBlinger.increment();
    }

    /**
     * Increment the counter for messages marked
     */
    public void incrementMarkCount() {
        markBlinger.increment();
        spamDetectedBlinger.increment();
        emailReceivedBlinger.increment();
    }

    /**
     * Increment the count for messages quarantined.
     */
    public void incrementQuarantineCount() {
        quarantineBlinger.increment();
        spamDetectedBlinger.increment();
        emailReceivedBlinger.increment();
    }

    protected void initSpamRBLList(SpamSettings tmpSpamSettings)
    {
        initSpamRBLList(tmpSpamSettings.getSpamRBLList());
    }

    protected void initSpamRBLList(List<SpamRBL> spamRBLList) {

        if (( null == spamRBLList) || ( false == spamRBLList.isEmpty())) {
            // if already initialized,
            // use list as-is (e.g., database contains final word)
            //
            // if need be, update existing list here
            //
            // on return, save to database
            return;
        } // else initialize list now (e.g., upgrade has just occurred)

        spamRBLList.add(new SpamRBL("zen.spamhaus.org", "Spamhaus SBL, XBL and PBL lists.", true));
        // spamRBLList.add(new SpamRBL("list.dsbl.org", "Distributed Sender Blackhole List", true));
        // spamRBLList.add(new SpamRBL("sbl-xbl.spamhaus.org", "Spamhaus Block and Exploits Block Lists", true));
        // spamRBLList.add(new SpamRBL("bl.spamcop.net", "SpamCop Blocking List", true));

        return;
    }

    protected void configureSpamSettings(SpamSettings tmpSpamSettings) {
        tmpSpamSettings.setSmtpConfig(new SpamSmtpConfig(true,
                SmtpSpamMessageAction.QUARANTINE,
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
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        }

        return this.spamSettings;
    }

    public void setSettings(final SpamSettings newSpamSettings)
    {
        // set lists if not already set
        initSpamRBLList(newSpamSettings);
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

    // AbstractNode methods ----------------------------------------------
    public String getVendor() {
        return "sa";
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void preInit(String args[])
    {
        initializeSettings();
        initSpamRBLList(spamSettings);
    }

    public SpamScanner getScanner()
    {
        return scanner;
    }


    private static class SpamRBLHandler implements PartialListUtil.Handler<SpamRBL>
    {
        public Long getId( SpamRBL rule )
        {
            return rule.getId();
        }

        public void update( SpamRBL current, SpamRBL newRule )
        {
            current.update( newRule );
        }

    }

    // XXX soon to be deprecated ----------------------------------------------

    public int getSpamRBLListLength()
    {
        return rblListLength;
    }

    public void setSpamRBLListLength(int newValue)
    {
        rblListLength = newValue;
    }

    public Date getLastUpdate()
    {
        return lastUpdate;
    }

    public void setLastUpdate(Date newValue)
    {
        lastUpdate = newValue;
    }

    public Date getLastUpdateCheck()
    {
        return lastUpdateCheck;
    }

    public void setLastUpdateCheck(Date newValue)
    {
        lastUpdateCheck = newValue;
    }

    public String getSignatureVersion()
    {
        return signatureVersion;
    }

    public void setSignatureVersion(String newValue)
    {
        signatureVersion = newValue;
    }
}
