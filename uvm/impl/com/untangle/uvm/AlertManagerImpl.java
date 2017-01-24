/**
 * $Id: ReportsManagerImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.alert.AlertEvent;
import com.untangle.uvm.alert.AlertSettings;
import com.untangle.uvm.alert.AlertRule;
import com.untangle.uvm.alert.AlertRuleCondition;
import com.untangle.uvm.alert.AlertRuleConditionField;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;


import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;


import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import org.json.JSONObject;

public class AlertManagerImpl implements AlertManager
{

    private static final Logger logger = Logger.getLogger(AlertManagerImpl.class);

    private static AlertManagerImpl instance = null;

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "alert.js";

    private AlertEventWriter eventWriter = new AlertEventWriter();

    private int currentSettingsVersion = 4;

    /**
     * The current alert settings
     */
    private AlertSettings settings;

    protected AlertManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        AlertSettings readSettings = null;

        try {
            readSettings = settingsManager.load( AlertSettings.class, this.settingsFilename );
        } catch ( SettingsManager.SettingsException e ) {
            logger.warn( "Failed to load settings:", e );
        }

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn( "No settings found - Initializing new settings." );
            this.setSettings( defaultSettings() );
        }
        else {
            this.settings = readSettings;

            logger.debug( "Loading Settings: " + this.settings.toJSONString() );
        }

        eventWriter.start();
    }

    public void setSettings( final AlertSettings newSettings )
    {
        /**
         * Set the Alert Rules IDs
         */
        int idx = 0;
        for (AlertRule rule : newSettings.getAlertRules()) {
            rule.setRuleId(++idx);
        }

        AlertSettings convertedSettings = null;
        if(newSettings.getVersion() < this.currentSettingsVersion){
            logger.warn("AlertManagerImpl: do conversion");
            convertedSettings = convertSettings(newSettings);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            if(convertedSettings != null){
                settingsManager.save( this.settingsFilename, convertedSettings );
            }else{
                settingsManager.save( this.settingsFilename, newSettings );
            }
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

    }

    private AlertSettings convertSettings(AlertSettings settings)
    {
        /**
         * 12.0 conversion
         */
        if ( settings.getVersion() == null ) {
            logger.warn("Running v12.0 conversion...");
            settings = conversion_12_0(settings);
        }
        /**
         * 12.1.1 conversion
         */
        if ( settings.getVersion() == 2 ) {
            logger.warn("Running v12.1.1 conversion...");
            settings = conversion_12_1_1(settings);
        }

        return settings;
    }

    /**
     * Get the network settings
     */
    public AlertSettings getSettings()
    {
        return this.settings;
    }

    private AlertSettings defaultSettings()
    {
        AlertSettings settings = new AlertSettings();
        settings.setVersion( 1 );
        settings.setAlertRules( defaultAlertRules() );

        return settings;
    }

    private LinkedList<AlertRule> defaultAlertRules()
    {
        LinkedList<AlertRule> rules = new LinkedList<AlertRule>();

        LinkedList<AlertRuleCondition> matchers;
        AlertRuleCondition matcher1;
        AlertRuleCondition matcher2;
        AlertRuleCondition matcher3;
        AlertRule alertRule;

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WanFailoverEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "action", "=", "DISCONNECTED" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "WAN is offline", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "load1", ">", "20" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Server load is high", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "diskFreePercent", "<", ".2" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Free disk space is low", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "memFreePercent", "<", ".05" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Free memory is low", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SystemStatEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "swapUsedPercent", ">", ".25" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Swap usage is high", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SessionEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "SServerPort", "=", "22" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Suspicious Activity: Client created many SSH sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SessionEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "SServerPort", "=", "3389" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "Suspicious Activity: Client created many RDP sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SessionEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "entitled", "=", "false" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "License limit exceeded. Session not entitled", true, 60*24 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "False" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Malware Distribution Point" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Malware Distribution Point website visit detected", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "True" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Malware Distribution Point" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Malware Distribution Point website visit blocked", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "False" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Botnet" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Botnet website visit detected", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "True" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Botnet" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Botnet website visit blocked", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "False" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Phishing/Fraud" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Phishing/Fraud website visit detected", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*WebFilterEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "blocked", "=", "True" ) );
        matchers.add( matcher2 );
        matcher3 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "category", "=", "Phishing/Fraud" ) );
        matchers.add( matcher3 );
        alertRule = new AlertRule( true, matchers, true, true, "Phishing/Fraud website visit blocked", false, 10 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*DeviceTableEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "key", "=", "add" ) );
        matchers.add( matcher2 );
        if ( "i386".equals(System.getProperty("os.arch", "unknown")) || "amd64".equals(System.getProperty("os.arch", "unknown"))) {
            alertRule = new AlertRule( false, matchers, true, true, "New device discovered", false, 0 );
        } else {
            alertRule = new AlertRule( true, matchers, true, true, "New device discovered", false, 0 );
        }
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*QuotaEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "action", "=", "2" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host exceeded quota.", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*PenaltyBoxEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "action", "=", "1" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host put in penalty box", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*ApplicationControlLogEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "protochain", "=", "*BITTORRE*" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host is using Bittorrent", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*HttpResponseEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "contentLength", ">", "1000000000" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Host is doing large download", true, 60 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*CaptureUserEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "event", "=", "FAILED" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "Failed Captive Portal login", false, 0 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*VirusHttpEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "clean", "=", "False" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "HTTP virus blocked", false, 0 );
        rules.add( alertRule );

        return rules;
    }

    private AlertSettings conversion_12_0(AlertSettings settings)
    {
        settings.setVersion( 1 );

        LinkedList<AlertRuleCondition> matchers;
        AlertRuleCondition matcher1;
        AlertRuleCondition matcher2;
        AlertRule alertRule;

        LinkedList<AlertRule> rules = settings.getAlertRules();

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SessionEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "entitled", "=", "false" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( true, matchers, true, true, "License exceeded. Session not entitled", true, 60*24 );
        rules.add( alertRule );

        matchers = new LinkedList<AlertRuleCondition>();
        matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*DeviceTableEvent*" ) );
        matchers.add( matcher1 );
        matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "key", "=", "add" ) );
        matchers.add( matcher2 );
        alertRule = new AlertRule( false, matchers, true, true, "New device discovered", false, 0 );
        rules.add( alertRule );

        return settings;
    }

    private AlertSettings conversion_12_1_1(AlertSettings settings)
    {
        settings.setVersion( 3 );

        try {
            boolean found = false;

            for (Iterator<AlertRule> it = settings.getAlertRules().iterator(); it.hasNext() ;) {
                AlertRule rule = it.next();
                if ("Free Memory is low".equals( rule.getDescription() ) ) {
                    logger.info("Replacing Free Memory alert rule...");
                    it.remove();
                    found = true;
                    break;
                }
            }

            if ( found ) {
                LinkedList<AlertRuleCondition> matchers;
                AlertRuleCondition matcher1;
                AlertRuleCondition matcher2;
                AlertRule alertRule;

                matchers = new LinkedList<AlertRuleCondition>();
                matcher1 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "class", "=", "*SystemStatEvent*" ) );
                matchers.add( matcher1 );
                matcher2 = new AlertRuleCondition( AlertRuleCondition.ConditionType.FIELD_CONDITION, new AlertRuleConditionField( "memFreePercent", "<", ".05" ) );
                matchers.add( matcher2 );
                alertRule = new AlertRule( false, matchers, true, true, "Free memory is low", true, 60 );

                LinkedList<AlertRule> rules = settings.getAlertRules();
                rules.add( 3, alertRule );
            }
        } catch (Exception e) {
            logger.warn("Conversion Exception",e);
        }

        return settings;
    }

    public void logEvent( LogEvent event )
    {
        eventWriter.inputQueue.offer(event);
    }

    private static void runAlertEventQueue( LinkedList<LogEvent> events )
    {
        for ( LogEvent event : events ) {
            runAlertEvent( event );
        }
    }

    private static void runAlertEvent( LogEvent event )
    {
        try {
            JSONObject jsonObject = event.toJSONObject();
            for ( AlertRule rule : UvmContextFactory.context().alertManager().getSettings().getAlertRules() ) {
                if ( ! rule.getEnabled() )
                {
                    continue;
                }

                if ( rule.isMatch( jsonObject ) ) {
                    logger.info( "alert match: " + rule.getDescription() + " matches " + jsonObject.toString() );

                    boolean alertSent = false;
                    AlertEvent alertEvent = new AlertEvent( rule.getDescription(), event.toSummaryString(), jsonObject, event, rule, false );
                    if ( rule.getAlert() ){
                        alertSent = sendAlertForEvent( rule, event );
                    }
                    if ( rule.getLog() ){
                        UvmContextFactory.context().logEvent( alertEvent );
                    }
                }
            }
        } catch ( Exception e ) {
            logger.warn("Failed to evaluate alert rules.", e);
        }
    }

    private static boolean sendAlertForEvent( AlertRule rule, LogEvent event )
    {
        if ( rule.getAlertLimitFrequency() && rule.getAlertLimitFrequencyMinutes() > 0 ) {
            long currentTime = System.currentTimeMillis();
            long lastAlertTime = rule.lastAlertTime();
            long secondsSinceLastAlert = ( currentTime - lastAlertTime ) / 1000;
            // if not enough time has elapsed, just return
            if ( secondsSinceLastAlert < ( rule.getAlertLimitFrequencyMinutes() * 60 ) )
                return false;
        }

        rule.updateAlertTime();

        String companyName = UvmContextFactory.context().brandingManager().getCompanyName();
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String fullName = hostName + (  domainName == null ? "" : ("."+domainName));
        String serverName = companyName + " " + I18nUtil.marktr("Server");
        JSONObject jsonObject = event.toJSONObject();
        String jsonEvent;

        cleanupJsonObject( jsonObject );

        try {
            jsonEvent = jsonObject.toString(4);
        } catch (org.json.JSONException e) {
            logger.warn("Failed to pretty print.",e);
            jsonEvent = jsonObject.toString();
        }

        LinkedList<AdminUserSettings> adminManagerUsers = UvmContextFactory.context().adminManager().getSettings().getUsers();

        String subject = serverName + " " +
            I18nUtil.marktr("Alert!") +
            " [" + fullName + "] ";

        String messageBody = I18nUtil.marktr("The following event occurred on the") + " " + serverName + " @ " + event.getTimeStamp() +
            "\r\n\r\n" +
            rule.getDescription() + ":" + "\r\n" +
            event.toSummaryString() +
            "\r\n\r\n" +
            I18nUtil.marktr("Causal Event:") + " " + event.getClass().getSimpleName() + 
            "\r\n" +
            jsonEvent + 
            "\r\n\r\n" +
            I18nUtil.marktr("This is an automated message sent because the event matched the configured Alert Rules.");

        if ( adminManagerUsers != null ) {
            for ( AdminUserSettings user : adminManagerUsers ) {
                if ( user.getEmailAddress() == null || "".equals( user.getEmailAddress() ) ){
                    continue;
                }
                if ( ! user.getEmailAlerts() ){
                    continue;
                }
                try {
                    String[] recipients = null;
                    recipients = new String[]{ user.getEmailAddress() };
                    UvmContextFactory.context().mailSender().sendMessage( recipients, subject, messageBody);
                } catch ( Exception e) {
                    logger.warn("Failed to send mail.",e);
                }
            }
        }

        return true;
    }

    /**
     * This thread periodically walks through the entries and removes expired entries
     * It also explicitly releases hosts from the penalty box and quotas after expiration
     */

    /**
      * The amount of time for the event write to sleep
       * if there is not a lot of work to be done
    */
    private static int SYNC_TIME = 30*1000; /* 30 seconds */

    /**
     * If the event queue length reaches the high water mark
     * Then the eventWriter is not able to keep up with demand
     * In this case the overloadedFlag is set to true
     */
    private static int HIGH_WATER_MARK = 1000000;

    /**
     * If overloadedFlag is set to true and the queue shrinks to this size
     * then overloadedFlag will be set to false
     */
    private static int LOW_WATER_MARK = 100000;

    private static boolean forceFlush = false;

    private class AlertEventWriter implements Runnable
    {

        private volatile Thread thread;

        /**
         * Maximum number of events to write per work cycle
         */
        private int maxEventsPerCycle = 20000; 

        /**
         * If true then the eventWriter is considered "overloaded" and can not keep up with demand
         * This is set if the event queue length reaches the high water mark
         * In this case we stop logging events entirely until we are no longer overloaded
         */
        private boolean overloadedFlag = false;
    
        /**
         * This stores the maximum queue delay for the last batch
         * That is difference between now() and the oldest event in the batch
         * This approximates the delay its taking for events to be written to the database
         * If the event writer falls behind this value can get large.
         * Typical values less than a minute. A value of one hour would mean its behind and writing events slower than they are being created
         * and that it is currently taking one hour before new events are written to the database
         */
        private long writeDelaySec = 0;

        /**
         * This is a queue of incoming events
         */
        private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

        public void run()
        {
            thread = Thread.currentThread();

            LinkedList<LogEvent> logQueue = new LinkedList<LogEvent>();
            LogEvent event = null;

            /**
             * Loop indefinitely and continue logging events
             */
            while (thread != null) {
                /**
                 * Sleep until next log time
                 * If force flush was called, don't sleep
                 * If there is already a full runs worth of events, don't sleep
                 * If events are significantly delayed (more than 2x SYNC_TIME), don't sleep
                 */
                if ( forceFlush ||
                     (inputQueue.size() > maxEventsPerCycle) ||
                    (writeDelaySec*1000 >  SYNC_TIME*2) ) {
                    logger.debug("persist(): skipping sleep");
                    // minor sleep to let other threads that my want to synchronize on this run
                    try {Thread.sleep(100);} catch (Exception e) {}
                } else {
                    try {Thread.sleep(SYNC_TIME);} catch (Exception e) {}
                }

                synchronized( this ) {
                    try {
                        /**
                         * Copy all events out of the queue
                        */
                        while ((event = inputQueue.poll()) != null && logQueue.size() < maxEventsPerCycle) {
                            if ( event instanceof AlertEvent )
                            {
                                /* Ignore our own events (they're destined for logging) */
                                continue;
                            }
                            logQueue.add(event);
                        }

                        /**
                         * Check queue lengths
                         */
                        if (!this.overloadedFlag && inputQueue.size() > HIGH_WATER_MARK)  {
                            logger.warn("OVERLOAD: High Water Mark reached.");
                            this.overloadedFlag = true;
                        }
                        if (this.overloadedFlag && inputQueue.size() < LOW_WATER_MARK) {
                            logger.warn("OVERLOAD: Low Water Mark reached. Continuing normal operation.");
                            this.overloadedFlag = false;
                        }

                        /**
                         * Run alert rules
                         */
                        runAlertEventQueue( logQueue );

                        logQueue.clear();
                    
                        try {Thread.sleep(1000);} catch (Exception e) {}

                    } catch (Exception e) {
                        logger.warn("Failed to write alert events.", e);
                    } finally {
                        /**
                         * If the forceFlush flag was set, reset it and wake any interested parties
                         */
                        if (forceFlush) {
                            forceFlush = false; //reset global flag
                            notifyAll();  /* notify any waiting threads that the flush is done */ 
                        }
                    }
                }
            }
        }

        protected void start()
        {
            UvmContextFactory.context().newThread(this).start();
        }

        protected void stop()
        {
            // this is disabled because it causes boxes to hang on stopping the uvm
            // forceFlush(); /* flush last few events */

            Thread tmp = thread;
            thread = null; /* thread will exit if thread is null */
            if (tmp != null) {
                tmp.interrupt();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void cleanupJsonObject( JSONObject jsonObject )
    {
        if ( jsonObject == null )
            return;

        java.util.Iterator<String> keys = (java.util.Iterator<String>)jsonObject.keys();
        while ( keys.hasNext() ) {
            String key = keys.next();

            if ("class".equals(key)) {
                keys.remove();
                continue;
            }
            if ("tag".equals(key)) {
                keys.remove();
                continue;
            }
            if ("partitionTablePostfix".equals(key)) {
                keys.remove();
                continue;
            }

            /**
             * Recursively clean json objects
             */
            try {
                JSONObject subObject = jsonObject.getJSONObject(key);
                if (subObject != null) {
                    cleanupJsonObject( subObject );
                }
            } catch (Exception e) {
                /* ignore */
            }

            /**
             * If the object implements JSONString, then its probably a jsonObject
             * Convert to JSON Object, recursively clean that, then replace it
             */
            try {
                if ( jsonObject.get(key) != null ) {
                    Object o = jsonObject.get(key);
                    if ( o instanceof org.json.JSONString ) {
                        JSONObject newObj = new JSONObject( o );
                        cleanupJsonObject( newObj );
                        jsonObject.put( key, newObj );
                    }
                }
            } catch (Exception e) {
                /* ignore */
            }
        }
    }
}
