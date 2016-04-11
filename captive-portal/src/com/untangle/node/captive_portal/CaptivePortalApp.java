/**
 * $Id$
 */
package com.untangle.node.captive_portal;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TimerTask;
import java.util.Timer;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.io.File;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.node.http.ReplacementGenerator;

public class CaptivePortalApp extends NodeBase
{
    public enum BlingerType
    {
        SESSALLOW, SESSBLOCK, SESSQUERY, AUTHGOOD, AUTHFAIL
    }

    private final int CLEANUP_INTERVAL = 60000;
    private final Logger logger = Logger.getLogger(getClass());
    private final Long policyId = getNodeSettings().getPolicyId();

    private final String CAPTURE_CUSTOM_CREATE_SCRIPT = System.getProperty("uvm.home") + "/bin/captive-portal-custom-create";
    private final String CAPTURE_CUSTOM_REMOVE_SCRIPT = System.getProperty("uvm.home") + "/bin/captive-portal-custom-remove";
    private final String CAPTURE_PERMISSIONS_SCRIPT = System.getProperty("uvm.home") + "/bin/captive-portal-permissions";

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
    private final String settingsFile = (System.getProperty("uvm.settings.dir") + "/untangle-node-captive-portal/settings_" + getNodeSettings().getId().toString()) + ".js";
    private final String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + getNodeSettings().getId().toString());

    protected CaptivePortalUserTable captureUserTable = new CaptivePortalUserTable();
    protected CaptivePortalUserCookieTable captureUserCookieTable = new CaptivePortalUserCookieTable();
    private CaptivePortalSettings captureSettings;
    private CaptivePortalTimer captureTimer;
    private Timer timer;

// THIS IS FOR ECLIPSE - @formatter:off

    public CaptivePortalApp( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        replacementGenerator = new CaptivePortalReplacementGenerator(getNodeSettings(),this);

        addMetric(new NodeMetric(STAT_SESSALLOW, I18nUtil.marktr("Sessions Allowed")));
        addMetric(new NodeMetric(STAT_SESSBLOCK, I18nUtil.marktr("Sessions Blocked")));
        addMetric(new NodeMetric(STAT_SESSQUERY, I18nUtil.marktr("DNS Lookups")));
        addMetric(new NodeMetric(STAT_AUTHGOOD, I18nUtil.marktr("Login Success")));
        addMetric(new NodeMetric(STAT_AUTHFAIL, I18nUtil.marktr("Login Failure")));

        this.trafficConnector = UvmContextFactory.context().pipelineFoundry().create("capture-octet", this, null, new CaptivePortalTrafficHandler( this ), Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -2, false);
        this.httpsConnector = UvmContextFactory.context().pipelineFoundry().create("capture-https", this, httpsSub, httpsHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.CLIENT, -1, false);
        this.httpConnector = UvmContextFactory.context().pipelineFoundry().create("capture-http", this, null, new CaptivePortalHttpHandler( this) , Fitting.HTTP_TOKENS, Fitting.HTTP_TOKENS, Affinity.CLIENT, -1, false);
        this.connectors = new PipelineConnector[] { trafficConnector, httpsConnector, httpConnector };
    }

