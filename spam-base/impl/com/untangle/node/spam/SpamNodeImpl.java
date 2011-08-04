/*
 * $Id$
 */
package com.untangle.node.spam;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.SimpleEventFilter;
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

public class SpamNodeImpl extends AbstractNode implements SpamNode
{
    private final Logger logger = Logger.getLogger(getClass());

    private final RBLEventHandler rblHandler = new RBLEventHandler(this);

    private final SpamRBLHandler spamRblHandler = new SpamRBLHandler();

    // We want to make sure that spam is before virus in the pipeline (towards the client for smtp,
    // server for pop/imap).
    // Would want the RBL to get evaluated before the casing, this way if it blocks a session
    // the casing doesn't have to be initialized.
    private final PipeSpec[] pipeSpecs = new PipeSpec[] {
        new SoloPipeSpec("spam-smtp", this, new TokenAdaptor(this, new SpamSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, 10),
        new SoloPipeSpec("spam-smtp-rbl", this, this.rblHandler, Fitting.SMTP_STREAM, Affinity.CLIENT, 11),
        new SoloPipeSpec("spam-pop", this, new TokenAdaptor(this, new SpamPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, 10),
        new SoloPipeSpec("spam-imap", this, new TokenAdaptor(this, new SpamImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, 10)
    };

    private final SpamScanner scanner;
    private final SpamAssassinDaemon saDaemon;
    private final EventLogger<SpamEvent> eventLogger;
    private final EventLogger<SpamSmtpRblEvent> rblEventLogger;

    private final PartialListUtil listUtil = new PartialListUtil();

    private volatile SpamSettings spamSettings;

    private final BlingBlinger emailReceivedBlinger;
    private final BlingBlinger spamDetectedBlinger;
    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;
    private final BlingBlinger markBlinger;
    private final BlingBlinger quarantineBlinger;

    /* Cached in the node in case the base settings lose the values during a save. */
    private Date lastSignatureUpdate = new Date();
    private Date lastSignatureUpdateCheck = new Date();
    private String signatureVersion = "";

    // constructors -----------------------------------------------------------

    @SuppressWarnings("unchecked")
	public SpamNodeImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
        saDaemon = new SpamAssassinDaemon();

        NodeContext tctx = getNodeContext();

        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        rblEventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        String vendor = scanner.getVendorName();

        ListEventFilter lef = new SpamAllFilter(vendor);
        eventLogger.addListEventFilter(lef);

        lef = new SpamSpamFilter(vendor);
        eventLogger.addListEventFilter(lef);

        lef = new SpamLogFilter(vendor);
        eventLogger.addListEventFilter(lef);

        // filters for RBL events
        // FIXME: no equivalent info in reports tables
        SimpleEventFilter ef = new RBLAllFilter();
        rblEventLogger.addSimpleEventFilter(ef);

        ef = new RBLSkippedFilter();
        rblEventLogger.addSimpleEventFilter(ef);

        MessageManager lmm = LocalUvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeId());
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Messages passed"), null, I18nUtil.marktr("PASS"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Messages dropped"), null, I18nUtil.marktr("DROP"));
        markBlinger = c.addActivity("mark", I18nUtil.marktr("Messages marked"), null, I18nUtil.marktr("MARK"));
        quarantineBlinger = c.addActivity("quarantine", I18nUtil.marktr("Messages quarantined"), null, I18nUtil.marktr("QUARANTINE"));
        spamDetectedBlinger = c.addMetric("spam", I18nUtil.marktr("Spam detected"), null);
        emailReceivedBlinger = c.addMetric("email", I18nUtil.marktr("Messages received"), null);
        lmm.setActiveMetricsIfNotSet(getNodeId(), passBlinger, blockBlinger, markBlinger, quarantineBlinger);
    }

    // Spam methods -----------------------------------------------------------

    public EventManager<SpamEvent> getEventManager()
    {
        return eventLogger;
    }

    public EventManager<SpamSmtpRblEvent> getRBLEventManager()
    {
        return rblEventLogger;
    }

    // Node methods ------------------------------------------------------

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
        tmpSpamSettings.getBaseSettings().
            setSmtpConfig(new SpamSmtpConfig(true,
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

        tmpSpamSettings.getBaseSettings().
            setPopConfig(new SpamPopConfig(true,
                                           SpamMessageAction.MARK,
                                           SpamProtoConfig.DEFAULT_STRENGTH,
                                           SpamProtoConfig.DEFAULT_ADD_SPAM_HEADERS,
                                           SpamProtoConfig.DEFAULT_HEADER_NAME ));

        tmpSpamSettings.getBaseSettings().
            setImapConfig(new SpamImapConfig(true,
                                             SpamMessageAction.MARK,
                                             SpamProtoConfig.DEFAULT_STRENGTH,
                                             SpamProtoConfig.DEFAULT_ADD_SPAM_HEADERS,
                                             SpamProtoConfig.DEFAULT_HEADER_NAME ));
    }

    public SpamSettings getSpamSettings()
    {
        if( this.spamSettings == null ) {
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        }

        return this.spamSettings;
    }

    public void setSpamSettings(final SpamSettings newSpamSettings)
    {
        // set lists if not already set
        initSpamRBLList(newSpamSettings);

        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    s.merge(newSpamSettings);
                    SpamNodeImpl.this.spamSettings = newSpamSettings;

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return;
    }

    public void enableSmtpSpamHeaders(boolean enableHeaders)
    {
        SpamBaseSettings sbs = getBaseSettings(false);
        sbs.getSmtpConfig().setAddSpamHeaders(enableHeaders);
        setBaseSettings(sbs);
    }

    public void enablePopSpamHeaders(boolean enableHeaders)
    {
        SpamBaseSettings sbs = getBaseSettings(false);
        sbs.getPopConfig().setAddSpamHeaders(enableHeaders);
        setBaseSettings(sbs);
    }

    public void enableImapSpamHeaders(boolean enableHeaders)
    {
        SpamBaseSettings sbs = getBaseSettings(false);
        sbs.getImapConfig().setAddSpamHeaders(enableHeaders);
        setBaseSettings(sbs);
    }

    public void enableSmtpFailClosed(boolean failClosed)
    {
        SpamBaseSettings sbs = getBaseSettings(false);
        sbs.getSmtpConfig().setFailClosed(failClosed);
        setBaseSettings(sbs);
    }

    public SpamBaseSettings getBaseSettings()
    {
        return getBaseSettings(false);
    }

    public SpamBaseSettings getBaseSettings(boolean updateScannerInfo)
    {
        SpamBaseSettings baseSettings = spamSettings.getBaseSettings();

        if (updateScannerInfo) {
            Date lastSignatureUpdate = scanner.getLastSignatureUpdate();
            if (lastSignatureUpdate != null) this.lastSignatureUpdate = lastSignatureUpdate;
            Date lastSignatureUpdateCheck = scanner.getLastSignatureUpdateCheck();
            if (lastSignatureUpdateCheck != null) this.lastSignatureUpdateCheck = lastSignatureUpdateCheck;
            String signatureVersion = scanner.getSignatureVersion();
            if (signatureVersion != null) this.signatureVersion = signatureVersion;
        }

        baseSettings.setLastUpdate(this.lastSignatureUpdate);
        baseSettings.setLastUpdateCheck(this.lastSignatureUpdateCheck);
        baseSettings.setSignatureVersion(this.signatureVersion);

        /* XXXX Have to figure out how to calculate the version string. */
        return baseSettings;
    }

    public void setBaseSettings(final SpamBaseSettings baseSettings)
    {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
                public boolean doWork(Session s) {
                    spamSettings.setBaseSettings(baseSettings);
                    s.merge(spamSettings);
                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);
    }

    @SuppressWarnings("unchecked") //getItems
    public List<SpamRBL> getSpamRBLList( int start, int limit, String ... sortColumns )
    {
        String query = "select s.spamRBLList from SpamSettings s where s.nodeId = :nodeId ";
        return listUtil.getItems( query, getNodeContext(), getNodeId(), start, limit, sortColumns );
    }

    public void updateSpamRBLList( final List<SpamRBL> added, final List<Long> deleted,
                                   final List<SpamRBL> modified )
    {
        TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    List<SpamRBL> rblList = getSpamSettings().getSpamRBLList();
                    listUtil.updateCachedItems( rblList, spamRblHandler, added, deleted, modified);
                    spamSettings = (SpamSettings)s.merge(spamSettings);

                    return true;
                }

                public Void getResult() { return null; }
            };

        getNodeContext().runTransaction( tw );

    }

    @SuppressWarnings("unchecked")
	public void updateAll( final SpamBaseSettings baseSettings, final List[] rblRules )
    {
        TransactionWork<Object> tw = new TransactionWork<Object>() {
                public boolean doWork(Session s) {
                    if (baseSettings != null) {
                        spamSettings.setBaseSettings(baseSettings);
                    }
                    if (rblRules != null && rblRules.length >= 3) {
                        List<SpamRBL> rblList = new LinkedList<SpamRBL>( getSpamSettings().getSpamRBLList());
                        listUtil.updateCachedItems( rblList, spamRblHandler, rblRules );
                        // spamSettings.setSpamRBLList( rblList );
                    }

                    spamSettings = (SpamSettings)s.merge(spamSettings);

                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);
    }

    public void initializeSettings()
    {
        logger.debug("Initializing Settings");

        SpamSettings tmpSpamSettings = new SpamSettings(getNodeId());
        configureSpamSettings(tmpSpamSettings);
        setSpamSettings(tmpSpamSettings);
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

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    @Override
    protected void preInit(String args[])
    {
        TransactionWork<Object> tw = new TransactionWork<Object>()
            {
                public boolean doWork(Session s)
                {
                    Query q = getSettingsQuery(s);
                    q.setParameter("nodeId", getNodeId());
                    spamSettings = (SpamSettings)q.uniqueResult();

                    // set lists if not already set
                    initSpamRBLList(spamSettings);

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return;
    }

    protected Query getSettingsQuery(Session s)
    {
        Query q = s.createQuery("from SpamSettings ss where ss.nodeId = :nodeId");
        q.setParameter("nodeId", getNodeId());
        return q;
    }

    public SpamScanner getScanner()
    {
        return scanner;
    }

    void log(SpamEvent se)
    {
        eventLogger.log(se);
    }

    void logRBL(SpamSmtpRblEvent spamSmtpRBLEvent) {
        rblEventLogger.log(spamSmtpRBLEvent);
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

    public Object getSettings()
    {
        return getSpamSettings();
    }

    public void setSettings(Object settings)
    {
        setSpamSettings((SpamSettings)settings);
        return;
    }

}
