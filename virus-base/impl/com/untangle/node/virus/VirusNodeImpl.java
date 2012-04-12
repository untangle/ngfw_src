/*
 * $Id$
 */
package com.untangle.node.virus;

import static com.untangle.node.util.Ascii.CRLF;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.apache.catalina.Valve;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.json.JSONString;

import com.untangle.uvm.SettingsManager;
import com.untangle.node.mail.papi.smtp.SMTPNotifyAction;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.util.PartialListUtil;
import com.untangle.uvm.LocalAppServerManager;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.message.BlingBlinger;
import com.untangle.uvm.message.Counters;
import com.untangle.uvm.message.MessageManager;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.OutsideValve;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.vnet.AbstractNode;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
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
public abstract class VirusNodeImpl extends AbstractNode implements VirusNode
{
    private static final String SETTINGS_CONVERSION_SCRIPT = System.getProperty( "uvm.bin.dir" ) + "/virus-base-convert-settings.py";

    private static final int TRICKLE_RATE = 90;
    
    private static final String MOD_SUB_TEMPLATE =
        "[VIRUS] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Virus Blocker.\r\n";

    private static final String MOD_BODY_SMTP_TEMPLATE =
        "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" +
        "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n"+
        "The infected portion of the message was removed by Virus Blocker.\r\n";

    private static final String NOTIFY_SUB_TEMPLATE =
        "[VIRUS NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String NOTIFY_BODY_TEMPLATE =
        "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF +
        "was received by $SMTPTransaction:TO$.  The message was found" + CRLF +
        "to contain the virus \"$VirusReport:VIRUS_NAME$\"." + CRLF +
        "The infected portion of the message was removed by Virus Blocker";

    private static final int FTP = 0;
    private static final int HTTP = 1;
    private static final int SMTP = 2;
    private static final int POP = 3;

    private static int deployCount = 0;
    
    private final VirusScanner scanner;
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

    private EventLogQuery httpInfectedEventQuery;
    private EventLogQuery httpCleanEventQuery;
    private EventLogQuery mailInfectedEventQuery;
    private EventLogQuery mailCleanEventQuery;
    
    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher() {
            /* Kill all sessions on ports 20, 21 and 80 */
            public boolean isMatch(Long sessionPolicyId,
                                   com.untangle.uvm.node.IPSessionDesc client,
                                   com.untangle.uvm.node.IPSessionDesc server)
            {
                /* Don't kill any UDP Sessions */
                if (client.protocol() == com.untangle.uvm.node.IPSessionDesc.PROTO_UDP) {
                    return false;
                }

                /* handle sessions with a null policy */
                Long policyId = getPolicyId();
                if (null != sessionPolicyId && null != policyId && !sessionPolicyId.equals( policyId )) {
                    return false;
                }

                if (testServerPort(client.serverPort()) || testServerPort(server.serverPort())) {
                    return true;
                }

                return false;
            }

            private boolean testServerPort( int serverPort )
            {
                /* FTP server is on 21, HTTP server is on 80 */
                if (serverPort == 21 || serverPort == 80) {
                    return true;
                }

                /* email SMTP (25) / POP3 (110) / IMAP (143) */
                if (serverPort == 25 || serverPort == 110 || serverPort == 143) {
                    return true;
                }

                return false;
            }
        };

    // constructors -----------------------------------------------------------

