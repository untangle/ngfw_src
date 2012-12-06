/**
 * $Id: CaptureNodeImpl.java,v 1.00 2011/12/12 13:31:21 mahotz Exp $
 */

package com.untangle.node.capture;

import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.node.EventLogQuery;
import com.untangle.uvm.node.SessionTuple;

public class CaptureNodeImpl extends NodeBase implements CaptureNode
{
    private final int CLEANUP_INTERVAL = 60000;
    private final Logger logger = Logger.getLogger(getClass());
    private final Long policyId = getNodeSettings().getPolicyId();

    private final String CAPTURE_CUSTOM_CREATE_SCRIPT = System.getProperty("uvm.home") + "/bin/capture-custom-create";
    private final String CAPTURE_CUSTOM_REMOVE_SCRIPT = System.getProperty("uvm.home") + "/bin/capture-custom-remove";
    private final String CAPTURE_PERMISSIONS_SCRIPT = System.getProperty("uvm.home") + "/bin/capture-permissions";

    private static final String STAT_SESSALLOW = "sessallow";
    private static final String STAT_SESSBLOCK = "sessblock";
    private static final String STAT_SESSPROXY = "sessproxy";
    private static final String STAT_AUTHGOOD = "authgood";
    private static final String STAT_AUTHFAIL = "authfail";

