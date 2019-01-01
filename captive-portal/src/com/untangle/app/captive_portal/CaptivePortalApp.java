/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Timer;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.io.File;
import java.io.FileOutputStream;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ExecManagerResult;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.HookBucket;
import com.untangle.uvm.HookCallback;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.app.DirectoryConnector;
import com.untangle.uvm.app.IPMaskedAddress;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.app.PortRange;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.servlet.UploadHandler;

/**
 * Implementation of the captive portal application.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalApp extends AppBase
{
    public enum BlingerType
    {
        SESSALLOW, SESSBLOCK, SESSQUERY, AUTHGOOD, AUTHFAIL
    }

    private final Logger logger = Logger.getLogger(getClass());
    private final Integer policyId = getAppSettings().getPolicyId();

    private final String CAPTURE_CUSTOM_CREATE_SCRIPT = System.getProperty("uvm.home") + "/bin/captive-portal-custom-create";
    private final String CAPTURE_CUSTOM_REMOVE_SCRIPT = System.getProperty("uvm.home") + "/bin/captive-portal-custom-remove";
    private final String CAPTURE_TEMPORARY_UPLOAD = System.getProperty("java.io.tmpdir") + "/capture_upload.zip";

    private static final int CLEANUP_INTERVAL = 60000;

    private static final String STAT_SESSALLOW = "sessallow";
    private static final String STAT_SESSBLOCK = "sessblock";
    private static final String STAT_SESSQUERY = "sessquery";
    private static final String STAT_AUTHGOOD = "authgood";
    private static final String STAT_AUTHFAIL = "authfail";

    private final CaptivePortalHttpsHandler httpsHandler = new CaptivePortalHttpsHandler(this);
    private final Subscription httpsSub = new Subscription(Protocol.TCP, IPMaskedAddress.anyAddr, PortRange.ANY, IPMaskedAddress.anyAddr, new PortRange(443, 443));

    private final PipelineConnector trafficConnector;
    private final PipelineConnector httpsConnector;
    private final PipelineConnector httpConnector;
    private final PipelineConnector[] connectors;

    private final CaptivePortalReplacementGenerator replacementGenerator;

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final String settingsFile = (System.getProperty("uvm.settings.dir") + "/captive-portal/settings_" + getAppSettings().getId().toString()) + ".js";

    protected CaptivePortalUserCookieTable captureUserCookieTable = new CaptivePortalUserCookieTable();
    protected CaptivePortalUserTable captureUserTable;
    private CaptivePortalSettings captureSettings;
    private CaptivePortalTimer captureTimer;
    private Timer timer;

    private HostRemovedHookCallback hostRemovedCallback = new HostRemovedHookCallback();
    private CaptureUsernameHookCallback usernameCheckCallback = new CaptureUsernameHookCallback();

// THIS IS FOR ECLIPSE - @formatter:off

    /**
     * The application constructor
     * 
     * @param appSettings
     *        The application settings
     * 
     * @param appProperties
     *        The application properties
     */
    public CaptivePortalApp( com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties )
    {
        super( appSettings, appProperties );

        captureUserTable = new CaptivePortalUserTable(this);
        replacementGenerator = new CaptivePortalReplacementGenerator(getAppSettings(),this);

        UvmContextFactory.context().servletFileManager().registerUploadHandler(new CustomPageUploadHandler());
        UvmContextFactory.context().servletFileManager().registerUploadHandler(new CustomPageRemoveHandler());

        addMetric(new AppMetric(STAT_SESSALLOW, I18nUtil.marktr("Sessions Allowed")));
        addMetric(new AppMetric(STAT_SESSBLOCK, I18nUtil.marktr("Sessions Blocked")));
        addMetric(new AppMetric(STAT_SESSQUERY, I18nUtil.marktr("DNS Lookups")));
        addMetric(new AppMetric(STAT_AUTHGOOD, I18nUtil.marktr("Login Success")));
        addMetric(new AppMetric(STAT_AUTHFAIL, I18nUtil.marktr("Login Failure")));

        this.trafficConnector = UvmContextFactory.context().pipelineFoundry().create("capture-octet", this, null, new CaptivePortalTrafficHandler( this ), Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -2, false);
        this.httpsConnector = UvmContextFactory.context().pipelineFoundry().create("capture-https", this, httpsSub, httpsHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -1, false);
        this.httpConnector = UvmContextFactory.context().pipelineFoundry().create("capture-http", this, null, new CaptivePortalHttpHandler( this) , Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, -1, false);
        this.connectors = new PipelineConnector[] { trafficConnector, httpsConnector, httpConnector };
    }

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * Get the application settings
     * 
     * @return The settings for the application instance
     */
    public CaptivePortalSettings getSettings()
    {
        return (this.captureSettings);
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new application settings
     */
    public void setSettings(CaptivePortalSettings newSettings)
    {
        // this is an old settings that is no longer used so always
        // set to null so it will be removed from the config file
        newSettings.setCheckServerCertificate(null);

        // first we commit the new settings to disk
        saveAppSettings(newSettings);

        // next we call the function to activate the new settings
        applyAppSettings(newSettings);

        // finally we validate all of the active sessions and cleanup
        // anything that is not allowed based on the new settings
        validateAllSessions(null);
    }

    /**
     * @return A list of active captive portal users
     */
    public ArrayList<CaptivePortalUserEntry> getActiveUsers()
    {
        return (captureUserTable.buildUserList());
    }

    /**
     * Increment a blinger
     * 
     * @param blingerType
     *        The blinger to increment
     * 
     * @param delta
     *        The amount to increment
     */
    public void incrementBlinger(BlingerType blingerType, long delta)
    {
        switch (blingerType)
        {
        case SESSALLOW:
            adjustMetric(STAT_SESSALLOW, delta);
            break;
        case SESSBLOCK:
            adjustMetric(STAT_SESSBLOCK, delta);
            break;
        case SESSQUERY:
            adjustMetric(STAT_SESSQUERY, delta);
            break;
        case AUTHGOOD:
            adjustMetric(STAT_AUTHGOOD, delta);
            break;
        case AUTHFAIL:
            adjustMetric(STAT_AUTHFAIL, delta);
            break;
        }
    }

    /**
     * Initialize new applications settings
     */
    @Override
    public void initializeSettings()
    {
        logger.info("Initializing default app settings");

        // create a new settings object
        CaptivePortalSettings localSettings = new CaptivePortalSettings();

        // setup all the defaults
        BrandingManager brand = UvmContextFactory.context().brandingManager();

        localSettings.setBasicLoginPageTitle("Captive Portal");
        localSettings.setBasicLoginPageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        localSettings.setBasicLoginUsername("Username:");
        localSettings.setBasicLoginPassword("Password:");
        localSettings.setBasicLoginMessageText("Please enter your username and password to connect to the internet.");
        localSettings.setBasicLoginFooter("If you have any questions, please contact your network administrator.");
        localSettings.setBasicMessagePageTitle("Captive Portal");
        localSettings.setBasicMessagePageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        localSettings.setBasicMessageMessageText("Click Continue to connect to the Internet.");
        localSettings.setBasicMessageAgreeBox(false);
        localSettings.setBasicMessageAgreeText("Clicking here means you agree to the terms above.");
        localSettings.setBasicMessageFooter("If you have any questions, please contact your network administrator.");

        // create a few example rules
        List<CaptureRule> ruleList = new LinkedList<>();
        LinkedList<CaptureRuleCondition> matcherList = null;

        // example interface rule
        CaptureRuleCondition interfaceMatch = new CaptureRuleCondition(CaptureRuleCondition.ConditionType.SRC_INTF, "non_wan");
        matcherList = new LinkedList<>();
        matcherList.add(interfaceMatch);
        ruleList.add(new CaptureRule(false, matcherList, true, "Capture all traffic on all non-WAN interfaces"));

        localSettings.setCaptureRules(ruleList);

        initializeCookieKey(localSettings);

        // save the settings to disk
        saveAppSettings(localSettings);

        // apply the new settings to the app
        applyAppSettings(localSettings);
    }

    /**
     * Initialize the cookie key used for cookie based authentication.
     * 
     * @param settings
     *        The appliation settings
     */
    private void initializeCookieKey(CaptivePortalSettings settings)
    {
        byte[] binaryKey = new byte[8];
        new java.util.Random().nextBytes(binaryKey);
        settings.initBinaryKey(binaryKey);
    }

    /**
     * Load the saved application settings.
     * 
     * @return The loaded application settings, or null if none were found
     */
    private CaptivePortalSettings loadAppSettings()
    {
        CaptivePortalSettings readSettings = null;

        try {
            readSettings = settingsManager.load(CaptivePortalSettings.class, settingsFile);
        } catch (Exception e) {
            logger.warn("Error loading app settings", e);
            return (null);
        }

        if (readSettings == null) return (null);

        logger.info("Loaded app settings from " + settingsFile);

        // if the old check certificate boolean is present we use it
        // to initialize the new certificate detection option
        Boolean oldCertCheck = readSettings.getCheckServerCertificate();
        if ((oldCertCheck != null) && (oldCertCheck.booleanValue() == true)) {
            readSettings.setCertificateDetection(CaptivePortalSettings.CertificateDetection.CHECK_CERTIFICATE);
        }

        // we're retiring auth type ANY since there are multiply ANY options
        if (readSettings.getAuthenticationType() == CaptivePortalSettings.AuthenticationType.ANY) {
            readSettings.setAuthenticationType(CaptivePortalSettings.AuthenticationType.ANY_DIRCON);
        }

        return (readSettings);
    }

    /**
     * Save the application settings
     * 
     * @param argSettings
     *        The application settings
     */
    private void saveAppSettings(CaptivePortalSettings argSettings)
    {
        // set a unique id for each capture rule
        int idx = this.getAppSettings().getPolicyId().intValue() * 100000;
        for (CaptureRule rule : argSettings.getCaptureRules())
            rule.setId(++idx);

        try {
            settingsManager.save(settingsFile, argSettings);
        } catch (Exception e) {
            logger.warn("Error in saveAppSettings", e);
            return;
        }

        logger.info("Saved app settings to " + settingsFile);
    }

    /**
     * this function is called when settings are loaded or initialized it gives
     * us a single place to do stuff when applying a new settings object to the
     * app.
     * 
     * @param argSettings
     *        The application settings to apply
     */
    private void applyAppSettings(CaptivePortalSettings argSettings)
    {
        this.captureSettings = argSettings;
    }

    /**
     * Called when settings are changed to validate all active sessions and
     * terminate any session that would not be allowed based on the updated
     * configuration.
     * 
     * @param argAddress
     *        Optional address of a client being logged out for whom all active
     *        sessions should be terminated.
     */
    private void validateAllSessions(InetAddress argAddress)
    {
        final InetAddress userAddress = argAddress;

        // shut down any outstanding sessions that would not
        // be allowed based on the active app settings
        this.killMatchingSessions(new SessionMatcher()
        {
            List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            /**
             * For every session we have to check all the rules to make sure we
             * don't kill anything that shouldn't be captured.
             * 
             * @param policyId
             * @param protocol
             * @param clientIntf
             * @param serverIntf
             * @param clientAddr
             * @param serverAddr
             * @param clientPort
             * @param serverPort
             * @param attachments
             * @return True if the session matches, otherwise false
             */
            public boolean isMatch(Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String, Object> attachments)
            {
                // if userAddress is not null and this session is for someone
                // other than userAddress then we just leave it alone
                if ((userAddress != null) && (clientAddr.equals(userAddress) == false)) return (false);

                // TODO - deal with MAC or IP

                // if session is for any active authenticated IP user return false
                if (captureUserTable.searchByAddress(clientAddr.getHostAddress().toString()) != null) return (false);

                // also check if there is an active authenticated MAC user
                HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(clientAddr);
                if (entry != null) {
                    if (entry.getMacAddress() != null) {
                        if (captureUserTable.searchByAddress(entry.getMacAddress()) != null) return (false);
                    }
                }

                // if session matches any pass list return false
                if (isSessionAllowed(clientAddr, serverAddr) != null) return (false);

                // check the session against the rule list
                for (CaptureRule rule : ruleList) {
                    if (rule.isMatch(protocol, clientIntf, serverIntf, clientAddr, serverAddr, clientPort, serverPort)) {
                        // on a matching rule continue if capture is false
                        if (rule.getCapture() == false) continue;

                        // capture is true so log and kill the session
                        logger.debug("Validate killing " + clientAddr.getHostAddress().toString() + ":" + clientPort + " --> " + serverAddr.getHostAddress().toString() + ":" + serverPort);
                        return (true);
                    }
                }

                // no matches anywhere so leave the session alone
                return (false);
            }
        });
    }

    /**
     * Return the application pipeline connectors.
     * 
     * @return The application pipeline connectors.
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called before the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        // create the custom path for this application instance
        String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + getAppSettings().getId().toString());

        // load user state from file (if exists)
        loadUserState();

        // run a script to create the directory for the custom captive page
        UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_CREATE_SCRIPT + " " + customPath);
    }

    /**
     * Called after the application is started.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStart(boolean isPermanentTransition)
    {
        logger.debug("Creating session cleanup timer task");
        captureTimer = new CaptivePortalTimer(this);
        timer = new Timer();
        timer.schedule(captureTimer, CLEANUP_INTERVAL, CLEANUP_INTERVAL);

        // register our host table and username check callback hooks
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.HOST_TABLE_REMOVE, this.hostRemovedCallback);
        UvmContextFactory.context().hookManager().registerCallback(com.untangle.uvm.HookManager.CAPTURE_USERNAME_CHECK, this.usernameCheckCallback);
    }

    /**
     * Called before the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStop(boolean isPermanentTransition)
    {
        // unregister our host table and username check callback hooks
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.HOST_TABLE_REMOVE, this.hostRemovedCallback);
        UvmContextFactory.context().hookManager().unregisterCallback(com.untangle.uvm.HookManager.CAPTURE_USERNAME_CHECK, this.usernameCheckCallback);

        // stop the session cleanup timer thread
        logger.debug("Destroying session cleanup timer task");
        timer.cancel();

        // shutdown any active sessions
        killAllSessions();

        // save user state to file
        saveUserState();

        // clear out the list of active users
        captureUserTable.purgeAllUsers();
        captureUserCookieTable.purgeAllUsers();
    }

    /**
     * Called after the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag.
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
    }

    /**
     * Called after application initialization.
     */
    @Override
    protected void postInit()
    {
        CaptivePortalSettings readSettings = loadAppSettings();

        if (readSettings == null) {
            // we didn't get anything from the load function so we call
            // the initialize function which will take care of
            // creating, writing, and applying a new settings object
            initializeSettings();
        } else {
            // we got something back from the load so pass it
            // to the common apply function
            if (readSettings.getSecretKey() == null) {
                initializeCookieKey(readSettings);
                saveAppSettings(readSettings);
            }
            applyAppSettings(readSettings);
        }
    }

    /**
     * Called when the application is uninstalled.
     */
    @Override
    protected void uninstall()
    {
        super.uninstall();

        // remove the custom path for this application instance
        String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + getAppSettings().getId().toString());

        // run a script to remove the directory for the custom captive page
        UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_REMOVE_SCRIPT + " " + customPath);
    }

    /**
     * Function to call our HTTP replacement generator to create the block page
     * we return when traffic is captured for an unauthenticated user.
     * 
     * @param block
     *        The block details
     * @param session
     *        The session details
     * @return The response token
     */
    protected Token[] generateResponse(CaptivePortalBlockDetails block, AppTCPSession session)
    {
        return replacementGenerator.generateResponse(block, session);
    }

    /**
     * Attempt to authenticate a captive portal user.
     * 
     * @param address
     *        The client address
     * @param username
     *        The username supplied by the client
     * @param password
     *        The password supplied by the client
     * @return Zero for successful authentication, any other value for failure
     */
    public int userAuthenticate(InetAddress address, String username, String password)
    {
        boolean isAuthenticated = false;

        try {
            password = URLDecoder.decode(password, "UTF-8");
        }

        catch (Exception e) {
            logger.warn("Using raw password due to URLDecodeer exception", e);
        }

        if (captureSettings.getConcurrentLoginsEnabled() == false) {
            boolean ignoreCase = false;

            if (captureSettings.getAuthenticationType() == CaptivePortalSettings.AuthenticationType.ACTIVE_DIRECTORY) {
                ignoreCase = true;
            }

            CaptivePortalUserEntry user = captureUserTable.searchByUsername(username, ignoreCase);

            // when concurrent logins are disabled and we have an active entry for the user
            // we check the address and ignore the match if they are the same since it's
            // not really a concurrent login but a duplicate login from the same client
            if ((user != null) && (address.getHostAddress().toString().equals(user.getUserAddress()) == false)) {
                CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
                logEvent(event);
                incrementBlinger(BlingerType.AUTHFAIL, 1);
                logger.info("Authenticate duplicate " + username + " " + address.getHostAddress().toString());
                return (2);
            }
        }

        switch (captureSettings.getAuthenticationType())
        {
        case NONE:
            isAuthenticated = true;
            break;

        case ACTIVE_DIRECTORY:
            try {
                // first create a copy of the original username and another
                // that is stripped of all the Active Directory foo:
                // domain*backslash*user -> user
                // user@domain -> user
                // We'll always use the stripped version internally but
                // well try both for authentication. See bug #7951
                String originalUsername = username;
                String strippedUsername = username;
                strippedUsername = strippedUsername.replaceAll(".*\\\\", "");
                strippedUsername = strippedUsername.replaceAll("@.*", "");

                // we always want to use the stripped version internally
                username = strippedUsername;

                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector == null) break;

                // try the original first and then the stripped version
                isAuthenticated = directoryConnector.activeDirectoryAuthenticate(originalUsername, password);
                if (isAuthenticated == false) isAuthenticated = directoryConnector.activeDirectoryAuthenticate(strippedUsername, password);
            } catch (Exception e) {
                logger.warn("Active Directory authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case LOCAL_DIRECTORY:
            try {
                isAuthenticated = UvmContextFactory.context().localDirectory().authenticate(username, password);
            } catch (Exception e) {
                logger.warn("Local Directory authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case RADIUS:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.radiusAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Radius authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case ANY_DIRCON:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().appManager().app("directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.anyAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("ANY_DIRCON authentication failure", e);
                isAuthenticated = false;
            }
            break;
        default:
            logger.error("Unknown Authenticate Method: " + captureSettings.getAuthenticationType());

        }

        if (!isAuthenticated) {
            CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL, 1);
            logger.info("Authenticate failure " + username + " " + address.getHostAddress().toString());
            return (1);
        }

        publishActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Authenticate success " + username + " " + address.getHostAddress().toString());
        return (0);
    }

    /**
     * Called to activate a captive portal user without validating a password.
     * Normally called when captive portal is configured in basic message mode
     * without user authentication.
     * 
     * @param address
     *        The address of the client
     * @param username
     *        The username for the client
     * @param agree
     *        The state of the agree checkbox
     * @param anonymous
     *        True when the user is anonymous
     * @return Zero for successful activation, any other value for failure
     */
    public int userActivate(InetAddress address, String username, String agree, boolean anonymous)
    {
        if (agree.equals("agree") == false) {
            CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL, 1);
            logger.info("Activate failure " + address.getHostAddress().toString());
            return (1);
        }

        publishActiveUser(address, username, anonymous);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Activate success " + address.getHostAddress().toString());

        return (0);
    }

    /**
     * Called by handler.py to activate an anonymous user when basic message
     * mode is enabled with no user authentication.
     * 
     * @param address
     *        The client address
     * @param agree
     *        The text from the agree control from the posted form
     * @return Zero for successful activation, any other value for failure
     */
    public int userActivate(InetAddress address, String agree)
    {
        return userActivate(address, "Anonymous", agree, true);
    }

    /**
     * Called to login a user when authentication is provided by some external
     * mechanism, such as with a custom script or via cookie.
     * 
     * @param address
     *        The address of the client
     * @param username
     *        The username for the client
     * @return Zero for successful login, any other value for failure
     */
    public int userLogin(InetAddress address, String username)
    {
        publishActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, CaptivePortalSettings.AuthenticationType.CUSTOM, CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Login success " + address.getHostAddress().toString());

        if (captureSettings.getSessionCookiesEnabled()) {
            captureUserCookieTable.removeActiveUser(address.getHostAddress().toString());
        }

        return (0);
    }

    /**
     * Called to login a Google OAuth-thenticated user
     * 
     * @param address
     *        The client address
     * @param username
     *        The client username
     * @return Zero for successful login
     */
    public int googleLogin(InetAddress address, String username)
    {
        publishActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, CaptivePortalSettings.AuthenticationType.GOOGLE, CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Google OAuth success " + address.getHostAddress().toString());

        return (0);
    }

    /**
     * Called to login a Facebook OAuth-thenticated user
     * 
     * @param address
     *        The client address
     * @param username
     *        The client username
     * @return Zero for successful login
     */
    public int facebookLogin(InetAddress address, String username)
    {
        publishActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, CaptivePortalSettings.AuthenticationType.FACEBOOK, CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Facebook OAuth success " + address.getHostAddress().toString());

        return (0);
    }

    /**
     * Called to login a Microsoft OAuth-thenticated user
     * 
     * @param address
     *        The client addreess
     * @param username
     *        The client username
     * @return Zero for successful login
     */
    public int microsoftLogin(InetAddress address, String username)
    {
        publishActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address.getHostAddress().toString(), username, CaptivePortalSettings.AuthenticationType.MICROSOFT, CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Microsoft OAuth success " + address.getHostAddress().toString());

        return (0);
    }

    /**
     * Called to logout a user
     * 
     * @param address
     *        The client address to be logged out
     * @return Zero for logout success, any other value for failure
     */
    public int userLogout(InetAddress address)
    {
        return (userLogout(address, CaptivePortalUserEvent.EventType.USER_LOGOUT));
    }

    /**
     * Called to logout a user for a specific reason
     * 
     * @param address
     *        The client address to be logged out
     * @param reason
     *        The reason for the logout event
     * @return Zero for logout success, any other value for failure
     */
    public int userLogout(InetAddress address, CaptivePortalUserEvent.EventType reason)
    {
        String userkey = null;

        // if using MAC tracking we want to search using client MAC address
        if (getSettings().getUseMacAddress()) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(address);
            if (entry != null) userkey = entry.getMacAddress();
        }

        // use the IP address if MAC tracking not active or no MAC available 
        if (userkey == null) {
            userkey = address.getHostAddress().toString();
        }

        CaptivePortalUserEntry user = captureUserTable.searchByAddress(userkey);

        if (user == null) {
            logger.info("Logout failure: " + userkey);
            return (1);
        }

        // remove from the user table
        destroyActiveUser(user);

        // call the session cleanup function passing the address of the
        // user we just logged out to clean up any outstanding sessions
        validateAllSessions(address);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, user.getUserAddress(), user.getUserName(), captureSettings.getAuthenticationType(), reason);
        logEvent(event);
        logger.info("Logout success: " + user.toString());

        if (captureSettings.getSessionCookiesEnabled() && ((reason == CaptivePortalUserEvent.EventType.USER_LOGOUT) || (reason == CaptivePortalUserEvent.EventType.ADMIN_LOGOUT))) {
            captureUserCookieTable.insertInactiveUser(user);
        }

        return (0);
    }

    /**
     * Called by the UI to force logout a user in the status grid
     * 
     * @param userkey
     *        The IP or MAC address of the user to be logged out
     * @return Zero for logout success, any other value for failure
     */
    public int userAdminLogout(String userkey)
    {
        return (userForceLogout(userkey, CaptivePortalUserEvent.EventType.ADMIN_LOGOUT));
    }

    /**
     *
     * @param userkey
     *        The IP or MAC address of the user to be logged out
     * @param reason
     *        The reason the user is being logged out
     * @return Zero for logout success, any other value for failure
     */
    public int userForceLogout(String userkey, CaptivePortalUserEvent.EventType reason)
    {
        HostTableEntry entry = null;
        InetAddress netaddr = null;

        CaptivePortalUserEntry user = captureUserTable.searchByAddress(userkey);

        if (user == null) {
            logger.info("Force logout failure: " + userkey);
            return (1);
        }

        // remove from the user table
        destroyActiveUser(user);

        if (user.getUserAddress().indexOf(':') >= 0) {
            // any semi-colon in the key indicate a MAC address                
            entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(user.getUserAddress());
        } else {
            // not a mac address so convert to InetAddr object
            entry = UvmContextFactory.context().hostTable().getHostTableEntry(user.getUserAddress());
        }

        // if we found a host table entry call the session cleanup function
        // passing the current IP address to cleanup outstanding sessions
        if (entry != null) {
            validateAllSessions(entry.getAddress());
        }

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, user.getUserAddress(), user.getUserName(), captureSettings.getAuthenticationType(), reason);
        logEvent(event);
        logger.info("Force logout success: " + user.toString());

        if (captureSettings.getSessionCookiesEnabled() && ((reason == CaptivePortalUserEvent.EventType.USER_LOGOUT) || (reason == CaptivePortalUserEvent.EventType.ADMIN_LOGOUT))) {
            captureUserCookieTable.insertInactiveUser(user);
        }

        return (0);
    }

    /**
     * Called after any successfull login to add the user to the active user
     * table and update the system host table to prevent the entry from being
     * removed while the client is an active captive portal user.
     * 
     * @param address
     *        The address of the client
     * @param username
     *        The username of the client
     * @param anonymous
     *        The anonymous flag for the login
     */
    protected void publishActiveUser(InetAddress address, String username, Boolean anonymous)
    {
        CaptivePortalUserEntry user = null;
        String userkey = null;

        // get the host table entry and include the create flag
        HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(address, true);

        // if using MAC tracking we want to get the client MAC address
        if ((getSettings().getUseMacAddress()) && (entry != null)) {
            userkey = entry.getMacAddress();
        }

        // use the IP address if MAC tracking not active or no MAC available 
        if (userkey == null) {
            userkey = address.getHostAddress().toString();
        }

        user = captureUserTable.insertActiveUser(userkey, username, anonymous);

        // should never happen but we check just in case
        if (entry == null) {
            logger.warn("Failed to create host table entry for: " + user.toString());
            return;
        }

        /**
         * For anonymous users clear the global capture username which shouldn't
         * be required but always better safe than sorry. For all other users we
         * set the clobal capture username. We also set the captive portal flag
         * to prevent the entry from being timed-out while the user is active in
         * our table.
         */

        if (anonymous == Boolean.TRUE) {
            entry.setUsernameCaptivePortal(null);
            entry.setCaptivePortalAuthenticated(true);
        } else {
            entry.setUsernameCaptivePortal(username);
            entry.setCaptivePortalAuthenticated(true);
        }
    }

    /**
     * Called for any user logout to remove the user from the active user table
     * and clear our special entries in the system host table.
     * 
     * @param user
     *        The user object to be removed
     */
    protected void destroyActiveUser(CaptivePortalUserEntry user)
    {
        HostTableEntry entry = null;

        captureUserTable.removeActiveUser(user.getUserAddress());

        if (user.getUserAddress().indexOf(':') >= 0) {
            // any semi-colon in the key indicate a MAC address                
            entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(user.getUserAddress());
        } else {
            // not a mac address so do normal lookup by IP address
            entry = UvmContextFactory.context().hostTable().getHostTableEntry(user.getUserAddress());
        }

        if (entry == null) {
            logger.warn("Missing host table entry for: " + user.toString());
            return;
        }

        entry.setUsernameCaptivePortal(null);
        entry.setCaptivePortalAuthenticated(false);
    }

    /**
     * Called by the traffic handlers to allow passing traffic for authenticated
     * users.
     * 
     * @param clientAddr
     *        The client address for the traffic
     * @return True if the client is authenticated, otherwise false
     */
    public boolean isClientAuthenticated(InetAddress clientAddr)
    {
        CaptivePortalUserEntry user = null;
        String userkey = null;

        // if using MAC tracking we want to search using client MAC address
        if (getSettings().getUseMacAddress()) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(clientAddr);
            if (entry != null) userkey = entry.getMacAddress();
        }

        // use the IP address if MAC tracking not active or no MAC available 
        if (userkey == null) {
            userkey = clientAddr.getHostAddress().toString();
        }

        user = captureUserTable.searchByAddress(userkey);

        if (user != null) {
            user.updateActivityTimer();
            return (true);
        }

        return (false);
    }

    /**
     * Called by the traffic handlers to allow passing traffic for hosts matched
     * in the passed client or server lists.
     * 
     * @param clientAddr
     *        The client address for the traffic
     * @param serverAddr
     *        The server address for the traffic
     * @return True if session should be allowed, otherwise false
     */
    public PassedAddress isSessionAllowed(InetAddress clientAddr, InetAddress serverAddr)
    {
        List<PassedAddress> clientList = getSettings().getPassedClients();
        List<PassedAddress> serverList = getSettings().getPassedServers();
        PassedAddress checker = null;

        // see if the client is in the pass list
        for (int cc = 0; cc < clientList.size(); cc++) {
            checker = clientList.get(cc);
            if (checker.getEnabled() != true) continue;
            if (checker.getAddress().isMatch(clientAddr) != true) continue;
            logger.debug("Client " + clientAddr.getHostAddress().toString() + " found in pass list");
            return (checker);
        }

        // see if the server is in the pass list
        for (int ss = 0; ss < serverList.size(); ss++) {
            checker = serverList.get(ss);
            if (checker.getEnabled() != true) continue;
            if (checker.getAddress().isMatch(serverAddr) != true) continue;
            logger.debug("Server " + serverAddr.getHostAddress().toString() + " found in pass list");
            return (checker);
        }

        return (null);
    }

    /**
     * Called by the traffic handlers to check traffic against capture rules.
     * 
     * @param sessreq
     *        The new session request
     * @return The first matching rule or null when no match is found
     */
    public CaptureRule checkCaptureRules(IPNewSessionRequest sessreq)
    {
        List<CaptureRule> ruleList = captureSettings.getCaptureRules();

        // check the session against the rule list
        for (CaptureRule rule : ruleList) {
            if (rule.isMatch(sessreq.getProtocol(), sessreq.getClientIntf(), sessreq.getServerIntf(), sessreq.getOrigClientAddr(), sessreq.getNewServerAddr(), sessreq.getOrigClientPort(), sessreq.getNewServerPort())) {
                return (rule);
            }
        }

        return (null);
    }

    /**
     * Called by the traffic handlers to check traffic against capture rules.
     * 
     * @param session
     *        The TCP session
     * 
     * @return The first matching rule or null when no match is found
     */
    public CaptureRule checkCaptureRules(AppTCPSession session)
    {
        List<CaptureRule> ruleList = captureSettings.getCaptureRules();

        // check the session against the rule list
        for (CaptureRule rule : ruleList) {
            if (rule.isMatch(session)) {
                return (rule);
            }
        }

        return (null);
    }

    /**
     * Checks to see if a user is in the cookie table
     * 
     * @param address
     *        The address of the client
     * @param username
     *        The username for the client
     * @return True if found in table, otherwise false
     */
    public boolean isUserInCookieTable(String address, String username)
    {
        return captureUserCookieTable.searchByAddressUsername(address, username) != null;
    }

    /**
     * Remove a user from the cookie table
     * 
     * @param address
     *        The address of the user to be removed
     */
    public void removeUserFromCookieTable(String address)
    {
        captureUserCookieTable.removeActiveUser(address);
    }

    /**
     * This runs the period clean up task and is used by the test suite.
     */
    public void runCleanup()
    {
        if (captureTimer != null) captureTimer.run();
    }

    /**
     * Attempt to load any relevent user state from a file if it exists
     */
    @SuppressWarnings("unchecked")
    private void loadUserState()
    {
        try {
            String filename = System.getProperty("uvm.conf.dir") + "/capture-users-" + this.getAppSettings().getId().toString() + ".js";
            /**
             * If there is no save file, just return
             */
            File saveFile = new File(filename);
            if (!saveFile.exists()) return;

            logger.info("Loading user state from file... ");
            ArrayList<CaptivePortalUserEntry> userlist = UvmContextFactory.context().settingsManager().load(ArrayList.class, filename);

            HostTableEntry entry;
            long userTimeout = getSettings().getUserTimeout();
            long currentTime = System.currentTimeMillis();
            boolean macAddressFlag;
            int usersLoaded = 0;

            /**
             * Insert all the non-expired users into the table. Since the
             * untangle-vm has likely been down, don't check idle timeout.
             */
            for (CaptivePortalUserEntry user : userlist) {
                long userTrigger = (user.getSessionCreation() + (userTimeout * 1000));

                /**
                 * Don't pass the create flag when checking for the host table
                 * here
                 */
                if (user.getUserAddress().indexOf(':') >= 0) {
                    // any semi-colon in the key indicate a MAC address                
                    entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(user.getUserAddress());
                    macAddressFlag = true;
                } else {
                    // not a mac address so do normal lookup by IP address
                    entry = UvmContextFactory.context().hostTable().getHostTableEntry(user.getUserAddress());
                    macAddressFlag = false;
                }

                /**
                 * If we aren't loading this expired user we need to clear our
                 * username and authenticated fields in the host table if we
                 * found a corresponding entry.
                 */
                if (currentTime > userTrigger) {
                    if (entry != null) {
                        entry.setUsernameCaptivePortal(null);
                        entry.setCaptivePortalAuthenticated(false);
                    }
                    continue;
                }

                /**
                 * User is not expired so we add to our table and set the name
                 * and authenticated fields in the host table. If we didn't find
                 * a host table entry and the user key is a MAC address there
                 * isn't anything we can do, otherwise we create the host table
                 * entry.
                 */
                if ((entry == null) && (macAddressFlag == true)) continue;

                if (entry == null) {
                    InetAddress addr;
                    try {
                        addr = InetAddress.getByName(user.getUserAddress());
                        entry = UvmContextFactory.context().hostTable().getHostTableEntry(addr, true);
                    } catch (java.net.UnknownHostException e) {
                        continue;
                    }
                }

                captureUserTable.insertActiveUser(user);
                entry.setUsernameCaptivePortal(user.getUserName());
                entry.setCaptivePortalAuthenticated(true);
                usersLoaded++;
            }

            /**
             * Delete the save file
             */
            saveFile.delete();

            logger.info("Loading user state from file... (" + usersLoaded + " entries)");

        } catch (Exception e) {
            logger.warn("Exception loading user state", e);
        }
    }

    /**
     * This method saves the current user state in a file in the conf directory
     * so we preserve user login state on untangle-vm or server reboots
     */
    private void saveUserState()
    {
        try {
            String filename = System.getProperty("uvm.conf.dir") + "/capture-users-" + this.getAppSettings().getId().toString() + ".js";
            ArrayList<CaptivePortalUserEntry> users = this.captureUserTable.buildUserList();
            if (users.size() < 1) return;

            logger.info("Saving user state to file... (" + users.size() + " entries)");
            UvmContextFactory.context().settingsManager().save(filename, users, false, false);
            logger.info("Saving user state to file... done");
        } catch (Exception e) {
            logger.warn("Exception saving user state", e);
        }
    }

    /**
     * This is called by the UI to upload custom captive pages.
     */
    private class CustomPageUploadHandler implements UploadHandler
    {
        /**
         * @return The path name for this upload handler
         */
        @Override
        public String getName()
        {
            return "CaptivePortal/custom_upload";
        }

        /**
         * Called when an file upload is submitted.
         * 
         * @param fileItem
         * @param argument
         * @return The result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            logger.info("CUSTOM UPLOAD: " + fileItem.getName() + " ARGUMENT: " + argument);

            // create the custom path for this application instance
            String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + argument);

            /*
             * save the uploaded file to disk so we can work on it
             */
            File tempFile = new File(CAPTURE_TEMPORARY_UPLOAD);
            if (tempFile.exists()) tempFile.delete();
            java.io.FileOutputStream tempStream = new FileOutputStream(tempFile);
            tempStream.write(fileItem.get());
            tempStream.close();

            /*
             * make sure we have a valid zip file and check the contents to see
             * if there is either a custom.html or custom.py script
             */
            ZipFile zipFile = null;
            try {
                int checker = 0;
                zipFile = new ZipFile(tempFile);
                Enumeration<? extends ZipEntry> zipList = zipFile.entries();

                while (zipList.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) zipList.nextElement();
                    String fileName = zipEntry.getName();
                    logger.debug("Custom zip contents: " + fileName);
                    if (fileName.equals("custom.html") == true) checker += 1;
                    if (fileName.equals("custom.py") == true) checker += 1;
                }

                if (checker == 0) {
                    tempFile.delete();
                    return new ExecManagerResult(1, "The uploaded ZIP file does not contain custom.html or custom.py in the base/parent directory");
                }

            } catch (ZipException zip) {
                tempFile.delete();
                return new ExecManagerResult(1, "The uploaded file does not appear to be a valid ZIP archive");
            } catch (Exception exn) {
                tempFile.delete();
                return new ExecManagerResult(1, exn.getMessage());
            } finally {
                if (zipFile != null){
                    try{
                        zipFile.close();
                    } catch (Exception exn) {
                        return new ExecManagerResult(1, exn.getMessage());
                    }
                }
            }

            /*
             * We seem to have a good ZIP archive and it contains the files we
             * expect so clean up any existing custom page that may already
             * exist and extract the file into our custom directory
             */
            UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_REMOVE_SCRIPT + " " + customPath);
            UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_CREATE_SCRIPT + " " + customPath);
            UvmContextFactory.context().execManager().exec("unzip -o " + CAPTURE_TEMPORARY_UPLOAD + " -d " + customPath);

            tempFile.delete();
            logger.debug("Custom zip uploaded to: " + customPath);
            return new ExecManagerResult(0, fileItem.getName());
        }
    }

    /**
     * This is called by the UI to remove custom captive pages.
     */
    private class CustomPageRemoveHandler implements UploadHandler
    {
        /**
         * @return The path name for this upload handler
         */
        @Override
        public String getName()
        {
            return "CaptivePortal/custom_remove";
        }

        /**
         * Called when an uploaded file is removed.
         * 
         * @param fileItem
         * @param argument
         * @return The result
         * @throws Exception
         */
        @Override
        public ExecManagerResult handleFile(FileItem fileItem, String argument) throws Exception
        {
            logger.info("CUSTOM REMOVE: " + fileItem.getName() + " ARGUMENT: " + argument);

            // create the custom path for this application instance
            String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + argument);

            // use our existing remove and create scripts to wipe any existing custom page
            UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_REMOVE_SCRIPT + " " + customPath);
            UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_CREATE_SCRIPT + " " + customPath);

            logger.debug("Custom zip removed from: " + customPath);
            return new ExecManagerResult(0, "The custom captive portal page has been removed");
        }
    }

    /**
     * This hook is called when a host is removed from the host table. If the
     * user is logged into captive portal the host table entry should never be
     * removed. However it is removed if the MAC address changes (a different
     * host) or something drastic occurs. In these cases we should log the host
     * out.
     */
    private class HostRemovedHookCallback implements HookCallback
    {
        /**
         * @return The name of this callback hook.
         */
        public String getName()
        {
            return "captive-portal-" + getAppSettings().getId().toString() + "-host-removed-hook";
        }

        /**
         * This is the callback function.
         * 
         * @param args
         *        The arguments passed to the callback
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof InetAddress)) {
                logger.warn("Invalid argument: " + o);
                return;
            }
            InetAddress addr = (InetAddress) o;
            if (isClientAuthenticated(addr)) userLogout(addr, CaptivePortalUserEvent.EventType.HOST_CHANGE);
        }
    }

    /**
     * This hook is called by the host table cleanup thread. We receive a
     * HookBucket object that contains a captive portal username from the host
     * table. If the user is logged in, we increment the number in the bucket to
     * signal that the user is currently logged in.
     */
    private class CaptureUsernameHookCallback implements HookCallback
    {

        /**
         * Gets the name for the callback hook
         * 
         * @return The name of the callback hook
         */
        public String getName()
        {
            return "captive-portal-" + getAppSettings().getId().toString() + "-username-check-hook";
        }

        /**
         * This is the callback function.
         *
         * @param args
         *        The arguments passed to the callback
         */
        public void callback(Object... args)
        {
            Object o = args[0];
            if (!(o instanceof HookBucket)) {
                logger.warn("Invalid argument: " + o);
                return;
            }

            HookBucket bucket = (HookBucket) o;

            /**
             * We search for an active user with a name matching the string in
             * the bucket. If the user is logged in, we increment the number in
             * the bucket to prevent the name from being removed from the host
             * table.
             */
            try {
                CaptivePortalUserEntry entry = captureUserTable.searchByUsername(bucket.getString(), true);
                if (entry != null) bucket.incrementNumber();
            } catch (Exception exn) {
                logger.warn("Exception in username callback checking:" + bucket.getString(), exn);
            }
        }
    }
}
