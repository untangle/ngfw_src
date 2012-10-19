/**
 * $Id: CaptureNodeImpl.java,v 1.00 2011/12/12 13:31:21 mahotz Exp $
 */

package com.untangle.node.capture;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Timer;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenAdaptor;
import com.untangle.node.http.ReplacementGenerator;
import com.untangle.uvm.node.DirectoryConnector;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.PortRange;
import com.untangle.uvm.node.NodeMetric;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipeSpec;
import com.untangle.uvm.vnet.SoloPipeSpec;
import com.untangle.uvm.vnet.Subscription;
import com.untangle.uvm.vnet.NodeBase;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.BrandingManager;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.node.License;
import com.untangle.uvm.node.EventLogQuery;

public class CaptureNodeImpl extends NodeBase implements CaptureNode
{
    private final int CLEANUP_INTERVAL = 60000;
    private final Logger logger = Logger.getLogger(getClass());

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
    private final Hashtable<String,PassedAddress> passedClientHash = new Hashtable<String,PassedAddress>();
    private final Hashtable<String,PassedAddress> passedServerHash = new Hashtable<String,PassedAddress>();

    protected CaptureUserTable captureUserTable = new CaptureUserTable();
    private CaptureSettings captureSettings;
    private CaptureTimer captureTimer = new CaptureTimer(this);
    private Timer timer;

    private EventLogQuery loginEventQuery;
    private EventLogQuery blockEventQuery;
    private EventLogQuery allEventQuery;
    private EventLogQuery flaggedEventQuery;
    private EventLogQuery blockedEventQuery;
    private EventLogQuery ruleEventQuery;

    public CaptureNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        replacementGenerator = new CaptureReplacementGenerator(getNodeSettings());

        this.loginEventQuery = new EventLogQuery(I18nUtil.marktr("Login Events"),
            "SELECT * FROM reports.n_capture_login_events evt ORDER BY time_stamp DESC");

        this.blockEventQuery = new EventLogQuery(I18nUtil.marktr("Block Events"),
            "SELECT * FROM reports.n_capture_block_events evt ORDER BY time_stamp DESC");

        this.allEventQuery = new EventLogQuery(I18nUtil.marktr("All Sessions"),
            "SELECT * FROM reports.sessions " +
            "WHERE policy_id = :policyId " +
            "AND classd_application IS NOT NULL " +
            "ORDER BY time_stamp DESC");

        this.flaggedEventQuery = new EventLogQuery(I18nUtil.marktr("Flagged Sessions"),
            "SELECT * FROM reports.sessions " +
            "WHERE policy_id = :policyId " +
            "AND classd_flagged IS TRUE " +
            "ORDER BY time_stamp DESC");

        this.blockedEventQuery = new EventLogQuery(I18nUtil.marktr("Blocked Sessions"),
            "SELECT * FROM reports.sessions " +
            "WHERE policy_id = :policyId " +
            "AND classd_blocked IS TRUE " +
            "ORDER BY time_stamp DESC");

        this.ruleEventQuery = new EventLogQuery(I18nUtil.marktr("Matched Rules"),
            "SELECT * FROM reports.sessions " +
             "WHERE policy_id = :policyId " +
             "AND classd_ruleid IS NOT NULL " +
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

        // now we call the shared apply function
        applyNodeSettings(newSettings);
    }

    @Override
    public ArrayList<CaptureUserEntry> getActiveUsers()
    {
        return(captureUserTable.buildUserList());
    }

    @Override
    public EventLogQuery[] getLoginEventQueries()
    {
        return new EventLogQuery[] { this.loginEventQuery };
    }

    @Override
    public EventLogQuery[] getBlockEventQueries()
    {
        return new EventLogQuery[] { this.blockEventQuery };
    }