    private final SoloPipeSpec trafficPipe = new SoloPipeSpec("capture-traffic", this, new CaptureTrafficHandler(this), Fitting.OCTET_STREAM, Affinity.SERVER, 0);
    private final SoloPipeSpec httpPipe = new SoloPipeSpec("capture-http", this, new TokenAdaptor(this, new CaptureHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { trafficPipe, httpPipe };
    private final CaptureReplacementGenerator replacementGenerator;

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final String settingsFile = (System.getProperty("uvm.settings.dir") + "/untangle-node-capture/settings_" + getNodeSettings().getId().toString());
    private final String customPath = (System.getProperty("uvm.web.dir") + "/capture/custom_" + getNodeSettings().getId().toString());
    private final Hashtable<InetAddress,PassedAddress> passedClientHash = new Hashtable<InetAddress,PassedAddress>();
    private final Hashtable<InetAddress,PassedAddress> passedServerHash = new Hashtable<InetAddress,PassedAddress>();

    protected CaptureUserTable captureUserTable = new CaptureUserTable();
    private CaptureSettings captureSettings;
    private CaptureTimer captureTimer;
    private Timer timer;

    private EventLogQuery userEventQuery;
    private EventLogQuery userSuccessQuery;
    private EventLogQuery userFailureQuery;
    private EventLogQuery userTimeoutQuery;
    private EventLogQuery userInactiveQuery;
    private EventLogQuery userLogoutQuery;
    private EventLogQuery adminLogoutQuery;

    private EventLogQuery allEventQuery;
    private EventLogQuery passEventQuery;
    private EventLogQuery captureEventQuery;

/**
 * nodeInstanceCount stores the number of this node type initialized thus far
 * nodeInstanceNum stores the number of this given node type
 * This is done so each node of this type has a unique sequential identifier
 */
    private static int nodeInstanceCount = 0;
    private final int nodeInstanceNum;

    public CaptureNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        synchronized(getClass()) { this.nodeInstanceNum = nodeInstanceCount++; };

        replacementGenerator = new CaptureReplacementGenerator(getNodeSettings());

        this.userEventQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "ORDER BY time_stamp DESC");

        this.userSuccessQuery = new EventLogQuery(I18nUtil.marktr("Login Success"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'LOGIN' " +
                "ORDER BY time_stamp DESC");

        this.userFailureQuery = new EventLogQuery(I18nUtil.marktr("Login Failure"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'FAILED' " +
                "ORDER BY time_stamp DESC");

        this.userTimeoutQuery = new EventLogQuery(I18nUtil.marktr("Session Timeout"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'TIMEOUT' " +
                "ORDER BY time_stamp DESC");

        this.userInactiveQuery = new EventLogQuery(I18nUtil.marktr("Idle Timeout"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'INACTIVE' " +
                "ORDER BY time_stamp DESC");

        this.userLogoutQuery = new EventLogQuery(I18nUtil.marktr("User Logout"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'USER_LOGOUT' " +
                "ORDER BY time_stamp DESC");

        this.adminLogoutQuery = new EventLogQuery(I18nUtil.marktr("Admin Logout"),
                "SELECT * FROM reports.n_capture_user_events " +
                "WHERE policy_id = :policyId " +
                "AND event_info = 'ADMIN_LOGOUT' " +
                "ORDER BY time_stamp DESC");

        this.captureEventQuery = new EventLogQuery(I18nUtil.marktr("Capture Events"),
                "SELECT * FROM reports.sessions " +
                "WHERE policy_id = :policyId " +
                "AND capture_blocked IS TRUE " +
                "ORDER BY time_stamp DESC");

        this.passEventQuery = new EventLogQuery(I18nUtil.marktr("Pass Events"),
                "SELECT * FROM reports.sessions " +
                "WHERE policy_id = :policyId " +
                "AND capture_blocked IS FALSE " +
                "ORDER BY time_stamp DESC");

        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Events"),
                "SELECT * FROM reports.sessions " +
                "WHERE policy_id = :policyId " +
                "AND capture_blocked IS NOT NULL " +
                "ORDER BY time_stamp DESC");

        addMetric(new NodeMetric(STAT_SESSALLOW, I18nUtil.marktr("Sessions allowed")));
        addMetric(new NodeMetric(STAT_SESSBLOCK, I18nUtil.marktr("Sessions blocked")));
        addMetric(new NodeMetric(STAT_SESSPROXY, I18nUtil.marktr("Sessions proxied")));
        addMetric(new NodeMetric(STAT_AUTHGOOD, I18nUtil.marktr("Login Success")));
        addMetric(new NodeMetric(STAT_AUTHFAIL, I18nUtil.marktr("Login Failure")));
    }

    @Override
    public CaptureSettings getSettings()
    {
        return(this.captureSettings);
    }

    @Override
    public void setSettings(CaptureSettings newSettings)
    {
        // first we commit the new settings to disk
        saveNodeSettings(newSettings);

        // next we call the function to activate the new settings
        applyNodeSettings(newSettings);

        // finally we validate all of the active sessions and cleanup
        // anything that is not allowed based on the new settings
        validateAllSessions();
    }

    @Override
    public ArrayList<CaptureUserEntry> getActiveUsers()
    {
        return(captureUserTable.buildUserList());
    }

    @Override
    public EventLogQuery[] getUserEventQueries()
    {
        return new EventLogQuery[] { this.userEventQuery, this.userSuccessQuery, this.userFailureQuery, this.userTimeoutQuery, this.userInactiveQuery, this.userLogoutQuery, this.adminLogoutQuery };
    }

    @Override
    public EventLogQuery[] getRuleEventQueries()
    {
        return new EventLogQuery[] { this.captureEventQuery, this.passEventQuery, this.allEventQuery };
    }

    public void incrementBlinger(BlingerType blingerType, long delta )
    {
        switch ( blingerType )
        {
        case SESSALLOW: adjustMetric(STAT_SESSALLOW, delta); break;
        case SESSBLOCK: adjustMetric(STAT_SESSBLOCK, delta); break;
        case SESSPROXY: adjustMetric(STAT_SESSPROXY, delta); break;
        case AUTHGOOD: adjustMetric(STAT_AUTHGOOD, delta); break;
        case AUTHFAIL: adjustMetric(STAT_AUTHFAIL, delta); break;
        }
    }

    @Override
    public void initializeSettings()
    {
        logger.info("Initializing default node settings");

        // create a new settings object
        CaptureSettings localSettings = new CaptureSettings();

        //  setup all the defaults
        BrandingManager brand = UvmContextFactory.context().brandingManager();

        localSettings.setBasicLoginPageTitle("Captive Portal");
        localSettings.setBasicLoginPageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        localSettings.setBasicLoginUsername("Username:");
        localSettings.setBasicLoginPassword("Password:");
        localSettings.setBasicLoginMessageText("If you want to wet your beak in the sea of interwebs, you need to login with a valid username and password.");
        localSettings.setBasicLoginFooter("If you have any questions, please contact your network administrator.");
        localSettings.setBasicMessagePageTitle("Captive Portal");
        localSettings.setBasicMessagePageWelcome("Welcome to the " + brand.getCompanyName() + " Captive Portal");
        localSettings.setBasicMessageMessageText("Click Continue to connect to the Internet.");
        localSettings.setBasicMessageAgreeBox(false);
        localSettings.setBasicMessageAgreeText("Clicking here means you agree to the terms above.");
        localSettings.setBasicMessageFooter("If you have any questions, please contact your network administrator.");

        // create a few example rules
        List<CaptureRule> ruleList = new LinkedList<CaptureRule>();
        LinkedList<CaptureRuleMatcher> matcherList = null;

        // example interface rule
        CaptureRuleMatcher interfaceMatch = new CaptureRuleMatcher(CaptureRuleMatcher.MatcherType.SRC_INTF, "2");
        matcherList = new LinkedList<CaptureRuleMatcher>();
        matcherList.add(interfaceMatch);
        ruleList.add(new CaptureRule(false, matcherList, true, "Capture all traffic on the internal interface"));

        localSettings.setCaptureRules(ruleList);

        // save the settings to disk
        saveNodeSettings(localSettings);

        // apply the new settings to the node
        applyNodeSettings(localSettings);
    }

    private CaptureSettings loadNodeSettings()
    {
        CaptureSettings readSettings = null;

        try
        {
            readSettings = settingsManager.load(CaptureSettings.class, settingsFile);
        }

        catch (Exception e)
        {
            logger.warn("Error loading node settings",e);
            return(null);
        }

        if (readSettings != null) logger.info("Loaded node settings from " + settingsFile);
        return(readSettings);
    }

    private void saveNodeSettings(CaptureSettings argSettings)
    {
        try
        {
            settingsManager.save(CaptureSettings.class, settingsFile, argSettings);
        }

        catch (Exception e)
        {
            logger.warn("Error in saveNodeSettings",e);
            return;
        }

        logger.info("Saved node settings to " + settingsFile);
    }

    private void applyNodeSettings(CaptureSettings argSettings)
    {
        // this function is called when settings are loaded or initialized
        // it gives us a single place to do stuff when applying a new
        // settings object to the node.

        // set a unique id for each capture rule
        int idx = (this.nodeInstanceNum * 1000);
        for (CaptureRule rule : argSettings.getCaptureRules()) rule.setId(++idx);

        List<PassedAddress> clientList = argSettings.getPassedClients();
        List<PassedAddress> serverList = argSettings.getPassedServers();
        PassedAddress local;

        passedClientHash.clear();
        passedServerHash.clear();

        // put all the passed clients into a hashtable
        for (int cc = 0; cc < clientList.size(); cc++)
        {
            local = clientList.get(cc);
            passedClientHash.put(local.getAddress(),local);
        }

        // put all of the passed servers into a hashtable
        for(int ss = 0; ss < serverList.size(); ss++)
        {
            local = serverList.get(ss);
            passedServerHash.put(local.getAddress(),local);
        }

        this.captureSettings = argSettings;
    }

    private void validateAllSessions()
    {
        // shut down any outstanding sessions that would not
        // be allowed based on the active node settings
        this.killMatchingSessions(new SessionMatcher()
        {
            List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            // for every session we have to check all the rules to make
            // sure we don't kill anything that shouldn't be captured
            public boolean isMatch( Long policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments )
            {
                // if session is for any active authenticated user return false
                if ( captureUserTable.searchByAddress(clientAddr ) != null) return(false);

                // if session matches any pass list return false
                if ( isSessionAllowed( clientAddr, serverAddr ) != null) return(false);

                // check the session against the rule list
                for (CaptureRule rule : ruleList)
                {
                    if ( rule.isMatch( protocol,
                                       clientIntf, serverIntf,
                                       clientAddr, serverAddr,
                                       clientPort, serverPort,
                                       (String)attachments.get(NodeSession.KEY_PLATFORM_USERNAME)))
                        {
                        // on a matching rule continue if capture is false
                        if (rule.getCapture() == false) continue;

                        // capture is true so log and kill the session
                        logger.debug("Validate killing " +
                                     clientAddr.getHostAddress().toString() + ":" + clientPort + " --> " +
                                     serverAddr.getHostAddress().toString() + ":" + serverPort );
                        return(true);
                        }
                }

                // no matches anywhere so leave the session alone
                return(false);
            }
        });

    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return(pipeSpecs);
    }

    @Override
    protected void preStart()
    {

        // run a script to add www-data to the uvmlogin group
        UvmContextFactory.context().execManager().execOutput( CAPTURE_PERMISSIONS_SCRIPT );

        // run a script to create the directory for the custom captive page
        UvmContextFactory.context().execManager().execOutput( CAPTURE_CUSTOM_CREATE_SCRIPT + " " + customPath );
    }

    @Override
    protected void postStart()
    {
        logger.debug("Creating session cleanup timer task");
        captureTimer = new CaptureTimer(this);
        timer = new Timer();
        timer.schedule(captureTimer,CLEANUP_INTERVAL,CLEANUP_INTERVAL);
    }

    @Override
    protected void preStop()
    {
        // stop the session cleanup timer thread
        logger.debug("Destroying session cleanup timer task");
        timer.cancel();
    }

    @Override
    protected void postStop()
    {
    }

    @Override
    protected void postInit()
    {
        CaptureSettings readSettings = loadNodeSettings();

            if (readSettings == null)
            {
                // we didn't get anything from the load function so we call
                // the initialize function which will take care of
                // creating, writing, and applying a new settings object
                initializeSettings();
            }

            else
            {
                // we got something back from the load so pass it
                // to the common apply function
                applyNodeSettings(readSettings);
            }
    }

    @Override protected void uninstall()
    {
        super.uninstall();

        // run a script to remove the directory for the custom captive page
        UvmContextFactory.context().execManager().execOutput( CAPTURE_CUSTOM_REMOVE_SCRIPT + " " + customPath );
    }

    protected Token[] generateResponse(CaptureBlockDetails block, NodeTCPSession session)
    {
        return replacementGenerator.generateResponse(block, session, false);
    }

///// ------------------------------------------------------------------------
///// public methods for user control

    public int userAuthenticate(InetAddress address, String username, String password)
    {
        boolean isAuthenticated = false;

        if (captureSettings.getConcurrentLoginsEnabled() == false)
        {
            CaptureUserEntry entry = captureUserTable.searchByUsername(username);

            if (entry != null)
            {
                CaptureUserEvent event = new CaptureUserEvent( policyId, address, username, captureSettings.getAuthenticationType(), CaptureUserEvent.EventType.FAILED );
                logEvent(event);
                incrementBlinger(BlingerType.AUTHFAIL,1);
                logger.info("Authenticate duplicate " + username + " " + address);
                return(2);
            }
        }

        switch( captureSettings.getAuthenticationType() )
        {
            case NONE:
                isAuthenticated = true;
                break;

            case ACTIVE_DIRECTORY:
                try
                {
                    DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                    if (adconnector != null) isAuthenticated = adconnector.activeDirectoryAuthenticate( username, password );
                }
                catch (Exception e)
                {
                    logger.warn("Active Directory failure", e);
                    isAuthenticated = false;
                }
                break;

            case LOCAL_DIRECTORY:
                try
                {
                    isAuthenticated = UvmContextFactory.context().localDirectory().authenticate( username, password );
                }
                catch (Exception e)
                {
                    logger.warn("Local Directory failure", e);
                    isAuthenticated = false;
                }
                break;

            case RADIUS:
                try
                {
                    DirectoryConnector adconnector = (DirectoryConnector)UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                    if (adconnector != null) isAuthenticated = adconnector.radiusAuthenticate( username, password );
                }
                catch (Exception e)
                {
                    logger.warn( "Radius Directory failure", e );
                    isAuthenticated = false;
                }
                break;
            }

        if ( !isAuthenticated )
        {
            CaptureUserEvent event = new CaptureUserEvent( policyId, address, username, captureSettings.getAuthenticationType(), CaptureUserEvent.EventType.FAILED );
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL,1);
            logger.info("Authenticate failure " + username + " " + address);
            return(1);
        }

        captureUserTable.insertActiveUser(address,username);

        CaptureUserEvent event = new CaptureUserEvent( policyId, address, username, captureSettings.getAuthenticationType(), CaptureUserEvent.EventType.LOGIN );
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD,1);
        logger.info("Authenticate success " + username + " " + address);
        return(0);
    }

    public int userActivate(InetAddress address, String agree)
    {
            if (agree.equals("agree") == false)
            {
                CaptureUserEvent event = new CaptureUserEvent( policyId, address, "Anonymous", captureSettings.getAuthenticationType(), CaptureUserEvent.EventType.FAILED );
                logEvent(event);
                incrementBlinger(BlingerType.AUTHFAIL,1);
                logger.info("Activate failure " + address);
                return(1);
            }

        captureUserTable.insertActiveUser(address,"Anonymous");

        CaptureUserEvent event = new CaptureUserEvent( policyId, address, "Anonymous", captureSettings.getAuthenticationType(), CaptureUserEvent.EventType.LOGIN );
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD,1);
        logger.info("Activate success " + address);
        return(0);
    }

    public int userLogout(InetAddress address)
    {
        return(userLogout(address,CaptureUserEvent.EventType.USER_LOGOUT));
    }

    public int userAdminLogout(InetAddress address)
    {
        return(userLogout(address,CaptureUserEvent.EventType.ADMIN_LOGOUT));
    }

    public int userLogout(InetAddress address,CaptureUserEvent.EventType reason)
    {
        CaptureUserEntry user = captureUserTable.searchByAddress(address);
        final InetAddress userAddress = address;

        if (user == null)
        {
            logger.info("Logout failure: " + address);
            return(1);
        }

        // remove from the user table
        captureUserTable.removeActiveUser(address);

        // shut down any outstanding sessions for the user
        this.killMatchingSessions(new SessionMatcher()
        {
            List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            // for every session we have to check all the rules to make
            // sure we don't kill anything that shouldn't be captured
            public boolean isMatch( Long policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String,Object> attachments )
            {
                // if session is not for this user return false
                if ( userAddress.equals( clientAddr) == false ) return(false);

                // if session matches any pass list return false
                if ( isSessionAllowed( clientAddr, serverAddr ) != null ) return(false);

                // check the session against the rule list
                for (CaptureRule rule : ruleList)
                {
                    if ( rule.isMatch( protocol,
                                       clientIntf, serverIntf,
                                       clientAddr, serverAddr,
                                       clientPort, serverPort,
                                       (String)attachments.get(NodeSession.KEY_PLATFORM_USERNAME)))
                        {
                        // on a matching rule continue if capture is false
                        if (rule.getCapture() == false) continue;

                        // capture is true so log and kill the session
                        logger.debug("Logout killing " +
                                     clientAddr.getHostAddress().toString() + ":" + clientPort + " --> " +
                                     serverAddr.getHostAddress().toString() + ":" + serverPort );
                        return(true);
                        }
                }

                // no matches anywhere so leave the session alone
                return(false);
            }
        });

        CaptureUserEvent event = new CaptureUserEvent( policyId, user.getUserAddress(), user.getUserName(), captureSettings.getAuthenticationType(), reason );
        logEvent(event);
        logger.info("Logout success: " + address);

        return(0);
    }

///// ------------------------------------------------------------------------
///// public method for testing all rules for a session

    public boolean isClientAuthenticated(InetAddress clientAddr)
    {
        // search for the address in the active user table
        CaptureUserEntry user = captureUserTable.searchByAddress(clientAddr);

            // if we have an authenticated user update activity and allow
            if (user != null)
            {
                user.updateActivityTimer();
                return(true);
            }

        return(false);
    }

    public PassedAddress isSessionAllowed(InetAddress clientAddr,InetAddress serverAddr)
    {
        PassedAddress checker = null;

        // see if the client is in the pass list
        checker = passedClientHash.get(clientAddr);
        if ((checker != null) && (checker.getLive() == true)) return(checker);

        // see if the server is in the pass list
        checker = passedServerHash.get(serverAddr);
        if ((checker != null) && (checker.getLive() == true)) return(checker);

        return(null);
    }

    public CaptureRule checkCaptureRules(IPNewSessionRequest sessreq)
    {
        List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            // check the session against the rule list
            for (CaptureRule rule : ruleList)
            {
                if (rule.isMatch(sessreq.getProtocol(),
                    sessreq.getClientIntf(), sessreq.getServerIntf(),
                    sessreq.getClientAddr(), sessreq.getServerAddr(),
                    sessreq.getClientPort(), sessreq.getServerPort(),
                    (String)sessreq.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME)))
                    {
                    return(rule);
                    }
            }

        return(null);
    }

    public CaptureRule checkCaptureRules(NodeTCPSession session)
    {
        List<CaptureRule> ruleList = captureSettings.getCaptureRules();

            // check the session against the rule list
            for (CaptureRule rule : ruleList)
            {
                if (rule.isMatch(session.getProtocol(),
                    session.getClientIntf(), session.getServerIntf(),
                    session.getClientAddr(), session.getServerAddr(),
                    session.getClientPort(), session.getServerPort(),
                    (String)session.globalAttachment(NodeSession.KEY_PLATFORM_USERNAME)))
                    {
                    return(rule);
                    }
            }

        return(null);
    }
}