    @SuppressWarnings("unchecked")
	public VirusNodeImpl(VirusScanner scanner)
    {
        this.scanner = scanner;
        this.pipeSpecs = initialPipeSpecs();
        this.replacementGenerator = new VirusReplacementGenerator(getNodeSettings());

        String vendor = scanner.getVendorName();

        this.httpInfectedEventQuery = new EventLogQuery(I18nUtil.marktr("Infected Web Events"),
                                                        "FROM HttpLogEventFromReports evt" + 
                                                        " WHERE evt.virus" + vendor + "Clean IS FALSE" + 
                                                        " AND evt.policyId = :policyId" + 
                                                        " ORDER BY evt.timeStamp DESC");
        this.httpCleanEventQuery = new EventLogQuery(I18nUtil.marktr("Clean Web Events"),
                                                     "FROM HttpLogEventFromReports evt" + 
                                                     " WHERE evt.virus" + vendor + "Clean IS TRUE" + 
                                                     " AND evt.policyId = :policyId" + 
                                                     " ORDER BY evt.timeStamp DESC");
        this.mailInfectedEventQuery = new EventLogQuery(I18nUtil.marktr("Infected Email Events"),
                                                        "FROM MailLogEventFromReports evt" + 
                                                        " WHERE evt.addrKind IN ('T', 'C')" +
                                                        " AND evt.virus" + vendor + "Clean IS FALSE" + 
                                                        " AND evt.policyId = :policyId" + 
                                                        " ORDER BY evt.timeStamp DESC");
        this.mailCleanEventQuery = new EventLogQuery(I18nUtil.marktr("Clean Email Events"),
                                                     "FROM MailLogEventFromReports evt" + 
                                                     " WHERE evt.addrKind IN ('T', 'C')" +
                                                     " AND evt.virus" + vendor + "Clean IS TRUE" + 
                                                     " AND evt.policyId = :policyId" + 
                                                     " ORDER BY evt.timeStamp DESC");

        MessageManager lmm = UvmContextFactory.context().messageManager();
        Counters c = lmm.getCounters(getNodeSettings().getId());
        scanBlinger   = c.addActivity("scan",   I18nUtil.marktr("Documents scanned"),  null, I18nUtil.marktr("SCAN"));
        blockBlinger  = c.addActivity("block",  I18nUtil.marktr("Documents blocked"),  null, I18nUtil.marktr("BLOCK"));
        passBlinger   = c.addActivity("pass",   I18nUtil.marktr("Documents passed"),   null, I18nUtil.marktr("PASS"));
        removeBlinger = c.addActivity("remove", I18nUtil.marktr("Infections removed"), null, I18nUtil.marktr("REMOVE"));
        passedInfectedMessageBlinger = c.addMetric("infected", I18nUtil.marktr("Passed by policy"), null);

        lmm.setActiveMetrics(getNodeSettings().getId(), scanBlinger, blockBlinger, passBlinger, removeBlinger);
    }

    // VirusNode methods -------------------------------------------------
    public VirusSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings( VirusSettings newSettings )
    {
        _setSettings(newSettings);
    }

    public void setHttpFileExtensions(List<GenericRule> fileExtensions)
    {
        settings.setHttpFileExtensions(fileExtensions);
        _setSettings(settings);
    }

    public void setHttpMimeTypes(List<GenericRule> fileExtensions)
    {
        settings.setHttpMimeTypes(fileExtensions);
        _setSettings(settings);
    }

    public EventLogQuery[] getWebEventQueries()
    {
        return new EventLogQuery[] { this.httpInfectedEventQuery, this.httpCleanEventQuery };
    }
    
    public EventLogQuery[] getMailEventQueries()
    {
        return new EventLogQuery[] { this.mailInfectedEventQuery, this.mailCleanEventQuery };
    }

    public VirusBlockDetails getDetails( String nonce )
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public String generateNonce( VirusBlockDetails details )
    {
        return replacementGenerator.generateNonce(details);
    }

    public Token[] generateResponse( String nonce, TCPSession session, String uri, boolean persistent )
    {
        return replacementGenerator.generateResponse(nonce, session, uri, null, persistent);
    }

    public Date getLastSignatureUpdate()
    {
        return scanner.getLastSignatureUpdate();
    }

    abstract protected int getStrength();

    abstract public String getName();

    // Node methods ------------------------------------------------------