    @Override
    public EventLogQuery[] getRuleEventQueries()
    {
        return new EventLogQuery[] { this.ruleEventQuery };
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

        // the set function takes care of writing the settings to
        // disk and applying the settings to the node
        setSettings(localSettings);
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

        this.captureSettings = argSettings;
        List<PassedAddress> clientList = captureSettings.getPassedClients();
        List<PassedAddress> serverList = captureSettings.getPassedServers();
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
    }

    @Override
    protected PipeSpec[] getPipeSpecs()
    {
        return(pipeSpecs);
    }

    @Override
    protected void preStart()
    {
    }

    @Override
    protected void postStart()
    {
        timer = new Timer();
        timer.schedule(captureTimer,CLEANUP_INTERVAL,CLEANUP_INTERVAL);
    }

    @Override
    protected void preStop()
    {
        // stop the session cleanup timer thread
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

    protected Token[] generateResponse(CaptureBlockDetails block, NodeTCPSession session)
    {
        return replacementGenerator.generateResponse(block, session, false);
    }

///// ------------------------------------------------------------------------
///// public methods for user control

    public int userAuthenticate(String address, String username, String password)
    {
        boolean isAuthenticated = false;

        if (captureSettings.getConcurrentLoginsEnabled() == false)
        {
            CaptureUserEntry entry = captureUserTable.searchByUsername(username);

            if (entry != null)
            {
                CaptureLoginEvent event = new CaptureLoginEvent( address, username, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.FAILED );
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
            CaptureLoginEvent event = new CaptureLoginEvent( address, username, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.FAILED );
            logEvent(event);
            incrementBlinger(BlingerType.AUTHFAIL,1);
            logger.info("Authenticate failure " + username + " " + address);
            return(1);
        }

        captureUserTable.insertActiveUser(address,username);

        CaptureLoginEvent event = new CaptureLoginEvent( address, username, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGIN );
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD,1);
        logger.info("Authenticate success " + username + " " + address);
        return(0);
    }

    public int userActivate(String address, String agree)
    {
            if (agree.equals("agree") == false)
            {
                CaptureLoginEvent event = new CaptureLoginEvent( address, address, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.FAILED );
                logEvent(event);
                incrementBlinger(BlingerType.AUTHFAIL,1);
                logger.info("Activate failure " + address);
                return(1);
            }

        captureUserTable.insertActiveUser(address,address);

        CaptureLoginEvent event = new CaptureLoginEvent( address, address, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGIN );
        logEvent(event);
        incrementBlinger(BlingerType.AUTHGOOD,1);
        logger.info("Activate success " + address);
        return(0);
    }

    public int userLogout(String address)
    {
        CaptureUserEntry user = captureUserTable.searchByAddress(address);

        if (user == null)
        {
            logger.info("Logout failure: " + address);
            return(1);
        }

        captureUserTable.removeActiveUser(address);

        CaptureLoginEvent event = new CaptureLoginEvent( user.getUserAddress(), user.getUserName(), captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGOUT );
        logEvent(event);
        logger.info("Logout success: " + address);

        return(0);
    }

///// ------------------------------------------------------------------------
///// public method for testing all rules for a session

    public boolean isSessionAllowed(String clientAddr,String serverAddr)
    {
        // first we see if the client is authenticated since if they
        // are we are done and the traffic is allowed
        CaptureUserEntry user = captureUserTable.searchByAddress(clientAddr);

            // if we have an authenticated user update activity and allow
            if (user != null)
            {
                user.updateActivityTimer();
                return(true);
            }

        // see if the client is in the pass list
        if (passedClientHash.get(clientAddr) != null) return(true);

        // see if the server is in the pass list
        if (passedServerHash.get(serverAddr) != null) return(true);

        return(false);
    }

    public CaptureRule checkCaptureRules(NodeSession session)
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
                    logger.debug("MATCH: " + rule.getDescription() + " BLOCK:" + rule.getBlock());
                    return(rule);
                    }
            }

        return(null);
    }
}
