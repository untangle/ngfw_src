/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spam;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import static com.untangle.node.util.Ascii.CRLF;

public class SpamImpl extends AbstractNode implements SpamNode
{
    private final Logger logger = Logger.getLogger(getClass());

    //===============================
    // Defaults for templates

    private static final String MOD_SUB_TEMPLATE =
        "[SPAM] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was determined by the Spam Blocker to be spam based on a score\r\n" +
        "of $SPAMReport:SCORE$ where anything above $SPAMReport:THRESHOLD$ is spam.\r\n";

    private static final String SPAM_HEADER_NAME = "X-Spam-Flag";
    private final static String SPAM_HDR_VALUE = "YES";
    private final static String HAM_HDR_VALUE = "NO";

    private static final String NOTIFY_SUB_TEMPLATE =
        "[SPAM NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF +
        "was received by $SMTPTransaction:TO$.  The message was determined" + CRLF +
        "by the Spam Blocker to be spam based on a score of $SPAMReport:SCORE$ where anything" + CRLF +
        "above $SPAMReport:THRESHOLD$ is spam.  The details of the report are as follows:" + CRLF + CRLF +
        "$SPAMReport:FULL$";

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
    private final EventLogger<SpamSMTPRBLEvent> rblEventLogger;

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
    private String signatureVersion = "";

    // constructors -----------------------------------------------------------

    public SpamImpl(SpamScanner scanner)
    {
        this.scanner = scanner;
        saDaemon = new SpamAssassinDaemon();

        NodeContext tctx = getNodeContext();

        eventLogger = EventLoggerFactory.factory().getEventLogger(tctx);
        rblEventLogger = EventLoggerFactory.factory().getEventLogger(tctx);

        String vendor = scanner.getVendorName();

        SimpleEventFilter ef = new SpamAllFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSpamFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamSmtpFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new SpamLogFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        // filters for RBL events
        ef = new RBLAllFilter();
        rblEventLogger.addSimpleEventFilter(ef);

        ef = new RBLSkippedFilter();
        rblEventLogger.addSimpleEventFilter(ef);

        LocalMessageManager lmm = LocalUvmContextFactory.context().localMessageManager();
        Counters c = lmm.getCounters(getTid());
        passBlinger = c.addActivity("pass", I18nUtil.marktr("Messages passed"), null, I18nUtil.marktr("PASS"));
        blockBlinger = c.addActivity("block", I18nUtil.marktr("Messages blocked"), null, I18nUtil.marktr("BLOCK"));
        markBlinger = c.addActivity("mark", I18nUtil.marktr("Messages marked"), null, I18nUtil.marktr("MARK"));
        quarantineBlinger = c.addActivity("quarantine", I18nUtil.marktr("Messages quarantined"), null, I18nUtil.marktr("QUARANTINE"));
        spamDetectedBlinger = c.addMetric("spam", I18nUtil.marktr("Spam detected"), null);
        emailReceivedBlinger = c.addMetric("email", I18nUtil.marktr("Messages received"), null);
        lmm.setActiveMetricsIfNotSet(getTid(), passBlinger, blockBlinger, markBlinger, quarantineBlinger);
    }

    // Spam methods -----------------------------------------------------------

    public EventManager<SpamEvent> getEventManager()
    {
        return eventLogger;
    }

    public EventManager<SpamSMTPRBLEvent> getRBLEventManager()
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

    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for POP/IMAP wrapped messages
     */
    public String getDefaultSubjectWrapperTemplate() {
        return MOD_SUB_TEMPLATE;
    }

    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for POP/IMAP
     */
    public String getDefaultBodyWrapperTemplate() {
        return MOD_BODY_TEMPLATE;
    }

    /**
     * Method for subclass (currently - clamphish) to define
     * the default Subject template for SMTP wrapped messages
     */
    public String getDefaultSMTPSubjectWrapperTemplate() {
        return getDefaultSubjectWrapperTemplate();
    }

    /**
     * Method for subclass (currently - clamphish) to define
     * the default wrapping body template for SMTP
     */
    public String getDefaultSMTPBodyWrapperTemplate() {
        return MOD_BODY_SMTP_TEMPLATE;
    }

    /**
     * Get the default name of the header added to mails
     * to indicate "spamminess"
     */
    public String getDefaultIndicatorHeaderName() {
        return SPAM_HEADER_NAME;
    }

    /**
     * Get the default value for the {@link #getDefaultIndicatorHeaderName header}
     * indicating if the email is/is not spam
     */
    public String getDefaultIndicatorHeaderValue(boolean isSpam) {
        return isSpam?SPAM_HDR_VALUE:HAM_HDR_VALUE;
    }

    /**
     * Get the default template used for notification messages'
     * subject
     */
    public String getDefaultNotifySubjectTemplate() {
        return NOTIFY_SUB_TEMPLATE;
    }

    /**
     * Get the default template used to create notification
     * messages' bodies
     */
    public String getDefaultNotifyBodyTemplate() {
        return NOTIFY_BODY_TEMPLATE;
    }

    /**
     * The settings for the IMAP/POP/SMTP templates have been added
     * to the Config objects, yet not in the database (9/05).
     * This method makes sure that they are set to the programatic default.
     *
     * Once we move these to the database, this method is obsolete.
     */
    private void ensureTemplateSettings(SpamSettings ss) {
        ensureTemplateSettings(ss.getBaseSettings());
    }

