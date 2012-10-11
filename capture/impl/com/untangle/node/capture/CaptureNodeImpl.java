/**
 * $Id: CaptureNodeImpl.java,v 1.00 2011/12/12 13:31:21 mahotz Exp $
 */

package com.untangle.node.capture;

import java.util.LinkedList;
import java.util.List;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Timer;

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

    private final SoloPipeSpec trafficPipe = new SoloPipeSpec("capture-traffic", this, new CaptureTrafficHandler(this), Fitting.OCTET_STREAM, Affinity.SERVER, 0);
    private final SoloPipeSpec httpPipe = new SoloPipeSpec("capture-http", this, new TokenAdaptor(this, new CaptureHttpFactory(this)), Fitting.HTTP_TOKENS, Affinity.CLIENT, 0);
    private final PipeSpec[] pipeSpecs = new PipeSpec[] { trafficPipe, httpPipe };
    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final String settingsFile = (System.getProperty("uvm.settings.dir") + "/untangle-node-capture/settings_" + getNodeSettings().getId().toString());
    private final CaptureReplacementGenerator replacementGenerator;

    protected CaptureStatistics captureStatistics;
    protected CaptureSettings captureSettings;
    protected CaptureUserTable captureUserTable;
    protected Timer timer;

    private EventLogQuery allEventQuery;
    private EventLogQuery flaggedEventQuery;
    private EventLogQuery blockedEventQuery;
    private EventLogQuery ruleEventQuery;

    public CaptureNodeImpl( com.untangle.uvm.node.NodeSettings nodeSettings, com.untangle.uvm.node.NodeProperties nodeProperties )
    {
        super( nodeSettings, nodeProperties );

        replacementGenerator = new CaptureReplacementGenerator(getNodeSettings());
        captureStatistics = new CaptureStatistics();
        captureUserTable = new CaptureUserTable();

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

//        this.addMetric(new NodeMetric(STAT_SCAN, I18nUtil.marktr("Session scanned")));
//        this.addMetric(new NodeMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
//        this.addMetric(new NodeMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
//        this.addMetric(new NodeMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));
    }

    @Override
    public CaptureStatistics getStatistics()
    {
        return(captureStatistics);
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
    public EventLogQuery[] getEventQueries()
    {
        return new EventLogQuery[] { this.allEventQuery, this.flaggedEventQuery, this.blockedEventQuery };
    }

    @Override
    public EventLogQuery[] getRuleEventQueries()
    {
        return new EventLogQuery[] { this.ruleEventQuery };
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
        timer.schedule(new CaptureTimer(this),CLEANUP_INTERVAL,CLEANUP_INTERVAL);
    }

    @Override
    protected void preStop()
    {
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

    public boolean userAuthenticate(String address, String username, String password)
    {
        boolean isAuthenticated = false;

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
                    logger.warn( "Unable to authenticate users.", e );
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
                    logger.warn( "Unable to authenticate users.", e );
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
                    logger.warn( "Unable to authenticate users.", e );
                    isAuthenticated = false;
                }
                break;
            }

        if ( !isAuthenticated )
        {
            CaptureLoginEvent event = new CaptureLoginEvent( address, username, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.FAILED );
            logEvent(event);

            // TODO blinger
            logger.info("Login failure " + username + " " + address);
            return false;
        }

        CaptureLoginEvent event = new CaptureLoginEvent( address, username, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGIN );
        logEvent(event);

        captureUserTable.insertActiveUser(address,username,password);
        // TODO blinger
        logger.info("Authenticate success " + username + " " + address);
        return(true);
    }

    public boolean userActivate(String address, String agree)
    {
        logger.debug("ADDRESS:" + address + " AGREE:" + agree);
        
            if (agree.equals("agree") == false)
            {
                CaptureLoginEvent event = new CaptureLoginEvent( address, address, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.FAILED );
                logEvent(event);

                // TODO blinger
                logger.info("Activate failure " + address);
                return false;
            }

        CaptureLoginEvent event = new CaptureLoginEvent( address, address, captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGIN );
        logEvent(event);

        captureUserTable.insertActiveUser(address,address,address);
        // TODO blinger
        logger.info("Activate success " + address);
        return(true);
    }

    public boolean userLogout(String address)
    {
        CaptureUserEntry user = captureUserTable.searchForUser(address);

        if (user == null)
        {
            logger.info("User not found in table: " + address);
            return(false);
        }

        logger.info("User logout success: " + address);
        captureUserTable.removeActiveUser(address);

        CaptureLoginEvent event = new CaptureLoginEvent( address, "", captureSettings.getAuthenticationType(), CaptureLoginEvent.EventType.LOGOUT );
        logEvent(event);

        return true;
    }
}
