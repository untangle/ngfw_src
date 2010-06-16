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

package com.untangle.node.virus;

import static com.untangle.node.util.Ascii.CRLF;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.node.mail.papi.smtp.SMTPNotifyAction;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.localapi.SessionMatcher;
import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.LocalMessageManager;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.policy.Policy;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.PipelineFoundry;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Virus Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class VirusNodeImpl extends AbstractNode
    implements VirusNode
{
    private static int deployCount = 0;

    //Programatic defaults for the contents
    //of the "message" emails generated as a result
    //of a virus being found (and "notify" being
    //enabled.
    private static final String MOD_SUB_TEMPLATE =
        "[VIRUS] $MIMEMessage:SUBJECT$";

    // OLD
    // private static final String OUT_MOD_BODY_TEMPLATE =
    // "The attached message from $MIMEMessage:FROM$ was found to contain\r\n" +
    // "the virus \"$VirusReport:VIRUS_NAME$\".  The infected portion of the attached email was removed\r\n" +
    // "by Untangle Virus Blocker.\r\n";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Untangle Virus Blocker.\r\n";
    private static final String MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Untangle Virus Blocker.\r\n";

    private static final String NOTIFY_SUB_TEMPLATE =
        "[VIRUS NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF +
        "was received by $SMTPTransaction:TO$.  The message was found" + CRLF +
        "to contain the virus \"$VirusReport:VIRUS_NAME$\"." + CRLF +
        "The infected portion of the message was removed by Untangle Virus Blocker";

    private static final PipelineFoundry FOUNDRY = LocalUvmContextFactory.context()
        .pipelineFoundry();

    private static final int FTP = 0;
    private static final int HTTP = 1;
    private static final int SMTP = 2;
    private static final int POP = 3;

    private final VirusScanner scanner;
    private final EventLogger<VirusEvent> eventLogger;
    private final PipeSpec[] pipeSpecs;
    private final VirusReplacementGenerator replacementGenerator;

    private final Logger logger = Logger.getLogger(VirusNodeImpl.class);

    private final PartialListUtil listUtil = new PartialListUtil();

    /* the signatures are updated at startup, so using new Date() is not that far off. */
    /* Cached in the node in case the base settings lose the values during a save. */
    private Date lastSignatureUpdate = new Date();
    private String signatureVersion = "";

    private VirusSettings settings;

    private final BlingBlinger scanBlinger;
    private final BlingBlinger passBlinger;
    private final BlingBlinger blockBlinger;
    private final BlingBlinger removeBlinger;
    private final BlingBlinger passedInfectedMessageBlinger;

    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher() {
            /* Kill all sessions on ports 20, 21 and 80 */
            public boolean isMatch(Policy sessionPolicy,
                                   com.untangle.uvm.node.IPSessionDesc client,
                                   com.untangle.uvm.node.IPSessionDesc server)
            {
                /* Don't kill any UDP Sessions */
                if (client.protocol()
                    == com.untangle.uvm.node.IPSessionDesc.PROTO_UDP) {
                    return false;
                }

                /* handle sessions with a null policy */
                Policy policy = getPolicy();
                if (null != sessionPolicy && null != policy
                    && !sessionPolicy.equals( policy )) {
                    return false;
                }

                if (testClientPort(client.clientPort())
                    || testServerPort(client.serverPort())
                    || testClientPort(server.clientPort())
                    || testServerPort(server.serverPort())) {
                    return true;
                }

                return false;
            }


            private boolean testClientPort( int clientPort )
            {
                /* FTP responds on port 20 */
                if (clientPort == 20) {
                    return true;
                }
                return false;
            }

            private boolean testServerPort( int serverPort )
            {
                /* FTP server is on 21, HTTP server is on 80 */
                if (serverPort == 21 || serverPort == 80 || serverPort == 20) {
                    return true;
                }

                /* email SMTP (25) / POP3 (110) / IMAP (143) */
                if (serverPort == 25 || serverPort == 110
                    || serverPort == 143) {
                    return true;
                }

                return false;
            }
        };

    // constructors -----------------------------------------------------------

    public VirusNodeImpl(VirusScanner scanner)
    {
        this.scanner = scanner;
        this.pipeSpecs = initialPipeSpecs();

        this.replacementGenerator = new VirusReplacementGenerator(getTid());

        NodeContext tctx = getNodeContext();
        eventLogger = EventLoggerFactory.factory()
            .getEventLogger(getNodeContext());

        String vendor = scanner.getVendorName();

        SimpleEventFilter ef = new VirusAllFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new VirusInfectedFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new VirusHttpFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new VirusLogFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new VirusMailFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        ef = new VirusSmtpFilter(vendor);
        eventLogger.addSimpleEventFilter(ef);

        LocalMessageManager lmm = LocalUvmContextFactory.context()
            .localMessageManager();
        Counters c = lmm.getCounters(getTid());
        scanBlinger = c.addActivity("scan",
                                    I18nUtil.marktr("Documents scanned"), null,
                                    I18nUtil.marktr("SCAN"));
        blockBlinger = c.addActivity("block",
                                     I18nUtil.marktr("Documents blocked"),
                                     null, I18nUtil.marktr("BLOCK"));
        passBlinger = c.addActivity("pass",
                                    I18nUtil.marktr("Documents passed"),
                                    null, I18nUtil.marktr("PASS"));
        removeBlinger = c.addActivity("remove",
                                      I18nUtil.marktr("Infections removed"),
                                      null, I18nUtil.marktr("REMOVE"));
        passedInfectedMessageBlinger = c.addMetric("infected",
                                                   I18nUtil.marktr("Passed by policy"),
                                                   null);
        lmm.setActiveMetricsIfNotSet(getTid(), scanBlinger, blockBlinger,
                                     passBlinger, removeBlinger);
    }

    // VirusNode methods -------------------------------------------------
    public VirusBaseSettings getBaseSettings()
    {
        return getBaseSettings(false);
    }

    public VirusBaseSettings getBaseSettings(boolean updateScannerInfo)
    {
        VirusBaseSettings baseSettings = settings.getBaseSettings();

        if (updateScannerInfo) {
            Date lastSignatureUpdate = scanner.getLastSignatureUpdate();
            if (lastSignatureUpdate != null) {
                this.lastSignatureUpdate = lastSignatureUpdate;
            }
            String signatureVersion = getSigVersion();
            if (signatureVersion != null) {
                this.signatureVersion = signatureVersion;
            }
        }

        baseSettings.setLastUpdate(this.lastSignatureUpdate);
        baseSettings.setSignatureVersion(this.signatureVersion);

        return baseSettings;
    }

    public void setBaseSettings(final VirusBaseSettings baseSettings)
    {
        TransactionWork tw = new TransactionWork() {
                public boolean doWork(Session s) {
                    settings.setBaseSettings(baseSettings);
                    s.merge(settings);
                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);
    }

    public void setVirusSettings(final VirusSettings settings)
    {
        //TEMP hack - bscott
        ensureTemplateSettings(settings.getBaseSettings());

        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    s.merge(settings);
                    VirusNodeImpl.this.settings = settings;

                    reconfigure();

                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
        shutdownMatchingSessions();
    }

    public VirusSettings getVirusSettings()
    {
        if( settings == null )
            logger.error("Settings not yet initialized. State: " + getNodeContext().getRunState() );
        return settings;
    }

    public List<MimeTypeRule> getHttpMimeTypes(final int start, final int limit, final String... sortColumns) {
        return listUtil.getItems("select vs.httpMimeTypes from VirusSettings vs " +
                                 "join vs.httpMimeTypes as httpMimeTypes where vs.tid = :tid ",
                                 getNodeContext(), getTid(),"httpMimeTypes", start, limit, sortColumns);
    }

    public void updateHttpMimeTypes(List<MimeTypeRule> added, List<Long> deleted, List<MimeTypeRule> modified) {

        updateRules(getHttpMimeTypes(), added, deleted, modified);
    }

    public List<StringRule> getExtensions(final int start, final int limit, final String... sortColumns) {
        return listUtil.getItems("select vs.extensions from VirusSettings vs where vs.tid = :tid ",
                                 getNodeContext(), getTid(), start, limit, sortColumns);
    }

    public void updateExtensions(List<StringRule> added, List<Long> deleted, List<StringRule> modified) {

        updateRules(getExtensions(), added, deleted, modified);
    }

    public void updateAll(final VirusBaseSettings baseSettings,
                          final List[] httpMimeTypes, final List[] extensions) {

        TransactionWork tw = new TransactionWork() {
                public boolean doWork(Session s) {
                    if (baseSettings != null) {
                        settings.setBaseSettings(baseSettings);
                    }
                    listUtil.updateCachedItems(getHttpMimeTypes(), httpMimeTypes );

                    listUtil.updateCachedItems(getExtensions(), extensions );

                    settings = (VirusSettings)s.merge(settings);

                    return true;
                }

                public Object getResult() {
                    return null;
                }
            };
        getNodeContext().runTransaction(tw);

        reconfigure();
    }

    public String getSigVersion()
    {
        return this.scanner.getSigVersion();
    }

    public EventManager<VirusEvent> getEventManager()
    {
        return eventLogger;
    }

    public VirusBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    String generateNonce(VirusBlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    Token[] generateResponse(String nonce, TCPSession session,
                             String uri,boolean persistent)
    {
        return replacementGenerator.generateResponse(nonce, session, uri,
                                                     null, persistent);

    }

    abstract protected int getStrength();

    // Node methods ------------------------------------------------------

    private PipeSpec[] initialPipeSpecs()
    {
        int strength = getStrength();
        PipeSpec[] result = new PipeSpec[] {
            new SoloPipeSpec("virus-ftp", this,
                             new TokenAdaptor(this, new VirusFtpFactory(this)),
                             Fitting.FTP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-http", this,
                             new TokenAdaptor(this, new VirusHttpFactory(this)),
                             Fitting.HTTP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-smtp", this,
                             new TokenAdaptor(this, new VirusSmtpFactory(this)),
                             Fitting.SMTP_TOKENS, Affinity.CLIENT, strength),
            new SoloPipeSpec("virus-pop", this,
                             new TokenAdaptor(this, new VirusPopFactory(this)),
                             Fitting.POP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-imap", this,
                             new TokenAdaptor(this, new VirusImapFactory(this)),
                             Fitting.IMAP_TOKENS, Affinity.SERVER, strength)
        };
        return result;
    }

    public void reconfigure()
    {
        // FTP
        Set<Subscription> subscriptions = new HashSet<Subscription>();
        {
            Subscription subscription = new Subscription(Protocol.TCP);
            subscriptions.add(subscription);
        }
        pipeSpecs[FTP].setSubscriptions(subscriptions);

        // HTTP
        subscriptions = new HashSet<Subscription>();
        if (settings.getBaseSettings().getHttpConfig().getScan()) {
            Subscription subscription = new Subscription(Protocol.TCP);
            subscriptions.add(subscription);
        }
        pipeSpecs[HTTP].setSubscriptions(subscriptions);

        // SMTP
        subscriptions = new HashSet<Subscription>();
        {
            Subscription subscription = new Subscription(Protocol.TCP);
            subscriptions.add(subscription);
        }
        pipeSpecs[SMTP].setSubscriptions(subscriptions);

        // POP
        subscriptions = new HashSet<Subscription>();
        {
            Subscription subscription = new Subscription(Protocol.TCP);
            subscriptions.add(subscription);
        }
        pipeSpecs[POP].setSubscriptions(subscriptions);
    }



    // AbstractNode methods ----------------------------------------------

    @Override
        protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    /**
     * The settings for the IMAP/POP/SMTP
     * templates have been added to the
     * Config objects, yet not in the database
     * (9/05).  This method makes sure that
     * they are set to the programatic
     * default.
     *
     * Once we move these to the database,
     * this method is obsolete.
     */
    private void ensureTemplateSettings(VirusBaseSettings vs) {
        vs.getImapConfig().setSubjectWrapperTemplate(MOD_SUB_TEMPLATE);
        vs.getImapConfig().setBodyWrapperTemplate(MOD_BODY_TEMPLATE);
        vs.getImapConfig().setBodyWrapperTemplate(MOD_BODY_TEMPLATE);

        vs.getPopConfig().setSubjectWrapperTemplate(MOD_SUB_TEMPLATE);
        vs.getPopConfig().setBodyWrapperTemplate(MOD_BODY_TEMPLATE);

        vs.getSmtpConfig().setSubjectWrapperTemplate(MOD_SUB_TEMPLATE);
        vs.getSmtpConfig().setBodyWrapperTemplate(MOD_BODY_SMTP_TEMPLATE);

        vs.getSmtpConfig().setNotifySubjectTemplate(NOTIFY_SUB_TEMPLATE);
        vs.getSmtpConfig().setNotifyBodyTemplate(NOTIFY_BODY_TEMPLATE);
    }

    public void initializeSettings()
    {
        VirusSettings vs = new VirusSettings(getTid());
        VirusBaseSettings baseSettings = vs.getBaseSettings();
        baseSettings.setHttpConfig(new VirusConfig(true, true, "Scan HTTP files"));
        baseSettings.setFtpConfig(new VirusConfig(true, true, "Scan FTP files" ));

        baseSettings.setSmtpConfig(new VirusSMTPConfig(true,
                                                       SMTPVirusMessageAction.REMOVE,
                                                       SMTPNotifyAction.NEITHER,
                                                       "Scan SMTP e-mail",
                                                       MOD_SUB_TEMPLATE,
                                                       MOD_BODY_SMTP_TEMPLATE,
                                                       NOTIFY_SUB_TEMPLATE,
                                                       NOTIFY_BODY_TEMPLATE));

        baseSettings.setPopConfig(new VirusPOPConfig(true,
                                                     VirusMessageAction.REMOVE,
                                                     "Scan POP e-mail",
                                                     MOD_SUB_TEMPLATE,
                                                     MOD_BODY_TEMPLATE));

        baseSettings.setImapConfig(new VirusIMAPConfig(true,
                                                       VirusMessageAction.REMOVE,
                                                       "Scan IMAP e-mail",
                                                       MOD_SUB_TEMPLATE,
                                                       MOD_BODY_TEMPLATE));

        initMimeTypes(vs);
        initFileExtensions(vs);

        setVirusSettings(vs);
    }

    private void initMimeTypes(VirusSettings vs)
    {
        Set s = new HashSet<MimeTypeRule>();
        s.add(new MimeTypeRule(new MimeType("message/*"), "messages", "misc", true));

        vs.setHttpMimeTypes(s);
    }

    private void initFileExtensions(VirusSettings vs)
    {
        Set s = new HashSet<StringRule>();
        /* XXX Need a description here */
        // Note that category is unused in the UI
        s.add(new StringRule("exe", "executable", "download" , true));
        s.add(new StringRule("com", "executable", "download", true));
        s.add(new StringRule("ocx", "executable", "ActiveX", true));
        s.add(new StringRule("dll", "executable", "ActiveX", false));
        s.add(new StringRule("cab", "executable", "ActiveX", true));
        s.add(new StringRule("bin", "executable", "download", true));
        s.add(new StringRule("bat", "executable", "download", true));
        s.add(new StringRule("pif", "executable", "download" , true));
        s.add(new StringRule("scr", "executable", "download" , true));
        s.add(new StringRule("cpl", "executable", "download" , true));
        s.add(new StringRule("hta", "executable", "download" , true));
        s.add(new StringRule("vb",  "script", "download" , true));
        s.add(new StringRule("vbe", "script", "download" , true));
        s.add(new StringRule("vbs", "script", "download" , true));
        s.add(new StringRule("zip", "archive", "download" , true));
        s.add(new StringRule("eml", "archive", "download" , true));
        s.add(new StringRule("hqx", "archive", "download", true));
        s.add(new StringRule("rar", "archive", "download" , true));
        s.add(new StringRule("arj", "archive", "download" , true));
        s.add(new StringRule("ace", "archive", "download" , true));
        s.add(new StringRule("gz",  "archive", "download" , true));
        s.add(new StringRule("tar", "archive", "download" , true));
        s.add(new StringRule("tgz", "archive", "download" , true));
        s.add(new StringRule("doc", "document", "document", false));
        s.add(new StringRule("ppt", "presentation", "document", false));
        s.add(new StringRule("xls", "spreadsheet", "document", false));
        s.add(new StringRule("mp3", "audio", "download", false));
        s.add(new StringRule("wav", "audio", "download", false));
        s.add(new StringRule("wmf", "audio", "download", false));
        s.add(new StringRule("mov", "video", "download", false));
        s.add(new StringRule("mpg", "video", "download", false));
        s.add(new StringRule("avi", "video", "download", false));
        s.add(new StringRule("swf", "flash", "download", false));
        s.add(new StringRule("jar",   "java", "download", false));
        s.add(new StringRule("class", "java", "download", false));
        vs.setExtensions(s);
    }


    protected void preInit(String args[])
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery
                        ("from VirusSettings vs where vs.tid = :tid");
                    q.setParameter("tid", getTid());
                    settings = (VirusSettings)q.uniqueResult();

                    boolean changed = false;
                    if (settings.getHttpMimeTypes() == null || settings.getHttpMimeTypes().isEmpty()) {
                        initMimeTypes(settings);
                        changed = true;
                    }
                    if (settings.getExtensions() == null || settings.getExtensions().isEmpty()) {
                        initFileExtensions(settings);
                        changed = true;
                    }
                    ensureTemplateSettings(settings.getBaseSettings());
                    if (changed) {
                        s.merge(settings);
                    }
                    return true;
                }

                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }

    protected void preStart()
    {
        reconfigure();
    }

    protected void postStart()
    {
        shutdownMatchingSessions();
    }

    @Override
        protected void postInit(String[] args)
    {
        deployWebAppIfRequired(logger);
    }

    @Override
        protected void postDestroy()
    {
        unDeployWebAppIfRequired(logger);
    }

    // package protected methods ----------------------------------------------

    VirusScanner getScanner()
    {
        return scanner;
    }

    int getTricklePercent()
    {
        return settings.getBaseSettings().getTricklePercent();
    }

    Set getExtensions()
    {
        return settings.getExtensions();
    }

    Set getHttpMimeTypes()
    {
        return settings.getHttpMimeTypes();
    }

    boolean getFtpDisableResume()
    {
        return settings.getBaseSettings().getFtpDisableResume();
    }

    boolean getHttpDisableResume()
    {
        return settings.getBaseSettings().getHttpDisableResume();
    }

    void log(VirusEvent evt)
    {
        eventLogger.log(evt);
    }

    protected SessionMatcher sessionMatcher()
    {
        return VIRUS_SESSION_MATCHER;
    }

    /**
     * Increment the counter for messages scanned
     */
    public void incrementScanCount()
    {
        scanBlinger.increment();
    }

    /**
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCount()
    {
        blockBlinger.increment();
    }

    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCount()
    {
        passBlinger.increment();
    }

    /**
     * Increment the counter for messages where we
     * removed a virus
     */
    public void incrementRemoveCount()
    {
        removeBlinger.increment();
    }

    /**
     * Increment the counter for messages where we
     * found a firus but passed it on due to a rule
     */
    public void incrementPassedInfectedMessageCount()
    {
        passedInfectedMessageBlinger.increment();
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        Valve v = new OutsideValve()
            {
                protected boolean isInsecureAccessAllowed()
                {
                    return true;
                }

                /* Unified way to determine which parameter to check */
                protected boolean isOutsideAccessAllowed()
                {
                    return false;
                }
            };

        if (null != asm.loadInsecureApp("/virus", "virus", v)) {
            logger.debug("Deployed Virus WebApp");
        } else {
            logger.error("Unable to deploy Virus WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        LocalUvmContext mctx = LocalUvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        if (asm.unloadWebApp("/virus")) {
            logger.debug("Unloaded Virus WebApp");
        } else {
            logger.warn("Unable to unload Virus WebApp");
        }
    }

    // private methods --------------------------------------------------------
    private void updateRules(final Set rules, final List added,
                             final List<Long> deleted, final List modified)
    {
        TransactionWork tw = new TransactionWork()
            {
                public boolean doWork(Session s)
                {
                    listUtil.updateCachedItems(rules, added, deleted, modified);

                    settings = (VirusSettings)s.merge(settings);

                    return true;
                }


                public Object getResult() { return null; }
            };
        getNodeContext().runTransaction(tw);
    }
}