    private void ensureTemplateSettings(SpamBaseSettings sbs) {
        SpamIMAPConfig ic = sbs.getImapConfig();
        ic.setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate());
        ic.setBodyWrapperTemplate(getDefaultBodyWrapperTemplate());
        ic.setHeaderName(getDefaultIndicatorHeaderName());
        ic.setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        ic.setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

        SpamPOPConfig pc = sbs.getPopConfig();
        pc.setSubjectWrapperTemplate(getDefaultSubjectWrapperTemplate());
        pc.setBodyWrapperTemplate(getDefaultBodyWrapperTemplate());
        pc.setHeaderName(getDefaultIndicatorHeaderName());
        pc.setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        pc.setHeaderValue(getDefaultIndicatorHeaderValue(false), false);

        SpamSMTPConfig sc = sbs.getSmtpConfig();
        sc.setSubjectWrapperTemplate(getDefaultSMTPSubjectWrapperTemplate());
        sc.setBodyWrapperTemplate(getDefaultSMTPBodyWrapperTemplate());
        sc.setHeaderName(getDefaultIndicatorHeaderName());
        sc.setHeaderValue(getDefaultIndicatorHeaderValue(true), true);
        sc.setHeaderValue(getDefaultIndicatorHeaderValue(false), false);
        sc.setNotifySubjectTemplate(getDefaultNotifySubjectTemplate());
        sc.setNotifyBodyTemplate(getDefaultNotifyBodyTemplate());
    }

    protected void initSpamRBLList(SpamSettings tmpSpamSettings) {
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
            setSmtpConfig(new SpamSMTPConfig(true,
                                             SMTPSpamMessageAction.QUARANTINE,
                                             SpamSMTPNotifyAction.NEITHER,
                                             SpamProtoConfig.DEFAULT_STRENGTH,
                                             true,
                                             SpamSMTPConfig.DEFAULT_SUPER_STRENGTH,
                                             true,
                                             "Scan SMTP e-mail",
                                             getDefaultSubjectWrapperTemplate(),
                                             getDefaultBodyWrapperTemplate(),
                                             getDefaultIndicatorHeaderName(),
                                             getDefaultIndicatorHeaderValue(true),
                                             getDefaultIndicatorHeaderValue(false),
                                             getDefaultNotifySubjectTemplate(),
                                             getDefaultNotifyBodyTemplate(),
                                             true,
                                             15));

        tmpSpamSettings.getBaseSettings().
            setPopConfig(new SpamPOPConfig(true,
                                           SpamMessageAction.MARK,
                                           SpamProtoConfig.DEFAULT_STRENGTH,
                                           "Scan POP e-mail",
                                           getDefaultSubjectWrapperTemplate(),
                                           getDefaultBodyWrapperTemplate(),
                                           getDefaultIndicatorHeaderName(),
                                           getDefaultIndicatorHeaderValue(true),
                                           getDefaultIndicatorHeaderValue(false) ));

        tmpSpamSettings.getBaseSettings().
            setImapConfig(new SpamIMAPConfig(true,
                                             SpamMessageAction.MARK,
                                             SpamProtoConfig.DEFAULT_STRENGTH,
                                             "Scan IMAP e-mail",
                                             getDefaultSMTPSubjectWrapperTemplate(),
                                             getDefaultSMTPBodyWrapperTemplate(),
                                             getDefaultIndicatorHeaderName(),
                                             getDefaultIndicatorHeaderValue(true),
                                             getDefaultIndicatorHeaderValue(false) ));
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
        //TEMP HACK, Until we move the templates to database
        ensureTemplateSettings(newSpamSettings);
        // set lists if not already set
        initSpamRBLList(newSpamSettings);

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(newSpamSettings);
                    SpamImpl.this.spamSettings = newSpamSettings;

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);

        return;
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
            String signatureVersion = scanner.getSignatureVersion();
            if (signatureVersion != null) this.signatureVersion = signatureVersion;
        }

        baseSettings.setLastUpdate(this.lastSignatureUpdate);
        baseSettings.setSignatureVersion(this.signatureVersion);

        /* XXXX Have to figure out how to calculate the version string. */
        return baseSettings;
    }

    public void setBaseSettings(final SpamBaseSettings baseSettings)
    {
        //TEMP HACK, Until we move the templates to database
        ensureTemplateSettings(baseSettings);

        TransactionWork tw = new TransactionWork() {
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

    public List<SpamRBL> getSpamRBLList( int start, int limit, String ... sortColumns )
    {
        String query = "select s.spamRBLList from SpamSettings s where s.tid = :tid ";
        return listUtil.getItems( query, getNodeContext(), getTid(), start, limit, sortColumns );
    }

    public void updateSpamRBLList( final List<SpamRBL> added, final List<Long> deleted,
                                   final List<SpamRBL> modified )
    {
        TransactionWork<Void> tw = new TransactionWork()
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

    public void updateAll( final SpamBaseSettings baseSettings, final List[] rblRules )
    {
        TransactionWork tw = new TransactionWork() {
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

        SpamSettings tmpSpamSettings = new SpamSettings(getTid());
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
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = getSettingsQuery(s);
                    q.setParameter("tid", getTid());
                    spamSettings = (SpamSettings)q.uniqueResult();

                    ensureTemplateSettings(spamSettings);
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
        Query q = s.createQuery("from SpamSettings ss where ss.tid = :tid");
        q.setParameter("tid", getTid());
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

    void logRBL(SpamSMTPRBLEvent spamSmtpRBLEvent) {
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
