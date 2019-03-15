/**
 * $Id: ApplicationControlApp.java 41228 2015-09-11 22:45:38Z dmorris $
 */

package com.untangle.app.application_control;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SettingsManager;
import com.untangle.uvm.SessionMatcher;
import com.untangle.uvm.app.License;
import com.untangle.uvm.app.AppMetric;
import com.untangle.uvm.vnet.Affinity;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.PipelineConnector;
import com.untangle.uvm.app.AppBase;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.logging.LogEvent;

/**
 * The Application Control application passes network traffic to the classd
 * daemon for categorization which is used to manage traffic based on rules and
 * application configuration.
 * 
 * @author mahotz
 * 
 */
public class ApplicationControlApp extends AppBase
{
    private final Logger logger = Logger.getLogger(getClass());

    protected static final String STAT_SCAN = "scan";
    protected static final String STAT_PASS = "pass";
    protected static final String STAT_FLAG = "flag";
    protected static final String STAT_BLOCK = "block";

    private final ApplicationControlEventHandler rawHandler = new ApplicationControlEventHandler(this, 0);
    private final ApplicationControlEventHandler webHandler = new ApplicationControlEventHandler(this, 443);
    private final ApplicationControlProtoList vineyard = new ApplicationControlProtoList(this);

    private final PipelineConnector rawConnector;
    private final PipelineConnector webConnector;
    private final PipelineConnector[] connectors;

    private final SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
    private final String appID = getAppSettings().getId().toString();
    private final int policyId = getAppSettings().getPolicyId().intValue();

    protected final InetSocketAddress daemonAddress = new InetSocketAddress("127.0.0.1", 8123);
    protected SocketChannel daemonSocket;
    protected SelectionKey readKey, writeKey;
    protected Selector readSelector, writeSelector;

    protected ApplicationControlStatistics statistics;
    protected ApplicationControlSettings settings;

    /*
     * appInstanceCount stores the number of this app type initialized thus far
     * appInstanceNum stores the number of this given app type This is done so
     * each app of this type has a unique sequential identifier
     */
    private static int appInstanceCount = 0;
    private final int appInstanceNum;

    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * 
     * @param appProperties
     *        The application properties
     */
    public ApplicationControlApp(com.untangle.uvm.app.AppSettings appSettings, com.untangle.uvm.app.AppProperties appProperties)
    {
        super(appSettings, appProperties);

        this.addMetric(new AppMetric(STAT_SCAN, I18nUtil.marktr("Session scanned")));
        this.addMetric(new AppMetric(STAT_PASS, I18nUtil.marktr("Sessions passed")));
        this.addMetric(new AppMetric(STAT_FLAG, I18nUtil.marktr("Sessions flagged")));
        this.addMetric(new AppMetric(STAT_BLOCK, I18nUtil.marktr("Sessions blocked")));

        this.rawConnector = UvmContextFactory.context().pipelineFoundry().create("application_control-raw", this, null, rawHandler, Fitting.OCTET_STREAM, Fitting.OCTET_STREAM, Affinity.SERVER, 5, true);
        this.webConnector = UvmContextFactory.context().pipelineFoundry().create("application_control-web", this, null, webHandler, Fitting.HTTP_STREAM, Fitting.HTTP_STREAM, Affinity.SERVER, 5, true);
        this.connectors = new PipelineConnector[] { rawConnector, webConnector };

        synchronized (getClass()) {
            this.appInstanceNum = appInstanceCount++;
        }
        ;

        statistics = new ApplicationControlStatistics();
    }

    /**
     * @return Cumulative traffic statistics
     */
    public ApplicationControlStatistics getStatistics()
    {
        return (statistics);
    }

    /**
     * Get the application settings
     * 
     * @return The application settings
     */
    public ApplicationControlSettings getSettings()
    {
        return (settings);
    }

    /**
     * Set the application settings
     * 
     * @param newSettings
     *        The new settings
     */
    public void setSettings(ApplicationControlSettings newSettings)
    {
        // Give each rule a unique ID before saving
        int idx = this.policyId * 100000;
        for (ApplicationControlLogicRule rule : newSettings.getLogicRules()) {
            rule.setId(++idx);
        }

        // Set flag for all "block" rules
        for (ApplicationControlProtoRule rule : newSettings.getProtoRules()) {
            if (rule.getBlock()) rule.setFlag(Boolean.TRUE);
        }

        newSettings.applyAppRules(statistics);

        try {
            settingsManager.save(System.getProperty("uvm.settings.dir") + "/application-control/settings_" + appID + ".js", newSettings);
        } catch (Exception e) {
            logger.error("setSettings()", e);
            return;
        }

        this.settings = newSettings;
        validateAllSessions();
    }