    private PipeSpec[] initialPipeSpecs()
    {
        int strength = getStrength();
        PipeSpec[] result = new PipeSpec[] {
            new SoloPipeSpec("virus-ftp", this, new TokenAdaptor(this, new VirusFtpFactory(this)), Fitting.FTP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-http", this, new TokenAdaptor(this, new VirusHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-smtp", this, new TokenAdaptor(this, new VirusSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, strength),
            new SoloPipeSpec("virus-pop", this, new TokenAdaptor(this, new VirusPopFactory(this)), Fitting.POP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-imap", this, new TokenAdaptor(this, new VirusImapFactory(this)), Fitting.IMAP_TOKENS, Affinity.SERVER, strength)
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
        if (settings.getScanHttp()) {
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

    public String getVendor()
    {
        return scanner.getVendorName();
    }

    // AbstractNode methods ----------------------------------------------

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return pipeSpecs;
    }

    public void initializeSettings()
    {
        VirusSettings vs = new VirusSettings();
        initMimeTypes(vs);
        initFileExtensions(vs);

        setSettings(vs);
    }

    private void initMimeTypes(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();
        s.add(new GenericRule("message/*", "messages", "misc", null, true));

        vs.setHttpMimeTypes(s);
    }

    private void initFileExtensions(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();

        s.add(new GenericRule("exe", "executable", "download" , null, true));
        s.add(new GenericRule("com", "executable", "download", null, true));
        s.add(new GenericRule("ocx", "executable", "ActiveX", null, true));
        s.add(new GenericRule("dll", "executable", "ActiveX", null, false));
        s.add(new GenericRule("cab", "executable", "ActiveX", null, true));
        s.add(new GenericRule("bin", "executable", "download", null, true));
        s.add(new GenericRule("bat", "executable", "download", null, true));
        s.add(new GenericRule("pif", "executable", "download" , null, true));
        s.add(new GenericRule("scr", "executable", "download" , null, true));
        s.add(new GenericRule("cpl", "executable", "download" , null, true));
        s.add(new GenericRule("hta", "executable", "download" , null, true));
        s.add(new GenericRule("vb",  "script", "download" , null, true));
        s.add(new GenericRule("vbe", "script", "download" , null, true));
        s.add(new GenericRule("vbs", "script", "download" , null, true));
        s.add(new GenericRule("zip", "archive", "download" , null, true));
        s.add(new GenericRule("eml", "archive", "download" , null, true));
        s.add(new GenericRule("hqx", "archive", "download", null, true));
        s.add(new GenericRule("rar", "archive", "download" , null, true));
        s.add(new GenericRule("arj", "archive", "download" , null, true));
        s.add(new GenericRule("ace", "archive", "download" , null, true));
        s.add(new GenericRule("gz",  "archive", "download" , null, true));
        s.add(new GenericRule("tar", "archive", "download" , null, true));
        s.add(new GenericRule("tgz", "archive", "download" , null, true));
        s.add(new GenericRule("doc", "document", "document", null, false));
        s.add(new GenericRule("docx", "document", "document", null, false));
        s.add(new GenericRule("ppt", "presentation", "document", null, false));
        s.add(new GenericRule("pptx", "presentation", "document", null, false));
        s.add(new GenericRule("xls", "spreadsheet", "document", null, false));
        s.add(new GenericRule("xlsx", "spreadsheet", "document", null, false));
        s.add(new GenericRule("pdf", "document", "document" , null, true));
        s.add(new GenericRule("mp3", "audio", "download", null, false));
        s.add(new GenericRule("wav", "audio", "download", null, false));
        s.add(new GenericRule("wmf", "audio", "download", null, false));
        s.add(new GenericRule("mov", "video", "download", null, false));
        s.add(new GenericRule("mpg", "video", "download", null, false));
        s.add(new GenericRule("avi", "video", "download", null, false));
        s.add(new GenericRule("swf", "flash", "download", null, false));
        s.add(new GenericRule("jar",   "java", "download", null, false));
        s.add(new GenericRule("class", "java", "download", null, false));

        vs.setHttpFileExtensions(s);
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        VirusSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/untangle-node-" + this.getName() + "/" + "settings_" + nodeID;
        
        try {
            readSettings = settingsManager.load( VirusSettings.class, settingsFileName );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:",e);
        }

        /**
         * If there are no settings, run the conversion script to see if there are any in the database
         * Then check again for the file
         */
        if (readSettings == null) {
            logger.warn("No settings found - Running conversion script to check DB");
            try {
                String convertCmd = SETTINGS_CONVERSION_SCRIPT + " " + nodeID.toString() + " " + this.getName() + " " + settingsFileName + ".js";
                logger.warn("Running: " + convertCmd);
                UvmContextFactory.context().execManager().exec( convertCmd );
            } catch ( Exception e ) {
                logger.warn( "Conversion script failed.", e );
            } 

            try {
                readSettings = settingsManager.load( VirusSettings.class, settingsFileName );
                if (readSettings != null) {
                    logger.warn("Found settings imported from database");
                    /* reinitialize the lists from scratch */
                    initMimeTypes(readSettings);
                    initFileExtensions(readSettings);
                }
            } catch (SettingsManager.SettingsException e) {
                logger.warn("Failed to load settings:",e);
            }
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            initializeSettings();
        }
        else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            
            this.settings = readSettings;
            logger.info("Settings: " + this.settings.toJSONString());
        }

        deployWebAppIfRequired(logger);
    }

    protected void preStart()
    {
        reconfigure();
    }

    protected void postStart()
    {
        killMatchingSessions(VIRUS_SESSION_MATCHER);
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
        return TRICKLE_RATE;
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

        UvmContext mctx = UvmContextFactory.context();
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

        UvmContext mctx = UvmContextFactory.context();
        LocalAppServerManager asm = mctx.localAppServerManager();

        if (asm.unloadWebApp("/virus")) {
            logger.debug("Unloaded Virus WebApp");
        } else {
            logger.warn("Unable to unload Virus WebApp");
        }
    }

    /**
     * Set the current settings to new Settings
     * And save the settings to disk
     */
    protected void _setSettings( VirusSettings newSettings )
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getNodeSettings().getId().toString();
        try {
            settingsManager.save(VirusSettings.class, System.getProperty("uvm.settings.dir") + "/" + "untangle-node-" + this.getName() + "/" + "settings_" + nodeID, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Reset existing sessions
         */
        killMatchingSessions(VIRUS_SESSION_MATCHER);
    }

    
}
