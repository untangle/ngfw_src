/**
 * $Id: EventManagerImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.event.AlertEvent;
import com.untangle.uvm.event.SyslogEvent;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.SyslogRule;
import com.untangle.uvm.event.TriggerRule;
import com.untangle.uvm.event.EventRuleCondition;
import com.untangle.uvm.event.EventRuleConditionField;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppSettings.AppState;
import com.untangle.uvm.app.Reporting;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.SyslogManagerImpl;

import com.untangle.uvm.AdminManager;
import com.untangle.uvm.AdminSettings;
import com.untangle.uvm.AdminUserSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

import org.json.JSONObject;

public class EventManagerImpl implements EventManager
{
    private static final Logger logger = Logger.getLogger(EventManagerImpl.class);

    private static EventManagerImpl instance = null;

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "events.js";
    private final String classesFilename = System.getProperty("uvm.lib.dir") + "/untangle-vm/events/" + "classFields.json";

    private EventWriter eventWriter = new EventWriter();

    private int currentSettingsVersion = 4;

    /**
     * The current event settings
     */
    private EventSettings settings;

    protected EventManagerImpl()
    {
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        EventSettings readSettings = null;

        try {
            readSettings = settingsManager.load( EventSettings.class, this.settingsFilename );
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

        SyslogManagerImpl.reconfigureCheck(settingsFilename, this.settings);
    }

    public void setSettings( final EventSettings newSettings )
    {
        /**
         * Set the Event Rules IDs
         */
        int idx = 0;
        for (AlertRule rule : newSettings.getAlertRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (SyslogRule rule : newSettings.getSyslogRules()) {
            rule.setRuleId(++idx);
        }
        idx = 0;
        for (TriggerRule rule : newSettings.getTriggerRules()) {
            rule.setRuleId(++idx);

            if ( rule.getTagName() == null )
                throw new RuntimeException("Missing tag name on trigger rule: " + idx);
            if ( rule.getTagTarget() == null )
                throw new RuntimeException("Missing tag target on trigger rule: " + idx);
            if ( rule.getTagLifetimeSec() == null )
                throw new RuntimeException("Missing tag lifetime on trigger rule: " + idx);
        }

        /**
         * Save the settings
         */
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        try {
            settingsManager.save( this.settingsFilename, newSettings );
        } catch (SettingsManager.SettingsException e) {
            logger.warn("Failed to save settings.",e);
            return;
        }

        /**
         * Change current settings
         */
        this.settings = newSettings;
        try {logger.debug("New Settings: \n" + new org.json.JSONObject(this.settings).toString(2));} catch (Exception e) {}

        SyslogManagerImpl.reconfigure(this.settings);
    }

    /**
     * Get the network settings
     */
    public EventSettings getSettings()
    {
        return this.settings;
    }

    public JSONObject getClassFields()
    {
        JSONObject classFields = null;
        File f = new File(classesFilename);
        if(f.exists()){
            try{
                InputStream is = new FileInputStream( classesFilename );
                String jsonTxt = IOUtils.toString(is);
                classFields = new JSONObject(jsonTxt);
            }catch(Exception e){
                logger.warn( "Unable to load event classes:", e);
            }
        }
        return classFields;
    }

    private EventSettings defaultSettings()
    {
        EventSettings settings = new EventSettings();
        settings.setVersion( 1 );
        settings.setAlertRules( defaultAlertRules() );
        settings.setSyslogRules( defaultSyslogRules() );
        settings.setTriggerRules( defaultTriggerRules() );

        return settings;
    }

    private LinkedList<AlertRule> defaultAlertRules()
    {
        LinkedList<AlertRule> rules = new LinkedList<AlertRule>();

        LinkedList<EventRuleCondition> matchers;
        EventRuleCondition matcher1;
        EventRuleCondition matcher2;
        EventRuleCondition matcher3;
        AlertRule eventRule;

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WanFailoverEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "action", "=", "DISCONNECTED" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "WAN is offline", false, 0 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "load1", ">", "20" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "Server load is high", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "diskFreePercent", "<", ".2" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "Free disk space is low", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "memFreePercent", "<", ".05" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Free memory is low", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "swapUsedPercent", ">", ".25" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "Swap usage is high", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "SServerPort", "=", "22" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "Suspicious Activity: Client created many SSH sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "SServerPort", "=", "3389" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "Suspicious Activity: Client created many RDP sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "entitled", "=", "false" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( true, matchers, true, true, "License limit exceeded. Session not entitled", true, 60*24 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "False" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Malware Distribution Point" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Malware Distribution Point website visit detected", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "True" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Malware Distribution Point" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Malware Distribution Point website visit blocked", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "False" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Botnet" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Botnet website visit detected", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "True" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Botnet" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Botnet website visit blocked", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "False" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Phishing/Fraud" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Phishing/Fraud website visit detected", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "blocked", "=", "True" );
        matchers.add( matcher2 );
        matcher3 = new EventRuleCondition( "category", "=", "Phishing/Fraud" );
        matchers.add( matcher3 );
        eventRule = new AlertRule( true, matchers, true, true, "Phishing/Fraud website visit blocked", false, 10 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*DeviceTableEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "key", "=", "add" );
        matchers.add( matcher2 );
        if ( "i386".equals(System.getProperty("os.arch", "unknown")) || "amd64".equals(System.getProperty("os.arch", "unknown"))) {
            eventRule = new AlertRule( false, matchers, true, true, "New device discovered", false, 0 );
        } else {
            eventRule = new AlertRule( true, matchers, true, true, "New device discovered", false, 0 );
        }
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*QuotaEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "action", "=", "2" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Host exceeded quota.", false, 0 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*PenaltyBoxEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "action", "=", "1" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Host put in penalty box", false, 0 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "protochain", "=", "*BITTORRE*" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Host is using Bittorrent", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*HttpResponseEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "contentLength", ">", "1000000000" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Host is doing large download", true, 60 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*CaptureUserEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "event", "=", "FAILED" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "Failed Captive Portal login", false, 0 );
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*VirusHttpEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "clean", "=", "False" );
        matchers.add( matcher2 );
        eventRule = new AlertRule( false, matchers, true, true, "HTTP virus blocked", false, 0 );
        rules.add( eventRule );

        return rules;
    }

    private LinkedList<SyslogRule> defaultSyslogRules()
    {
        LinkedList<SyslogRule> rules = new LinkedList<SyslogRule>();

        LinkedList<EventRuleCondition> matchers;
        EventRuleCondition matcher1;
        EventRuleCondition matcher2;
        EventRuleCondition matcher3;
        SyslogRule eventRule;

        matchers = new LinkedList<EventRuleCondition>();
        eventRule = new SyslogRule( true, matchers, true, true, "All events", false, 0 );
        rules.add( eventRule );

        return rules;
    }

    private LinkedList<TriggerRule> defaultTriggerRules()
    {
        LinkedList<TriggerRule> rules = new LinkedList<TriggerRule>();

        LinkedList<EventRuleCondition> matchers;
        EventRuleCondition matcher1;
        EventRuleCondition matcher2;
        EventRuleCondition matcher3;
        TriggerRule eventRule;

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*");
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "category", "=", "Proxy" );
        matchers.add( matcher2 );
        eventRule = new TriggerRule( false, matchers, true, "Tag proxy-using hosts", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "localAddr" );
        eventRule.setTagName( "proxy-use" );
        eventRule.setTagLifetimeSec( new Long(60*30) ); // 30 minutes
        rules.add( eventRule );

        matchers = new LinkedList<EventRuleCondition>();
        matcher1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*" );
        matchers.add( matcher1 );
        matcher2 = new EventRuleCondition( "application", "=", "BITTORRE" );
        matchers.add( matcher2 );
        eventRule = new TriggerRule( false, matchers, true, "Tag bittorrent-using hosts", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "localAddr" );
        eventRule.setTagName( "bittorrent-use" );
        eventRule.setTagLifetimeSec( new Long(60*5) ); // 5 minutes
        rules.add( eventRule );

        return rules;
    }

    public void logEvent( LogEvent event )
    {
        eventWriter.inputQueue.offer(event);
    }

    private void runEvent( LogEvent event )
    {
        try {
            runAlertRules( event );
        } catch ( Exception e ) {
            logger.warn("Failed to evaluate alert rules.", e);
        }

        try {
            runTriggerRules( event );
        } catch ( Exception e ) {
            logger.warn("Failed to evaluate trigger rules.", e);
        }

        try {
            runSyslogRules( event );
        } catch ( Exception e ) {
            logger.warn("Failed to evaluate syslog rules.", e);
        }
    }

    private void runAlertRules( LogEvent event )
    {
        if ( event == null )
            return;
        if ( event instanceof AlertEvent )
            return;

        List<AlertRule> rules = UvmContextFactory.context().eventManager().getSettings().getAlertRules();
        if ( rules == null )
            return;

        JSONObject jsonObject = event.toJSONObject();
        for ( AlertRule rule : rules ) {
            if ( ! rule.getEnabled() )
                continue;
            if ( ! rule.isMatch( jsonObject ) )
                continue;

            logger.debug( "alert match: " + rule.getDescription() + " matches " + jsonObject.toString() );

            if(rule.getEmail()){
                sendEmailForEvent( rule, event );
            }
            if(rule.getLog()){
                AlertEvent eventEvent = new AlertEvent( rule.getDescription(), event.toSummaryString(), jsonObject, event, rule, false );
                UvmContextFactory.context().logEvent( eventEvent );
            }
        }
    }

    private void runTriggerRules( LogEvent event )
    {
        if ( event == null )
            return;
        //if ( event instanceof TriggerEvent )
        //    return;

        List<TriggerRule> rules = UvmContextFactory.context().eventManager().getSettings().getTriggerRules();
        if ( rules == null )
            return;

        JSONObject jsonObject = event.toJSONObject();

        for ( TriggerRule rule : rules ) {
            if ( ! rule.getEnabled() )
                continue;
            if ( ! rule.isMatch( jsonObject ) )
                continue;

            logger.debug( "trigger \"" + rule.getDescription() + "\" matches: " + event );

            String target = findAttribute( jsonObject, rule.getTagTarget(), 3 );
            if ( target == null ) {
                logger.debug( "trigger: failed to find target \"" + rule.getTagTarget() + "\"");
                continue;
            }

            target = target.replaceAll("/",""); // remove annoying / from InetAddress toString()

            HostTableEntry host = null;
            UserTableEntry user = null;
            DeviceTableEntry device = null;
            List<Tag> tags;

            host = UvmContextFactory.context().hostTable().getHostTableEntry( target );
            if ( rule.getAction().toString().contains("USER") ) {
                user = UvmContextFactory.context().userTable().getUserTableEntry( target );
                if ( user == null && host != null )
                    user = UvmContextFactory.context().userTable().getUserTableEntry( host.getUsername() );
            }
            if ( rule.getAction().toString().contains("DEVICE") ) {
                device = UvmContextFactory.context().deviceTable().getDevice( target );
                if ( device == null && host != null )
                    device = UvmContextFactory.context().deviceTable().getDevice( host.getMacAddress() );
            }

            if ( rule.getAction().toString().contains("_HOST") && host == null ) {
                logger.debug( "trigger: failed to find host \"" + target + "\"");
                continue;
            }
            if ( rule.getAction().toString().contains("_USER") && user == null ) {
                logger.debug( "trigger: failed to find user \"" + target + "\"");
                continue;
            }
            if ( rule.getAction().toString().contains("_DEVICE") && device == null ) {
                logger.debug( "trigger: failed to find device \"" + target + "\"");
                continue;
            }

            switch( rule.getAction() ) {
            case TAG_HOST:
                logger.debug("Tagging host " + target + " with tag \"" + rule.getTagName() + "\"");
                host.addTag( new Tag( rule.getTagName(), rule.getTagLifetimeSec()*1000 ) );
                break;
            case UNTAG_HOST:
                logger.debug("Untagging host " + target + " with tag \"" + rule.getTagName() + "\"");
                tags = host.getTags();
                if ( tags == null ) break;
                for ( Tag t : tags ) {
                    if ( rule.nameMatches( t ) ) {
                        logger.debug("Untagging host " + target + " removing tag \"" + t.getName() + "\"");
                        host.removeTag( t );
                    }
                }
                break;
            case TAG_USER:
                logger.debug("Tagging user " + target + " with tag \"" + rule.getTagName() + "\"" );
                user.addTag( new Tag( rule.getTagName(), rule.getTagLifetimeSec()*1000 ) );
                break;
            case UNTAG_USER:
                logger.debug("Untagging user " + target + " with tag \"" + rule.getTagName() + "\"");
                tags = user.getTags();
                if ( tags == null ) break;
                for ( Tag t : tags ) {
                    if ( rule.nameMatches( t ) ) {
                        logger.debug("Untagging user " + target + " removing tag \"" + t.getName() + "\"");
                        user.removeTag( t );
                    }
                }
                break;
            case TAG_DEVICE:
                logger.debug("Tagging device " + target + " with tag \"" + rule.getTagName() + "\"" );
                device.addTag( new Tag( rule.getTagName(), rule.getTagLifetimeSec()*1000 ) );
                break;
            case UNTAG_DEVICE:
                logger.debug("Untagging device " + target + " with tag \"" + rule.getTagName() + "\"");
                tags = device.getTags();
                if ( tags == null ) break;
                for ( Tag t : tags ) {
                    if ( rule.nameMatches( t ) ) {
                        logger.debug("Untagging device " + target + " removing tag \"" + t.getName() + "\"");
                        device.removeTag( t );
                    }
                }
                break;
            }
        }
    }

    private void runSyslogRules( LogEvent event )
    {
        if ( event == null )
            return;
        if ( event instanceof SyslogEvent )
            return;
        if ( ! settings.getSyslogEnabled() )
            return;

        List<SyslogRule> rules = UvmContextFactory.context().eventManager().getSettings().getSyslogRules();
        if ( rules == null )
            return;

        JSONObject jsonObject = event.toJSONObject();
        for ( SyslogRule rule : rules ) {
            if ( ! rule.getEnabled() )
                continue;
            if ( ! rule.isMatch( jsonObject ) ) 
                continue;

            logger.debug( "syslog match: " + rule.getDescription() + " matches " + jsonObject.toString() );

            event.setTag(SyslogManagerImpl.LOG_TAG_PREFIX);
            if(rule.getLog()){
                SyslogEvent eventEvent = new SyslogEvent( rule.getDescription(), event.toSummaryString(), jsonObject, event, rule, false );
                UvmContextFactory.context().logEvent( eventEvent );
            }
            if ( rule.getSyslog() ){
                try {
                    SyslogManagerImpl.sendSyslog( event );
                } catch (Exception exn) {
                    logger.warn("failed to send syslog", exn);
                }
            }
        }
    }

    private static String findAttribute( JSONObject json, String name, int maxDepth )
    {
        if ( json == null || name == null ) return null;
        if ( maxDepth < 1 ) return null;

        //logger.info("findAttribute( " + name + " , " + json + ")");
        try {
            String[] keys = JSONObject.getNames(json);
            if ( keys == null ) return null;

            for( String key : keys ) {
                if ("class".equals(key))
                    continue;
                if (name.equalsIgnoreCase(key)) {
                    Object o = json.get(key);
                    return (o == null ? null : o.toString());
                }
            }

            for( String key : keys ) {
                try {
                    if ("class".equals(key))
                        continue;
                    Object o = json.get(key);
                    if ( o == null )
                        continue;
                    if ( ! (o instanceof java.io.Serializable) )
                        continue;
                    JSONObject obj = new JSONObject(o);
                    if ( obj.length() < 2 )
                        continue;

                    if ( o != null ) {
                        String s = findAttribute(obj,name,maxDepth-1);
                        if ( s != null )
                            return s;
                    }
                } catch (Exception e) {}
            }
        } catch (Exception e) {
            logger.warn("Exception",e);
        }

        return null;
    }

    private static boolean sendEmailForEvent( AlertRule rule, LogEvent event )
    {
        if(rule.frequencyCheck() == false){
            return false;
        }

        String companyName = UvmContextFactory.context().brandingManager().getCompanyName();
        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String fullName = hostName + (  domainName == null ? "" : ("."+domainName));
        String serverName = companyName + " " + I18nUtil.marktr("Server");
        JSONObject jsonObject = event.toJSONObject();
        String jsonString;

        cleanupJsonObject( jsonObject );

        try {
            jsonString = jsonObject.toString(4);
        } catch (org.json.JSONException e) {
            logger.warn("Failed to pretty print.",e);
            jsonString = jsonObject.toString();
        }

        String subject = serverName + " " +
            I18nUtil.marktr("Event!") +
            " [" + fullName + "] ";

        String messageBody = I18nUtil.marktr("The following event occurred on the") + " " + serverName + " @ " + event.getTimeStamp() +
            "\r\n\r\n" +
            rule.getDescription() + ":" + "\r\n" +
            event.toSummaryString() +
            "\r\n\r\n" +
            I18nUtil.marktr("Causal Event:") + " " + event.getClass().getSimpleName() + 
            "\r\n" +
            jsonString +
            "\r\n\r\n" +
            I18nUtil.marktr("This is an automated message sent because the event matched the configured Event Rules.");

        LinkedList<String> alertRecipients = new LinkedList<String>();

        /*
         * Local admin users
         */
        LinkedList<AdminUserSettings> adminManagerUsers = UvmContextFactory.context().adminManager().getSettings().getUsers();
        if ( adminManagerUsers != null ) {
            for ( AdminUserSettings user : adminManagerUsers ) {
                if ( user.getEmailAddress() == null || "".equals( user.getEmailAddress() ) ){
                    continue;
                }
                if ( ! user.getEmailAlerts() ){
                    continue;
                }
                alertRecipients.add( user.getEmailAddress() );
            }
        }

        /*
         * Report users
         */
        App reportsApp = UvmContextFactory.context().appManager().app("reports");
        List<String> reportsEmailAddresses = ((Reporting) reportsApp).getAlertEmailAddresses();
        alertRecipients.addAll(reportsEmailAddresses);

        for( String emailAddress : alertRecipients){
            logger.warn("emailAddress=" + emailAddress);
            try {
                String[] recipients = null;
                recipients = new String[]{ emailAddress };
                UvmContextFactory.context().mailSender().sendMessage( recipients, subject, messageBody);
            } catch ( Exception e) {
                logger.warn("Failed to send mail.",e);
            }
        }

        return true;
    }

    private static void cleanupJsonObject( JSONObject jsonObject )
    {
        if ( jsonObject == null )
            return;

        @SuppressWarnings("unchecked")
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


    /**
     * This thread waits on the l
     * It also explicitly releases hosts from the penalty box and quotas after expiration
     */
    private class EventWriter implements Runnable
    {

        private volatile Thread thread;
        private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<LogEvent>();

        public void run()
        {
            thread = Thread.currentThread();
            LogEvent event = null;

            /**
             * Loop indefinitely and continue running event rules
             */
            while (thread != null) {
                synchronized( this ) {
                    try {
                        if (inputQueue.size() > 10000) {
                            logger.warn("Large input queue size: " + inputQueue.size());
                        }

                        event = inputQueue.take();

                        runEvent( event );

                        UvmContextFactory.context().hookManager().callCallbacks( HookManager.REPORTS_EVENT_LOGGED, event );

                    } catch (Exception e) {
                        logger.warn("Failed to run event rules.", e);
                        try {Thread.sleep(1000);} catch (Exception exc) {}
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
            Thread tmp = thread;
            thread = null; /* thread will exit if thread is null */
            if (tmp != null) {
                tmp.interrupt();
            }
        }
    }

}