// THIS IS FOR ECLIPSE - @formatter:on

    /**
     * The UI components seem to automagically call getSettings and setSettings
     * to handle the load and save stuff, so these functions just call our
     * getCaptivePortalSettings and setCaptivePortalSettings functions.
     */

    public CaptivePortalSettings getSettings()
    {
        return (getCaptivePortalSettings());
    }

    public void setSettings(CaptivePortalSettings newSettings)
    {
        this.setCaptivePortalSettings(newSettings);
    }

    public CaptivePortalSettings getCaptivePortalSettings()
    {
        return (this.captureSettings);
    }

    public void setCaptivePortalSettings(CaptivePortalSettings newSettings)
    {
        // this is an old settings that is no longer used so always
        // set to null so it will be removed from the config file
        newSettings.setCheckServerCertificate(null);

        // first we commit the new settings to disk
        saveNodeSettings(newSettings);

        // next we call the function to activate the new settings
        applyNodeSettings(newSettings);

        // finally we validate all of the active sessions and cleanup
        // anything that is not allowed based on the new settings
        validateAllSessions(null);
    }

    public ArrayList<CaptivePortalUserEntry> getActiveUsers()
    {
        return (captureUserTable.buildUserList());
    }

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

    @Override
    public void initializeSettings()
    {
        logger.info("Initializing default node settings");

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
        List<CaptureRule> ruleList = new LinkedList<CaptureRule>();
        LinkedList<CaptureRuleCondition> matcherList = null;

        // example interface rule
        CaptureRuleCondition interfaceMatch = new CaptureRuleCondition(CaptureRuleCondition.ConditionType.SRC_INTF, "non_wan");
        matcherList = new LinkedList<CaptureRuleCondition>();
        matcherList.add(interfaceMatch);
        ruleList.add(new CaptureRule(false, matcherList, true, "Capture all traffic on all non-WAN interfaces"));

        localSettings.setCaptureRules(ruleList);

        initializeCookieKey(localSettings);

        // save the settings to disk
        saveNodeSettings(localSettings);

        // apply the new settings to the node
        applyNodeSettings(localSettings);
    }

    private void initializeCookieKey(CaptivePortalSettings settings)
    {
        byte[] binaryKey = new byte[8];
        new java.util.Random().nextBytes(binaryKey);
        settings.initBinaryKey(binaryKey);
    }

    private CaptivePortalSettings loadNodeSettings()
    {
        CaptivePortalSettings readSettings = null;

        try {
            readSettings = settingsManager.load(CaptivePortalSettings.class, settingsFile);
        } catch (Exception e) {
            logger.warn("Error loading node settings", e);
            return (null);
        }

        if (readSettings != null) logger.info("Loaded node settings from " + settingsFile);

        // if the old check certificate boolean is present we use it
        // to initialize the new certificate detection option
        Boolean oldCertCheck = readSettings.getCheckServerCertificate();
        if ((oldCertCheck != null) && (oldCertCheck.booleanValue() == true)) {
            readSettings.setCertificateDetection(CaptivePortalSettings.CertificateDetection.CHECK_CERTIFICATE);
        }

        return (readSettings);
    }

    private void saveNodeSettings(CaptivePortalSettings argSettings)
    {
        // set a unique id for each capture rule
        int idx = this.getNodeSettings().getPolicyId().intValue() * 100000;
        for (CaptureRule rule : argSettings.getCaptureRules())
            rule.setId(++idx);

        try {
            settingsManager.save(settingsFile, argSettings);
        } catch (Exception e) {
            logger.warn("Error in saveNodeSettings", e);
            return;
        }

        logger.info("Saved node settings to " + settingsFile);
    }

    private void applyNodeSettings(CaptivePortalSettings argSettings)
    {
        // this function is called when settings are loaded or initialized
        // it gives us a single place to do stuff when applying a new
        // settings object to the node.

        this.captureSettings = argSettings;
    }

    private void validateAllSessions(InetAddress argAddress)
    {
        final InetAddress userAddress = argAddress;

        // shut down any outstanding sessions that would not
        // be allowed based on the active node settings
        this.killMatchingSessions(new SessionMatcher()
        {
            List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            // for every session we have to check all the rules to make
            // sure we don't kill anything that shouldn't be captured
            public boolean isMatch(Long policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String, Object> attachments)
            {
                // if userAddress is not null and this session is for someone
                // other than userAddress then we just leave it alone
                if ((userAddress != null) && (clientAddr.equals(userAddress) == false)) return (false);

                // if session is for any active authenticated user return false
                if (captureUserTable.searchByAddress(clientAddr) != null) return (false);

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

    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    @Override
    protected void preStart()
    {
        // run a script to add www-data to the uvmlogin group
        UvmContextFactory.context().execManager().exec(CAPTURE_PERMISSIONS_SCRIPT);

        // run a script to create the directory for the custom captive page
        UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_CREATE_SCRIPT + " " + customPath);
    }

    @Override
    protected void postStart()
    {
        logger.debug("Creating session cleanup timer task");
        captureTimer = new CaptivePortalTimer(this);
        timer = new Timer();
        timer.schedule(captureTimer, CLEANUP_INTERVAL, CLEANUP_INTERVAL);
    }

    @Override
    protected void preStop()
    {
        // stop the session cleanup timer thread
        logger.debug("Destroying session cleanup timer task");
        timer.cancel();

        // shutdown any active sessions
        killAllSessions();

        // clear out the list of active users
        captureUserTable.purgeAllUsers();
        captureUserCookieTable.purgeAllUsers();
    }

    @Override
    protected void postStop()
    {
    }

    @Override
    protected void postInit()
    {
        CaptivePortalSettings readSettings = loadNodeSettings();

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
                saveNodeSettings(readSettings);
            }
            applyNodeSettings(readSettings);
        }
    }

    @Override
    protected void uninstall()
    {
        super.uninstall();

        // run a script to remove the directory for the custom captive page
        UvmContextFactory.context().execManager().exec(CAPTURE_CUSTOM_REMOVE_SCRIPT + " " + customPath);
    }

    protected Token[] generateResponse(CaptivePortalBlockDetails block, NodeTCPSession session)
    {
        return replacementGenerator.generateResponse(block, session);
    }

    // public methods for user control ----------------------------------------

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

            CaptivePortalUserEntry entry = captureUserTable.searchByUsername(username, ignoreCase);

            // when concurrent logins are disabled and we have an active entry for the user
            // we check the address and ignore the match if they are the same since it's
            // not really a concurrent login but a duplicate login from the same client
            if ( (entry != null) && (address.equals(entry.getUserAddress()) == false) ) {
                CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
                logEvent(event);
                incrementBlinger(BlingerType.AUTHFAIL, 1);
                logger.info("Authenticate duplicate " + username + " " + address);
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

                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
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
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.radiusAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Radius authentication failure", e);
                isAuthenticated = false;
            }
            break;
            
        case GOOGLE:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.googleAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Google authentication failure", e);
                isAuthenticated = false;
            }
            break;

        case FACEBOOK:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.facebookAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("Facebook authentication failure", e);
                isAuthenticated = false;
            }
            break;
            
        case ANY:
            try {
                DirectoryConnector directoryConnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-directory-connector");
                if (directoryConnector != null) isAuthenticated = directoryConnector.anyAuthenticate(username, password);
            } catch (Exception e) {
                logger.warn("ANY authentication failure", e);
                isAuthenticated = false;
            }
            break;
        default:
            logger.error("Unknown Authenticate Method: " + captureSettings.getAuthenticationType());
            
        }

        if (!isAuthenticated) {
            CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL, 1);
            logger.info("Authenticate failure " + username + " " + address);
            return (1);
        }

        captureUserTable.insertActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Authenticate success " + username + " " + address);
        return (0);
    }

    public int userActivate(InetAddress address, String username, String agree, boolean anonymous)
    {
        if (agree.equals("agree") == false) {
            CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.FAILED);
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL, 1);
            logger.info("Activate failure " + address);
            return (1);
        }

        captureUserTable.insertActiveUser(address, username, anonymous);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, captureSettings.getAuthenticationType(), CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Activate success " + address);

        if (captureSettings.getSessionCookiesEnabled()) {
            captureUserCookieTable.removeActiveUser(address);
        }

        return (0);
    }

    public int userActivate(InetAddress address, String agree)
    {
        return userActivate(address, "Anonymous", agree, true);
    }

    public int userLogin(InetAddress address, String username)
    {
        captureUserTable.insertActiveUser(address, username, false);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, address, username, CaptivePortalSettings.AuthenticationType.CUSTOM, CaptivePortalUserEvent.EventType.LOGIN);
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD, 1);
        logger.info("Login success " + address);
        return (0);
    }

    public int userLogout(InetAddress address)
    {
        return (userLogout(address, CaptivePortalUserEvent.EventType.USER_LOGOUT));
    }

    public int userAdminLogout(InetAddress address)
    {
        return (userLogout(address, CaptivePortalUserEvent.EventType.ADMIN_LOGOUT));
    }

    public int userLogout(InetAddress address, CaptivePortalUserEvent.EventType reason)
    {
        CaptivePortalUserEntry user = captureUserTable.searchByAddress(address);

        if (user == null) {
            logger.info("Logout failure: " + address);
            return (1);
        }

        // remove from the user table
        captureUserTable.removeActiveUser(address);

        // call the session cleanup function passing the address of the
        // user we just logged out to clean up any outstanding sessions
        validateAllSessions(address);

        CaptivePortalUserEvent event = new CaptivePortalUserEvent(policyId, user.getUserAddress(), user.getUserName(), captureSettings.getAuthenticationType(), reason);
        logEvent(event);
        logger.info("Logout success: " + address);

        if (captureSettings.getSessionCookiesEnabled() &&
            ((reason == CaptivePortalUserEvent.EventType.USER_LOGOUT) ||
             (reason == CaptivePortalUserEvent.EventType.ADMIN_LOGOUT))) {
            captureUserCookieTable.insertInactiveUser(user);
        }

        return (0);
    }

    // public method for testing all rules for a session ----------------------

    public boolean isClientAuthenticated(InetAddress clientAddr)
    {
        // search for the address in the active user table
        CaptivePortalUserEntry user = captureUserTable.searchByAddress(clientAddr);

        // if we have an authenticated user update activity and allow
        if (user != null) {
            user.updateActivityTimer();
            return (true);
        }

        return (false);
    }

    public PassedAddress isSessionAllowed(InetAddress clientAddr, InetAddress serverAddr)
    {
        List<PassedAddress> clientList = getCaptivePortalSettings().getPassedClients();
        List<PassedAddress> serverList = getCaptivePortalSettings().getPassedServers();
        PassedAddress checker = null;

        // see if the client is in the pass list
        for (int cc = 0; cc < clientList.size(); cc++) {
            checker = clientList.get(cc);
            if (checker.getLive() != true) continue;
            if (checker.getAddress().isMatch(clientAddr) != true) continue;
            logger.debug("Client " + clientAddr.getHostAddress().toString() + " found in pass list");
            return (checker);
        }

        // see if the server is in the pass list
        for (int ss = 0; ss < serverList.size(); ss++) {
            checker = serverList.get(ss);
            if (checker.getLive() != true) continue;
            if (checker.getAddress().isMatch(serverAddr) != true) continue;
            logger.debug("Server " + serverAddr.getHostAddress().toString() + " found in pass list");
            return (checker);
        }

        return (null);
    }

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

    public boolean isUserInCookieTable(InetAddress address, String username)
    {
        return captureUserCookieTable.searchByAddressUsername(address, username) != null;
    }

    public void removeUserFromCookieTable(InetAddress address)
    {
        captureUserCookieTable.removeActiveUser(address);
    }

    public CaptureRule checkCaptureRules(NodeTCPSession session)
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
     * This runs the periodic cleanup task now.
     * It is used by the test suite
     */
    public void runCleanup()
    {
        if (captureTimer != null)
            captureTimer.run();
    }
}
