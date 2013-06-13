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
import java.util.Map;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.json.JSONString;

import com.untangle.uvm.SettingsManager;
import com.untangle.node.smtp.SMTPNotifyAction;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeSession;

/**
 * Virus Node.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class VirusNodeImpl extends NodeBase implements VirusNode
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_PASS = "pass";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_REMOVE = "remove";
    private static final String STAT_PASS_POLICY = "pass-infected";
    
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

    private static int deployCount = 0;
    
    private final VirusScanner scanner;
    private final PipeSpec[] pipeSpecs;
    private final VirusReplacementGenerator replacementGenerator;

    private final Logger logger = Logger.getLogger(VirusNodeImpl.class);

    /* the signatures are updated at startup, so using new Date() is not that far off. */
    /* Cached in the node in case the base settings lose the values during a save. */
    private Date lastSignatureUpdate = new Date();
    private String signatureVersion = "";

    private VirusSettings settings;

    private EventLogQuery httpScannedEventQuery;
    private EventLogQuery httpInfectedEventQuery;
    private EventLogQuery httpCleanEventQuery;
    private EventLogQuery mailScannedEventQuery;
    private EventLogQuery mailInfectedEventQuery;
    private EventLogQuery mailCleanEventQuery;
    private EventLogQuery ftpScannedEventQuery;
    private EventLogQuery ftpInfectedEventQuery;
    private EventLogQuery ftpCleanEventQuery;
    
    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher() {
            /* Kill all FTP, HTTP, SMTP, sessions */
            public boolean isMatch( Long policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments )
            {
                /* Only look at TCP sessions */
                if (protocol != 6 /* TCP */) {
                    return false;
                }

                /* FTP server is on 21, HTTP server is on 80 */
                if (serverPort == 21 || serverPort == 80) {
                    return true;
                }

                /* email SMTP (25) */
                if (serverPort == 25) {
                    return true;
                }

                return false;
            }
        };

    // constructors -----------------------------------------------------------

    public VirusNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties, VirusScanner scanner )
    {
        super( nodeSettings, nodeProperties );

        this.scanner = scanner;
        this.pipeSpecs = initialPipeSpecs();
        this.replacementGenerator = new VirusReplacementGenerator(getNodeSettings());

        String nodeName = getName();

        this.httpScannedEventQuery = new EventLogQuery(I18nUtil.marktr("Scanned Web Events"),
                                                        " SELECT * FROM reports.http_events " + 
                                                        " WHERE " + nodeName + "_clean IS NOT NULL" + 
                                                        " AND policy_id = :policyId" + 
                                                        " ORDER BY time_stamp DESC");
        this.httpInfectedEventQuery = new EventLogQuery(I18nUtil.marktr("Infected Web Events"),
                                                        " SELECT * FROM reports.http_events " + 
                                                        " WHERE " + nodeName + "_clean IS FALSE" + 
                                                        " AND policy_id = :policyId" + 
                                                        " ORDER BY time_stamp DESC");
        this.httpCleanEventQuery = new EventLogQuery(I18nUtil.marktr("Clean Web Events"),
                                                     " SELECT * FROM reports.http_events " + 
                                                     " WHERE " + nodeName + "_clean IS TRUE" + 
                                                     " AND policy_id = :policyId" + 
                                                     " ORDER BY time_stamp DESC");
        this.mailScannedEventQuery = new EventLogQuery(I18nUtil.marktr("Scanned Email Events"),
                                                     " SELECT * FROM reports.mail_addrs " + 
                                                        " WHERE addr_kind IN ('T', 'C')" +
                                                        " AND " + nodeName + "_clean IS NOT NULL " + 
                                                        " AND policy_id = :policyId" + 
                                                        " ORDER BY time_stamp DESC");
        this.mailInfectedEventQuery = new EventLogQuery(I18nUtil.marktr("Infected Email Events"),
                                                     " SELECT * FROM reports.mail_addrs " + 
                                                        " WHERE addr_kind IN ('T', 'C')" +
                                                        " AND " + nodeName + "_clean IS FALSE" + 
                                                        " AND policy_id = :policyId" + 
                                                        " ORDER BY time_stamp DESC");
        this.mailCleanEventQuery = new EventLogQuery(I18nUtil.marktr("Clean Email Events"),
                                                     " SELECT * FROM reports.mail_addrs " + 
                                                     " WHERE addr_kind IN ('T', 'C')" +
                                                     " AND " + nodeName + "_clean IS TRUE" + 
                                                     " AND policy_id = :policyId" + 
                                                     " ORDER BY time_stamp DESC");
        this.ftpScannedEventQuery = new EventLogQuery(I18nUtil.marktr("Scanned Ftp Events"),
									                " SELECT * FROM reports.ftp_events " + 
									                " WHERE " + nodeName + "_clean IS NOT NULL" + 
									                " AND policy_id = :policyId" + 
									                " ORDER BY time_stamp DESC");
		this.ftpInfectedEventQuery = new EventLogQuery(I18nUtil.marktr("Infected Ftp Events"),
									                " SELECT * FROM reports.ftp_events " + 
									                " WHERE " + nodeName + "_clean IS FALSE" + 
									                " AND policy_id = :policyId" + 
									                " ORDER BY time_stamp DESC");
		this.ftpCleanEventQuery = new EventLogQuery(I18nUtil.marktr("Clean Ftp Events"),
										             " SELECT * FROM reports.ftp_events " + 
										             " WHERE " + nodeName + "_clean IS TRUE" + 
										             " AND policy_id = :policyId" + 
										             " ORDER BY time_stamp DESC");

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Documents scanned")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Documents blocked")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Documents passed")));
        this.addMetric(new NodeMetric(STAT_REMOVE, I18nUtil.marktr("Infections removed")));
        this.addMetric(new NodeMetric(STAT_PASS_POLICY, I18nUtil.marktr("Passed by policy")));
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
        return new EventLogQuery[] { this.httpScannedEventQuery, this.httpInfectedEventQuery, this.httpCleanEventQuery };
    }
    
    public EventLogQuery[] getFtpEventQueries()
    {
        return new EventLogQuery[] { this.ftpScannedEventQuery, this.ftpInfectedEventQuery, this.ftpCleanEventQuery };
    }
    
    public EventLogQuery[] getMailEventQueries()
    {
        return new EventLogQuery[] { this.mailScannedEventQuery, this.mailInfectedEventQuery, this.mailCleanEventQuery };
    }

    public VirusBlockDetails getDetails( String nonce )
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public String generateNonce( VirusBlockDetails details )
    {
        return replacementGenerator.generateNonce(details);
    }

    public Token[] generateResponse( String nonce, NodeTCPSession session, String uri )
    {
        return replacementGenerator.generateResponse(nonce, session, uri, null );
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
        	new SoloPipeSpec("virus-ftp-ctl", this, new TokenAdaptor(this, new VirusFtpFactory(this)), Fitting.FTP_CTL_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-ftp-data", this, new TokenAdaptor(this, new VirusFtpFactory(this)), Fitting.FTP_DATA_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-http", this, new TokenAdaptor(this, new VirusHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.SERVER, strength),
            new SoloPipeSpec("virus-smtp", this, new TokenAdaptor(this, new VirusSmtpFactory(this)), Fitting.SMTP_TOKENS, Affinity.CLIENT, strength),
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
    }

    // NodeBase methods ----------------------------------------------

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
            logger.debug("Settings: " + this.settings.toJSONString());
        }

        deployWebAppIfRequired(logger);
    }

    protected void preStart()
    {
        reconfigure();
    }

    protected void postStart()
    {
        /**
         * killall sessions on HTTP, FTP, etc
         * This is so it blocks viruses immediately, by forcing existing connections to be closed
         */
        killMatchingSessionsGlobal(VIRUS_SESSION_MATCHER);
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
        this.incrementMetric(STAT_SCAN);
    }

    /**
     * Increment the counter for blocked (SMTP only).
     */
    public void incrementBlockCount()
    {
        this.incrementMetric(STAT_BLOCK);
    }

    /**
     * Increment the counter for messages passed
     */
    public void incrementPassCount()
    {
        this.incrementMetric(STAT_PASS);
    }

    /**
     * Increment the counter for messages where we
     * removed a virus
     */
    public void incrementRemoveCount()
    {
        this.incrementMetric(STAT_REMOVE);
    }

    /**
     * Increment the counter for messages where we
     * found a firus but passed it on due to a rule
     */
    public void incrementPassedInfectedMessageCount()
    {
        this.incrementMetric(STAT_PASS_POLICY);
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (0 != deployCount++) {
            return;
        }

        if (null != UvmContextFactory.context().tomcatManager().loadServlet("/virus", "virus")) {
            logger.debug("Deployed Virus WebApp");
        } else {
            logger.error("Unable to deploy Virus WebApp");
        }
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger) {
        if (0 != --deployCount) {
            return;
        }

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/virus")) {
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
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        /**
         * Reset existing sessions for this node only
         */
        killMatchingSessions(VIRUS_SESSION_MATCHER);
    }

    
}
