/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import static com.untangle.uvm.util.Ascii.CRLF;

import java.net.InetAddress;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Token;
import com.untangle.app.http.HeaderToken;

/**
 * Virus App.
 */
public abstract class VirusBlockerBaseApp extends AppBase
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
     * Cached in the app in case the base settings lose the values during a
     * save.
     */
    private Date lastSignatureUpdate = new Date();
    private String signatureVersion = "";

    protected boolean fileScannerAvailable = true;
    private VirusSettings settings;

    /* This can't be static because it uses policy which is per app */
    private final SessionMatcher VIRUS_SESSION_MATCHER = new SessionMatcher()
    {
        /**
         * Session matcher checker and handler where we kill all FTP, HTTP, and
         * SMTP sessions
         * 
         * @param policyId
         *        The policy ID
         * @param protocol
         *        The protocol
         * @param clientIntf
         *        The client interface
         * @param serverIntf
         *        The server interface
         * @param clientAddr
         *        The client address
         * @param serverAddr
         *        The server address
         * @param clientPort
         *        The client port
         * @param serverPort
         *        The server port
         * @param attachments
         *        Session attachments
         * @return True if the session should be terminated, otherwise false
         */
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

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public VirusBlockerBaseApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.addMetric(new AppMetric(STAT_SCAN, I18nUtil.marktr("Documents scanned")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Documents blocked")));
        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Documents passed")));
        this.addMetric(new AppMetric(STAT_REMOVE, I18nUtil.marktr("Infections removed")));
        this.addMetric(new AppMetric(STAT_PASS_POLICY, I18nUtil.marktr("Passed by policy")));

        this.virusFtpCtlHandler = new VirusFtpHandler(this);
        this.virusFtpDataHandler = new VirusFtpHandler(this);
        this.virusHttpHandler = new VirusHttpHandler(this);
        this.virusSmtpHandler = new VirusSmtpHandler(this);

        this.virusFtpCtl = UvmContextFactory.context().pipelineFoundry().create("virus-ftp-ctl", this, null, virusFtpCtlHandler, Fitting.FTP_CTL_TOKENS, Fitting.FTP_CTL_TOKENS, Affinity.SERVER, getFtpStrength(), isPremium());
        this.virusFtpData = UvmContextFactory.context().pipelineFoundry().create("virus-data-ctl", this, null, virusFtpDataHandler, Fitting.FTP_DATA_TOKENS, Fitting.FTP_DATA_TOKENS, Affinity.SERVER, getFtpStrength(), isPremium());
        this.virusHttp = UvmContextFactory.context().pipelineFoundry().create("virus-http", this, null, virusHttpHandler, Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.SERVER, getHttpStrength(), isPremium());
        this.virusSmtp = UvmContextFactory.context().pipelineFoundry().create("virus-smtp", this, null, virusSmtpHandler, Fitting.SMTP_TOKENS, Fitting.SMTP_TOKENS, Affinity.CLIENT, getSmtpStrength(), isPremium());
        this.connectors = new PipelineConnector[] { virusFtpCtl, virusFtpData, virusHttp, virusSmtp };
        this.replacementGenerator = new VirusReplacementGenerator(getAppSettings());

        String appName = getName();
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public VirusSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(VirusSettings newSettings)
    {
        _setSettings(newSettings);
    }

    /**
     * Get the list of HTTP file extensions
     * 
     * @return The list of extensions
     */
    public List<GenericRule> getHttpFileExtensions()
    {
        return settings.getHttpFileExtensions();
    }

    /**
     * Set the list of HTTP file extensions
     * 
     * @param fileExtensions
     *        The list of extensions
     */
    public void setHttpFileExtensions(List<GenericRule> fileExtensions)
    {
        settings.setHttpFileExtensions(fileExtensions);
        _setSettings(settings);
    }

    /**
     * Get the HTTP mime types
     * 
     * @return The list of mime types
     */
    public List<GenericRule> getHttpMimeTypes()
    {
        return settings.getHttpMimeTypes();
    }

    /**
     * Set the HTTP mime types
     * 
     * @param mimeTypes
     *        The list of mime types
     */
    public void setHttpMimeTypes(List<GenericRule> mimeTypes)
    {
        settings.setHttpMimeTypes(mimeTypes);
        _setSettings(settings);
    }

    /**
     * Get the pass sites
     * 
     * @return The list of pass sites
     */
    public List<GenericRule> getPassSites()
    {
        return settings.getPassSites();
    }

    /**
     * Get the pass sites
     * 
     * @param passSites
     *        The list of pass sites
     */
    public void setPassSites(List<GenericRule> passSites)
    {
        settings.setPassSites(passSites);
        _setSettings(settings);
    }

    /**
     * Get the virus block details
     * 
     * @param nonce
     *        The nonce
     * @return The details
     */
    public VirusBlockDetails getDetails(String nonce)
    {
        return replacementGenerator.getNonceData(nonce);
    }

    /**
     * Generate a nonce
     * 
     * @param details
     *        The virus block details
     * @return A nonce
     */
    public String generateNonce(VirusBlockDetails details)
    {
        return replacementGenerator.generateNonce(details);
    }

    /**
     * Generate a response
     * 
     * @param nonce
     *        The nonce
     * @param session
     *        The session
     * @param uri
     *        The URI
     * @return The response
     */
    public Token[] generateResponse(String nonce, AppTCPSession session, String uri)
    {
        return replacementGenerator.generateResponse(nonce, session, uri, null);
    }

    /**
     * Generate a response
     * 
     * @param nonce
     *        The nonce
     * @param session
     *        The session
     * @param uri
     *        The URI
     * @param header
     *        The header
     * @return The response
     */
    public Token[] generateResponse(String nonce, AppTCPSession session, String uri, HeaderToken header)
    {
        return replacementGenerator.generateResponse(nonce, session, uri, header);
    }

    /**
     * Get the date of the last virus signature update
     * 
     * @return The date of the last virus signature update
     */
    public Date getLastSignatureUpdate()
    {
        return scanner.getLastSignatureUpdate();
    }

    /**
     * Checks to see if the local file scanner is available
     * 
     * @return True if available, otherwise false
     */
    public boolean isFileScannerAvailable()
    {
        return fileScannerAvailable;
    }

    /**
     * Gets the HTTP strength
     * 
     * @return The HTTP strength
     */
    protected abstract int getHttpStrength();

    /**
     * Gets the FTP strength
     * 
     * @return The FTP strength
     */
    protected abstract int getFtpStrength();

    /**
     * Gets the SMTP strength
     * 
     * @return The SMTP strength
     */
    protected abstract int getSmtpStrength();

    /**
     * Gets the name
     * 
     * @return The name
     */
    public abstract String getName();

    /**
     * Gets the application name
     * 
     * @return The application name
     */
    public abstract String getAppName();

    /**
     * Checks to see if application is free or premium
     * 
     * @return False for free, true for premium
     */
    public abstract boolean isPremium();

    /**
     * Clear the cache for all event handlers
     */
    public void clearAllEventHandlerCaches()
    {
        virusFtpCtlHandler.clearEventHandlerCache();
        virusFtpDataHandler.clearEventHandlerCache();
        virusHttpHandler.clearEventHandlerCache();
        virusSmtpHandler.clearEventHandlerCache();
    }

    /**
     * Applies application settings to all scanner systems
     */
    public void reconfigure()
    {
        virusHttp.setEnabled(settings.getScanHttp());
        virusSmtp.setEnabled(settings.getScanSmtp());
        virusFtpCtl.setEnabled(settings.getScanFtp());
        virusFtpData.setEnabled(settings.getScanFtp());
    }

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
     * Initialize the application settings
     */
    public void initializeSettings()
    {
        VirusSettings vs = new VirusSettings();
        initMimeTypes(vs);
        initPassSites(vs);
        initFileExtensions(vs);

        setSettings(vs);
    }

    /**
     * Initialize the mime types
     * 
     * @param vs
     *        The virus settings
     */
    private void initMimeTypes(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();
        s.add(new GenericRule("message/*", "messages", "misc", null, true));
        s.add(new GenericRule("application/zip", "application", "zip", null, true));
        s.add(new GenericRule("application/x-tar", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-gz", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-gzip", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-compressed", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-zip-compressed", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-rar-compressed", "application", "compressed", null, true));
        s.add(new GenericRule("application/x-7z-compressed", "application", "compressed", null, true));
        s.add(new GenericRule("application/octet-stream", "application", "octet", null, false));

        vs.setHttpMimeTypes(s);
    }

    /**
     * Initialize the pass sites
     * 
     * @param vs
     *        The virus settings
     */
    private void initPassSites(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();
        s.add(new GenericRule("*windowsupdate.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*windowsupdate.microsoft.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*update.microsoft.com", "Microsoft", "update", null, true));
        s.add(new GenericRule("*liveupdate.symantecliveupdate.com", "Symantec", "update", null, true));

        vs.setPassSites(s);
    }

    /**
     * Initialize the file extensions
     * 
     * @param vs
     *        The virus settings
     */
    private void initFileExtensions(VirusSettings vs)
    {
        List<GenericRule> s = new LinkedList<GenericRule>();

        s.add(new GenericRule("exe", "executable", "download", null, true));
        s.add(new GenericRule("com", "executable", "download", null, true));
        s.add(new GenericRule("ocx", "executable", "ActiveX", null, true));
        s.add(new GenericRule("dll", "executable", "ActiveX", null, true));
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
        s.add(new GenericRule("7z", "archive", "download", null, true));
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

    /**
     * Called after the application has been initialized
     */
    @Override
    protected void postInit()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        VirusSettings readSettings = null;
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/" + this.getAppName() + "/" + "settings_" + appID + ".js";

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

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    protected void preStart(boolean isPermanentTransition)
    {
        deployWebAppIfRequired(logger);

        reconfigure();
    }

    /**
     * Called after the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    protected void postStart(boolean isPermanentTransition)
    {
        /**
         * killall sessions on HTTP, FTP, etc This is so it blocks viruses
         * immediately, by forcing existing connections to be closed
         */
        killMatchingSessionsGlobal(VIRUS_SESSION_MATCHER);
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    protected void postStop(boolean isPermanentTransition)
    {
        unDeployWebAppIfRequired(logger);
    }

    /**
     * Gets the virus scanner
     * 
     * @return The virus scanner
     */
    protected VirusScanner getScanner()
    {
        return scanner;
    }

    /**
     * Sets the virus scanner
     * 
     * @param scanner
     *        The scanner
     */
    protected void setScanner(VirusScanner scanner)
    {
        this.scanner = scanner;
    }

    /**
     * Gets the trickle rate percentage
     * 
     * @return The trickle rate percentage
     */
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

    /**
     * Deploy the web application
     * 
     * @param logger
     *        The logger
     */
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

    /**
     * Undeploy the web application
     * 
     * @param logger
     *        The logger
     */
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
     * 
     * @param newSettings
     *        The new settings
     */
    protected void _setSettings(VirusSettings newSettings)
    {
        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        String appID = this.getAppSettings().getId().toString();
        String settingsFileName = System.getProperty("uvm.settings.dir") + "/" + this.getAppName() + "/" + "settings_" + appID + ".js";
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
         * Reset existing sessions for this app only
         */
        killMatchingSessions(VIRUS_SESSION_MATCHER);
    }
}
