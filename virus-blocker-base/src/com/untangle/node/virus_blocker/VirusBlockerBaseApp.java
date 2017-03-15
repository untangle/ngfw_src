/*
 * $Id$
 */
package com.untangle.node.virus_blocker;

import static com.untangle.uvm.util.Ascii.CRLF;

import java.io.File;
import java.net.InetAddress;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Token;
import com.untangle.node.http.HeaderToken;

/**
 * Virus Node.
 */
public abstract class VirusBlockerBaseApp extends NodeBase
{
    private static final String STAT_SCAN = "scan";
    private static final String STAT_PASS = "pass";
    private static final String STAT_BLOCK = "block";
    private static final String STAT_REMOVE = "remove";
    private static final String STAT_PASS_POLICY = "pass-infected";

    private static final int TRICKLE_RATE = 90;

    private static final String MOD_SUB_TEMPLATE = "[VIRUS] $MIMEMessage:SUBJECT$";

    private static final String MOD_BODY_TEMPLATE = "The attached message from $MIMEMessage:FROM$\r\n" + "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n" + "The infected portion of the message was removed by Virus Blocker.\r\n";

    private static final String MOD_BODY_SMTP_TEMPLATE = "The attached message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)\r\n" + "was found to contain the virus \"$VirusReport:VIRUS_NAME$\".\r\n" + "The infected portion of the message was removed by Virus Blocker.\r\n";

    private static final String NOTIFY_SUB_TEMPLATE = "[VIRUS NOTIFICATION] re: $MIMEMessage:SUBJECT$";

    private static final String NOTIFY_BODY_TEMPLATE = "On $MIMEHeader:DATE$ a message from $MIMEMessage:FROM$ ($SMTPTransaction:FROM$)" + CRLF + "was received by $SMTPTransaction:TO$.  The message was found" + CRLF + "to contain the virus \"$VirusReport:VIRUS_NAME$\"." + CRLF + "The infected portion of the message was removed by Virus Blocker";

    private VirusFtpHandler virusFtpCtlHandler;
    private VirusFtpHandler virusFtpDataHandler;
    private VirusHttpHandler virusHttpHandler;
    private VirusSmtpHandler virusSmtpHandler;

    private PipelineConnector virusFtpCtl;
    private PipelineConnector virusFtpData;
    private PipelineConnector virusHttp;
    private PipelineConnector virusSmtp;

    private static int deployCount = 0;

    private VirusScanner scanner = null;

    private final PipelineConnector[] connectors;
    private final VirusReplacementGenerator replacementGenerator;
    private final Logger logger = Logger.getLogger(VirusBlockerBaseApp.class);

    /*
     * the signatures are updated at startup, so using new Date() is not that
     * far off.
     */
    /*
     * Cached in the node in case the base settings lose the values during a
     * save.
     */
    private Date lastSignatureUpdate = new Date();
    private String signatureVersion = "";

    private boolean fileScannerAvailable = true;
    private VirusSettings settings;