    /**
     * Function to log traffic status events to the debug log
     * 
     * @param evt
     *        The event to log
     * @param extraInfo
     *        Extra info to be logged
     */
    public void logStatusEvent(LogEvent evt, String extraInfo)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("LOG EVENT (" + extraInfo + ") = " + evt.toString());
        }
        logEvent(evt);
    }

    /**
     * Function to initialize application settings
     */
    @Override
    public void initializeSettings()
    {
        logger.info("Initializing default app settings...");

        this.settings = new ApplicationControlSettings();
        this.settings.setLogicRules(buildDefaultRules());
        this.settings.setProtoRules(vineyard.buildProtoList());
        this.settings.applyAppRules(statistics);

        this.setSettings(this.settings);
    }

    /**
     * Function to return our pipeline connectors
     * 
     * @return Our pipeline connectors
     */
    @Override
    protected PipelineConnector[] getConnectors()
    {
        return this.connectors;
    }

    /**
     * Called before the application is stopped.
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {
        // connect to the controller singleton
        UvmContextFactory.context().daemonManager().incrementUsageCount("untangle-classd");

        for (int counter = 0; daemonCheck() == false; counter++) {
            if (counter > 60) throw (new RuntimeException("Unable to start application-control app: missing daemon"));

            logger.info("Waiting for daemon connection... " + counter);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }

        // spin up the daemon socket
        socketStartup();
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
        // stop daemon
        UvmContextFactory.context().daemonManager().decrementUsageCount("untangle-classd");

        // close the daemon socket
        socketDestroy();
    }

    /**
     * Called after application initialization.
     */
    @Override
    protected void postInit()
    {
        ApplicationControlSettings readSettings = null;
        LinkedList<ApplicationControlProtoRule> workingList = null;
        String settingsFile = (System.getProperty("uvm.settings.dir") + "/application-control/settings_" + appID + ".js");

        try {
            // read our app settings from the file
            readSettings = settingsManager.load(ApplicationControlSettings.class, settingsFile);

            // if not found initialize with the defaults
            if (readSettings == null) {
                initializeSettings();
            }

            // apply the settings loaded from the file
            else {
                logger.info("Loaded settings from " + settingsFile);
                workingList = vineyard.mergeProtoList(readSettings.getProtoRules());
                if (workingList != null) {
                    readSettings.setProtoRules(workingList);
                    logger.info("Saving updated protocol list to " + settingsFile);
                    settingsManager.save(settingsFile, readSettings);
                }
                this.settings = readSettings;
                settings.applyAppRules(statistics);
            }
        }

        catch (Exception e) {
            logger.error("postInit()", e);
        }
    }

    /**
     * Called to setup the socket we use to communicate with the classd daemon.
     */
    protected void socketStartup()
    {
        // connect the search socket to the daemon
        try {
            daemonSocket = SocketChannel.open();
            readSelector = Selector.open();
            writeSelector = Selector.open();
            daemonSocket.socket().setTcpNoDelay(true);
            daemonSocket.configureBlocking(false);
            readKey = daemonSocket.register(readSelector, SelectionKey.OP_READ);
            writeKey = daemonSocket.register(writeSelector, SelectionKey.OP_WRITE);
            daemonSocket.connect(daemonAddress);
        }

        catch (Exception e) {
            logger.error("socketStartup()", e);
            socketDestroy();
        }
    }

    /**
     * Called to shut down the socket we use to communicate with the classd
     * daemon.
     */
    protected void socketDestroy()
    {
        try {
            if (readKey != null) readKey.cancel();
        } catch (Exception e) {
            logger.error("socketDestroy(readKey)", e);
        }
        readKey = null;

        try {
            if (writeKey != null) writeKey.cancel();
        } catch (Exception e) {
            logger.error("socketDestroy(writeKey)", e);
        }
        writeKey = null;

        try {
            if (readSelector != null) readSelector.close();
        } catch (Exception e) {
            logger.error("socketDestroy(readSelector)", e);
        }
        readSelector = null;

        try {
            if (writeSelector != null) writeSelector.close();
        } catch (Exception e) {
            logger.error("socketDestroy(writeSelector)", e);
        }
        writeSelector = null;

        try {
            if (daemonSocket != null) daemonSocket.close();
        } catch (Exception e) {
            logger.error("socketDestroy(daemonSocket)", e);
        }
        daemonSocket = null;
    }

    /**
     * Function to see if the classd daemon is running by attempting to connect
     * to the socket where it listens for connections
     * 
     * @return True if daemon is running, otherwise false
     */
    protected boolean daemonCheck()
    {
        Socket sock = null;
        boolean result = true;
        try {
            sock = new Socket();
            sock.connect(daemonAddress, 1000);

        } catch (Exception exn) {
            return (false);
        } finally {
            if(sock != null){
                try{
                    sock.close();
                }catch (Exception exn) {
                    result = false;
                }
            }
        }

        return result;
    }

    /**
     * Check for a valid license
     * 
     * @return True if license is valid, otherwise false
     */
    public boolean isLicenseValid()
    {
        if (UvmContextFactory.context().licenseManager().isLicenseValid(License.APPLICATION_CONTROL)) return true;
        return false;
    }

    /**
     * Create the default rules
     * 
     * @return The default rules
     */
    private LinkedList<ApplicationControlLogicRule> buildDefaultRules()
    {
        LinkedList<ApplicationControlLogicRule> logicRules = new LinkedList<>();

        int ruleNumber = 1;
        ApplicationControlLogicRule rule;
        LinkedList<ApplicationControlLogicRuleCondition> matchers;
        ApplicationControlLogicRuleCondition ruleMatcher1, ruleMatcher2, ruleMatcher3, ruleMatcher4;
        ApplicationControlLogicRuleAction action;

        rule = new ApplicationControlLogicRule();
        matchers = new LinkedList<>();
        ruleMatcher1 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.PROTOCOL, "TCP");
        ruleMatcher2 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.DST_PORT, "443");
        ruleMatcher3 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.APPLICATION_CONTROL_PROTOCHAIN, "*/YOUTUBE*");
        matchers.add(ruleMatcher1);
        matchers.add(ruleMatcher2);
        matchers.add(ruleMatcher3);
        action = new ApplicationControlLogicRuleAction(ApplicationControlLogicRuleAction.ActionType.BLOCK, Boolean.TRUE);
        rule.setConditions(matchers);
        rule.setAction(action);
        rule.setDescription("Block all HTTPS (encrypted) YouTube traffic.");
        rule.setId(ruleNumber++);
        logicRules.add(rule);

        rule = new ApplicationControlLogicRule();
        matchers = new LinkedList<>();
        ruleMatcher1 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.PROTOCOL, "TCP");
        ruleMatcher2 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.DST_PORT, "80");
        ruleMatcher3 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.APPLICATION_CONTROL_PROTOCHAIN, "*/HTTP*", true);
        matchers.add(ruleMatcher1);
        matchers.add(ruleMatcher2);
        matchers.add(ruleMatcher3);
        action = new ApplicationControlLogicRuleAction(ApplicationControlLogicRuleAction.ActionType.BLOCK, Boolean.TRUE);
        rule.setConditions(matchers);
        rule.setAction(action);
        rule.setDescription("Block all TCP port 80 traffic that is not HTTP.");
        rule.setId(ruleNumber++);
        logicRules.add(rule);

        rule = new ApplicationControlLogicRule();
        matchers = new LinkedList<>();
        ruleMatcher1 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.PROTOCOL, "TCP");
        ruleMatcher2 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.DST_PORT, "22");
        ruleMatcher3 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.APPLICATION_CONTROL_PROTOCHAIN, "*/SSH*", true);
        matchers.add(ruleMatcher1);
        matchers.add(ruleMatcher2);
        matchers.add(ruleMatcher3);
        action = new ApplicationControlLogicRuleAction(ApplicationControlLogicRuleAction.ActionType.BLOCK, Boolean.TRUE);
        rule.setConditions(matchers);
        rule.setAction(action);
        rule.setDescription("Block all TCP port 22 traffic that is not SSH.");
        rule.setId(ruleNumber++);
        logicRules.add(rule);

        rule = new ApplicationControlLogicRule();
        matchers = new LinkedList<>();
        ruleMatcher1 = new ApplicationControlLogicRuleCondition(ApplicationControlLogicRuleCondition.ConditionType.APPLICATION_CONTROL_CATEGORY, "Proxy");
        matchers.add(ruleMatcher1);
        action = new ApplicationControlLogicRuleAction(ApplicationControlLogicRuleAction.ActionType.TARPIT, Boolean.TRUE);
        rule.setConditions(matchers);
        rule.setAction(action);
        rule.setDescription("Tarpit all traffic classified as \"Proxy\" applications.");
        rule.setId(ruleNumber++);
        logicRules.add(rule);

        return (logicRules);
    }

    /**
     * Function to validate all sessions using the active settings
     */
    private void validateAllSessions()
    {
        this.killMatchingSessions(new SessionMatcher()
        {
            List<ApplicationControlProtoRule> protoList = settings.getProtoRules();

            /**
             * Look at every active session and apply the current protocol rules
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
            // look at every active session and apply the current proto rules
            public boolean isMatch(Integer policyId, short protocol, int clientIntf, int serverIntf, InetAddress clientAddr, InetAddress serverAddr, int clientPort, int serverPort, Map<String, Object> attachments)
            {
                // find the application and if missing leave the session alone
                String application = (String) attachments.get(AppSession.KEY_APPLICATION_CONTROL_APPLICATION);
                if (application == null) return (false);

                // see if there is a rule for the application and if not leave the session alone
                ApplicationControlProtoRule protoRule = settings.searchProtoRules(application);
                if (protoRule == null) return (false);

                // if the application is blocked return true to shut it down
                if (protoRule.getBlock() == true) {
                    logger.info("Killing active " + application + " session | CLIENT:" + clientAddr.getHostAddress() + " SERVER:" + serverAddr.getHostAddress());
                    return (true);
                }

                //  application is not blocked so leave the session alone
                return (false);
            }
        });
    }
}