    /* This can't be static because it uses policy which is per node */
    private final SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher()
    {
        /* Kill all FTP, HTTP, SMTP, sessions */
        public boolean isMatch(Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String, Object> attachments)
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

    public VirusBlockerBaseApp(com.untangle.uvm.node.AppSettings appSettings, com.untangle.uvm.node.AppProperties appProperties )
    {
        super(appSettings, appProperties);

        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Documents scanned")));
        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Documents blocked")));
        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Documents passed")));
        this.addMetric(new NodeMetric(STAT_REMOVE, I18nUtil.marktr("Infections removed")));
        this.addMetric(new NodeMetric(STAT_PASS_POLICY, I18nUtil.marktr("Passed by policy")));

        this.virusFtpCtlHandler = new VirusFtpHandler(this);
        this.virusFtpDataHandler = new VirusFtpHandler(this);
        this.virusHttpHandler = new VirusHttpHandler(this);
        this.virusSmtpHandler = new VirusSmtpHandler(this);

        this.virusFtpCtl = UvmContextFactory.context().pipelineFoundry().create("virus-ftp-ctl", this, null, virusFtpCtlHandler, Fitting.FTP_CTL_TOKENS, Fitting.FTP_CTL_TOKENS, Affinity.SERVER, getFtpStrength(), isPremium());
        this.virusFtpData = UvmContextFactory.context().pipelineFoundry().create("virus-data-ctl", this, null, virusFtpDataHandler, Fitting.FTP_DATA_TOKENS, Fitting.FTP_DATA_TOKENS, Affinity.SERVER, getFtpStrength(), isPremium());
        this.virusHttp = UvmContextFactory.context().pipelineFoundry().create("virus-http", this, null, virusHttpHandler, Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.SERVER, getHttpStrength(), isPremium());
        this.virusSmtp = UvmContextFactory.context().pipelineFoundry().create("virus-smtp", this, null, virusSmtpHandler, Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, getSmtpStrength(), isPremium());
        this.connectors = new PipelineConnector[] { virusFtpCtl, virusFtpData, virusHttp, virusSmtp };

        // if the bdamserver package is not installed we set our special flag
        try {
            File daemonCheck = new File("/etc/init.d/untangle-bdamserver");
            if (daemonCheck.exists() == false) {
                fileScannerAvailable = false;
            }
        } catch (Exception exn) {
            fileScannerAvailable = false;
        }

        this.replacementGenerator = new VirusReplacementGenerator(getAppSettings());

        String nodeName = getName();
    }

    // VirusNode methods -------------------------------------------------
    public VirusSettings getSettings()
    {
        return this.settings;
    }

    public void setSettings(VirusSettings newSettings)
    {
        _setSettings(newSettings);
    }

    public List<GenericRule> getHttpFileExtensions()
    {
        return settings.getHttpFileExtensions();
    }

    public void setHttpFileExtensions(List<GenericRule> fileExtensions)
    {
        settings.setHttpFileExtensions(fileExtensions);
        _setSettings(settings);
    }

    public List<GenericRule> getHttpMimeTypes()
    {
        return settings.getHttpMimeTypes();
    }

    public void setHttpMimeTypes(List<GenericRule> mimeTypes)
    {
        settings.setHttpMimeTypes(mimeTypes);
        _setSettings(settings);
    }

    public List<GenericRule> getPassSites()
    {
        return settings.getPassSites();
    }

    public void setPassSites(List<GenericRule> passSites)
    {
        settings.setPassSites(passSites);
        _setSettings(settings);
    }

    public VirusBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    public String generateNonce(VirusBlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    public Token[] generateResponse(String nonce, NodeTCPSession session, String uri)
    {
        return replacementGenerator.generateResponse(nonce, session, uri, null);
    }

    public Token[] generateResponse(String nonce, NodeTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateResponse(nonce, session, uri, header);
    }

    public Date getLastSignatureUpdate()
    {
        return scanner.getLastSignatureUpdate();
    }

    public boolean isFileScannerAvailable()
    {
        return fileScannerAvailable;
    }

    protected abstract int getHttpStrength();

    protected abstract int getFtpStrength();

    protected abstract int getSmtpStrength();

    public abstract String getName();

    public abstract String getAppName();

    public abstract boolean isPremium();

    public void clearAllEventHandlerCaches()
    {
        virusFtpCtlHandler.clearEventHandlerCache();
        virusFtpDataHandler.clearEventHandlerCache();
        virusHttpHandler.clearEventHandlerCache();
        virusSmtpHandler.clearEventHandlerCache();
    }

    public void reconfigure()
    {
        virusHttp.setEnabled(settings.getScanHttp());
        virusSmtp.setEnabled(settings.getScanSmtp());
        virusFtpCtl.setEnabled(settings.getScanFtp());
        virusFtpData.setEnabled(settings.getScanFtp());
    }

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    public void initializeSettings()
    {
        VirusSettings vs = new VirusSettings();
        initMimeTypes(vs);
        initPassSites(vs);
        initFileExtensions(vs);

        setSettings(vs);
    }

    private void initMimeTypes(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();
        s.add(new GenericRule("message/*", "messages", "misc", null, true));

        vs.setHttpMimeTypes(s);
    }

    private void initPassSites(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();
        s.add(new GenericRule("*windowsupdate.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*windowsupdate.microsoft.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*update.microsoft.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*liveupdate.symantecliveupdate.com", "Symantec", "update", null, true));

        vs.setPassSites(s);
    }

    private void initFileExtensions(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();

        s.add(new GenericRule("exe", "executable", "download", null, true));
        s.add(new GenericRule("com", "executable", "download", null, true));
        s.add(new GenericRule("ocx", "executable", "ActiveX", null, true));
        s.add(new GenericRule("dll", "executable", "ActiveX", null, false));
        s.add(new GenericRule("cab", "executable", "ActiveX", null, true));
        s.add(new GenericRule("bin", "executable", "download", null, true));
        s.add(new GenericRule("bat", "executable", "download", null, true));
        s.add(new GenericRule("pif", "executable", "download", null, true));
        s.add(new GenericRule("scr", "executable", "download", null, true));
        s.add(new GenericRule("cpl", "executable", "download", null, true));
        s.add(new GenericRule("hta", "executable", "download", null, true));
        s.add(new GenericRule("msi", "executable", "download", null, true));
        s.add(new GenericRule("vb", "script", "download", null, true));
        s.add(new GenericRule("vbe", "script", "download", null, true));
        s.add(new GenericRule("vbs", "script", "download", null, true));
        s.add(new GenericRule("zip", "archive", "download", null, true));
        s.add(new GenericRule("eml", "archive", "download", null, true));
        s.add(new GenericRule("hqx", "archive", "download", null, true));
        s.add(new GenericRule("rar", "archive", "download", null, true));
        s.add(new GenericRule("arj", "archive", "download", null, true));
        s.add(new GenericRule("ace", "archive", "download", null, true));
        s.add(new GenericRule("gz", "archive", "download", null, true));
        s.add(new GenericRule("tar", "archive", "download", null, true));
        s.add(new GenericRule("tgz", "archive", "download", null, true));
        s.add(new GenericRule("doc", "document", "document", null, true));
        s.add(new GenericRule("docx", "document", "document", null, true));
        s.add(new GenericRule("ppt", "presentation", "document", null, true));
        s.add(new GenericRule("pptx", "presentation", "document", null, true));
        s.add(new GenericRule("xls", "spreadsheet", "document", null, true));
        s.add(new GenericRule("xlsx", "spreadsheet", "document", null, true));
        s.add(new GenericRule("pdf", "document", "document", null, true));
        s.add(new GenericRule("mp3", "audio", "download", null, false));
        s.add(new GenericRule("wav", "audio", "download", null, false));
        s.add(new GenericRule("wmf", "audio", "download", null, false));
        s.add(new GenericRule("mov", "video", "download", null, false));
        s.add(new GenericRule("mpg", "video", "download", null, false));
        s.add(new GenericRule("avi", "video", "download", null, false));
        s.add(new GenericRule("swf", "flash", "download", null, false));
        s.add(new GenericRule("jar", "java", "download", null, false));
        s.add(new GenericRule("class", "java", "download", null, false));

        vs.setHttpFileExtensions(s);
    }

    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        VirusSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/" + this.getAppName() + "/" + "settings_" + nodeID + ".js";

        try {
            readSettings = settingsManager.load(VirusSettings.class, settingsFileName);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to load settings:", e);
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn("No settings found - Initializing new settings.");

            initializeSettings();
        } else {
            logger.info("Loading Settings...");

            // UPDATE settings if necessary
            if (readSettings.getPassSites().size() == 0) {
                initPassSites(readSettings);
            }

            this.settings = readSettings;
            logger.debug("Settings: " + this.settings.toJSONString());
        }
    }

    protected void preStart( boolean isPermanentTransition )
    {
        deployWebAppIfRequired(logger);

        reconfigure();
    }

    protected void postStart( boolean isPermanentTransition )
    {
        /**
         * killall sessions on HTTP, FTP, etc This is so it blocks viruses
         * immediately, by forcing existing connections to be closed
         */
        killMatchingSessionsGlobal(VIRUS_SESSION_MATCHER);
    }

    protected void postStop( boolean isPermanentTransition )
    {
        unDeployWebAppIfRequired(logger);
    }

    // package protected methods ----------------------------------------------

    protected VirusScanner getScanner()
    {
        return scanner;
    }

    protected void setScanner(VirusScanner scanner)
    {
        this.scanner = scanner;
    }
    
    protected int getTricklePercent()
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
     * Increment the counter for messages where we removed a virus
     */
    public void incrementRemoveCount()
    {
        this.incrementMetric(STAT_REMOVE);
    }

    /**
     * Increment the counter for messages where we found a firus but passed it
     * on due to a rule
     */
    public void incrementPassedInfectedMessageCount()
    {
        this.incrementMetric(STAT_PASS_POLICY);
    }

    private static synchronized void deployWebAppIfRequired(Logger logger)
    {
        if (deployCount == 0) {
            if (null != UvmContextFactory.context().tomcatManager().loadServlet("/virus", "virus")) {
                logger.debug("Deployed Virus WebApp");
            } else {
                logger.error("Unable to deploy Virus WebApp");
            }
        }

        deployCount++;
    }

    private static synchronized void unDeployWebAppIfRequired(Logger logger)
    {
        deployCount--;
        if (deployCount > 0) {
            return;
        }

        if (UvmContextFactory.context().tomcatManager().unloadServlet("/virus")) {
            logger.debug("Unloaded Virus WebApp");
        } else {
            logger.warn("Unable to unload Virus WebApp");
        }
    }

    /**
     * Set the current settings to new Settings And save the settings to disk
     */
    protected void _setSettings(VirusSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String nodeID = this.getAppSettings().getId().toString();
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/" + this.getAppName() + "/" + "settings_" + nodeID + ".js";
        try {
            settingsManager.save(settingsFileName, newSettings);
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.", e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {
            logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));
        } catch (Exception e) {
        }

        reconfigure();

        /**
         * Reset existing sessions for this node only
         */
        killMatchingSessions(VIRUS_SESSION_MATCHER);
    }
}
