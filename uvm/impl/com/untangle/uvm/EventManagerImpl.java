/**
 * $Id: EventManagerImpl.java,v 1.00 2015/03/04 13:59:12 dmorris Exp $
 */
package com.untangle.uvm;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.event.AlertEvent;
import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.event.AlertRule;
import com.untangle.uvm.event.EventRule;
import com.untangle.uvm.event.SyslogRule;
import com.untangle.uvm.event.TriggerRule;
import com.untangle.uvm.event.EventRuleCondition;
import com.untangle.uvm.app.App;
import com.untangle.uvm.app.Reporting;
import com.untangle.uvm.app.IPMatcher;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.SyslogManagerImpl;
import com.untangle.uvm.util.GlobUtil;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.util.StringUtil;

import com.untangle.uvm.AdminUserSettings;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import java.lang.Class;
import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONString;

/**
 * Event prcessor
 */
public class EventManagerImpl implements EventManager
{
    private static final Integer SETTINGS_CURRENT_VERSION = 3;

    private static final Logger logger = Logger.getLogger(EventManagerImpl.class);

    private static EventManagerImpl instance = null;

    private final String settingsFilename = System.getProperty("uvm.settings.dir") + "/untangle-vm/" + "events.js";
    private final String classesFilename = System.getProperty("uvm.lib.dir") + "/untangle-vm/events/" + "classFields.json";

    private LocalEventWriter localEventWriter = new LocalEventWriter();
    private RemoteEventWriter remoteEventWriter = new RemoteEventWriter();

    private Map<String, String> i18nMap;

    private static String DefaultEmailSubject = "";
    private static String DefaultEmailBody = "";
    private static Map<String,EventTemplateVariable> eventTemplateVariables;

    private static final String PREVIEW_EVENT_CLASS_NAME = "SystemStatEvent";
    private LogEvent mostRecentPreviewEvent = null;

    /**
     * The current event settings
     */
    private EventSettings settings;

    private Pattern classesToProcess = Pattern.compile("");

    /**
     * Initialize event manager.
     *
     * * Load settings.
     * * Start event writer.
     * 
     * @return Instance of event manager.
     */
    protected EventManagerImpl()
    {
        i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        SettingsManager settingsManager = UvmContextFactory.context().settingsManager();
        EventSettings readSettings = null;

        try {
            readSettings = settingsManager.load( EventSettings.class, this.settingsFilename );
        } catch ( SettingsManager.SettingsException e ) {
            logger.warn( "Failed to load settings:", e );
        }

        buildEmailTemplateDefaults();
        buildEmailTemplateVariables();

        /**
         * If there are still no settings, just initialize
         */
        if (readSettings == null) {
            logger.warn( "No settings found - Initializing new settings." );
            this.setSettings( defaultSettings() );
        } else {
            updateSettings(readSettings);

            this.settings = readSettings;

            logger.debug( "Loading Settings: " + this.settings.toJSONString() );
        }
        buildClassesToProcess();

        localEventWriter.start();
        remoteEventWriter.start();

        SyslogManagerImpl.reconfigureCheck(settingsFilename, this.settings);
    }

    /**
     * Update settings.
     * @param newSettings EventSettings to replace current settings.
     */
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
        buildClassesToProcess();
    }

    /**
     * Get the network settings
     * @return EventSettings of current settings.
     */
    public EventSettings getSettings()
    {
        return this.settings;
    }

    /**
     * Extract class field from rule and add to list if not already found.
     *
     * @param rule    EventRule to process.
     * @param classes List of classes to add to and check.
     */
    private void buildClassesToProcessRules(EventRule rule, List<String> classes){
        if(!rule.getEnabled()){
            return;
        }
        String fieldValue = null;
        for(EventRuleCondition condition: rule.getConditions()){
            if(condition.getField().equals("class") || condition.getField().equals("javaClass")){
                fieldValue = condition.getFieldValue();
                if(!classes.contains(fieldValue)){
                    classes.add(fieldValue);
                }
            }
        }
    }

    /**
     * Build regex list of classes to match to add to local queue.
     */
    private void buildClassesToProcess(){
        LinkedList<String> classes = new LinkedList<>();

        for(EventRule rule : Stream.of(settings.getAlertRules(), settings.getSyslogRules(), settings.getTriggerRules())
                            .flatMap(Collection::stream)
                            .collect(Collectors.toList()) ){
            buildClassesToProcessRules(rule, classes);
        }

        synchronized (classesToProcess) {
            classesToProcess = Pattern.compile(GlobUtil.globToRegex(String.join("|", classes) ));
        }
    }

    /**
     * Retreive class fields for UI.
     * @return JSONObject of class fields.
     */
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

    /**
     * Return template parameters
     *
     * @return     List of JSON objects for template parameters
     */
    public JSONArray getTemplateParameters(){
        JSONArray result = null;
        int index = 0;

        result = new JSONArray();
        try{
            SortedSet<String> sortedKeys = new TreeSet<>(eventTemplateVariables.keySet());

            for(String variable : sortedKeys){
                JSONObject jo = new JSONObject(EventManagerImpl.eventTemplateVariables.get(variable));
                jo.remove("class");
                result.put(index++, jo);
            }
        }catch(Exception e){
            logger.warn("getTemplateParameters:", e);
        }

        return result;
    }

    /**
     * Generate a preview of the email format alert with passed
     * templates and conversion.  Also convert whitepace characters for UI display.
     * @param  rule            AlertRule to process.
     * @param  event           LogEvent to process.
     * @param  subjectTemplate Email subject template.
     * @param  bodyTemplate    Email body template
     * @param  convert         If true, pass convert to human readable.  Otherwise, don't.
     * @return                 Map containing emailSubject and emailBody.
     */
    public Map<String, String> emailAlertFormatPreview(AlertRule rule, LogEvent event, String subjectTemplate, String bodyTemplate, boolean convert){
        if(mostRecentPreviewEvent != null){
            event = mostRecentPreviewEvent;
        }
        Map<String,String> email = emailAlertFormat(rule, event, subjectTemplate, bodyTemplate, convert);
        for(String key : email.keySet() ){
            email.put(key, email.get(key).replaceAll("\n", "<br>"));
            email.put(key, email.get(key).replaceAll("\n", "<br>"));
        }

        return email;
    }

    /**
     * Generate email subject, body from rule, event, and templates.
     * @param  rule            AlertRule to process.
     * @param  event           LogEvent to process.
     * @param  subjectTemplate Email subject template.
     * @param  bodyTemplate    Email body template
     * @param  convert         If true, pass convert to human readable.  Otherwise, don't.
     * @return                 Map containing emailSubject and emailBody.
     */
    public static Map<String, String> emailAlertFormat(AlertRule rule, LogEvent event, String subjectTemplate, String bodyTemplate, boolean convert)
    {
        String subject = subjectTemplate == null ? DefaultEmailSubject : subjectTemplate;
        String body = bodyTemplate == null ? DefaultEmailBody : bodyTemplate;

        for(EventTemplateVariable variable : EventManagerImpl.eventTemplateVariables.values() ){
            if(variable.getNameMatcher().matcher(subject).find()){
                subject = subject.replaceAll(variable.getName(), variable.getValue(rule, event, convert));
            }
            if(variable.getNameMatcher().matcher(body).find()){
                body = body.replaceAll(variable.getName(), variable.getValue(rule, event, convert));
            }
        }

        Map<String, String> email = new HashMap<>();
        email.put( "emailSubject", subject);
        email.put( "emailBody", body );

        return email;
    }


    /**
     * Update passed settings to latest verson.
     *
     * @param  settings Current EventSettings
     * @return          Nothing
     */
    private void updateSettings(EventSettings settings){
        if(settings.getVersion() < SETTINGS_CURRENT_VERSION){

            boolean webfilterEvent = false;
            boolean sessionEvent = false;
            for(EventRule er : Stream.of(settings.getAlertRules(), settings.getSyslogRules(), settings.getTriggerRules())
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList()) ){

                webfilterEvent = false;
                sessionEvent = false;
                for(EventRuleCondition c : er.getConditions()){
                    if(c.getField().equals("class") && c.getFieldValue().equals("*WebFilterEvent*")){
                        webfilterEvent = true;
                    }
                    if(c.getField().equals("class") && c.getFieldValue().equals("*SessionEvent*")){
                        sessionEvent = true;
                    }
                    if(webfilterEvent){
                        if(c.getField().equals("category") && c.getFieldValue().equals("Malware Distribution Point")){
                            c.setFieldValue("Malware Sites");
                            er.setDescription(er.getDescription().replaceAll("Malware Distribution Point", "Malware Sites"));
                        }
                        if(c.getField().equals("category") && c.getFieldValue().equals("Botnet")){
                            c.setFieldValue("Bot Nets");
                            er.setDescription(er.getDescription().replaceAll("Botnet", "Bot Nets"));
                        }
                        if(c.getField().equals("category") && c.getFieldValue().equals("Phishing/Fraud")){
                            c.setFieldValue("Phishing and Other Frauds");
                            er.setDescription(er.getDescription().replaceAll("Phishing/Fraud", "Phishing and Other Frauds"));
                        }
                    }
                    if(sessionEvent){
                        if(c.getField().equals("sServerPort")){
                            c.setField("SServerPort");
                        }
                    }
                }

            }

            settings.setVersion(SETTINGS_CURRENT_VERSION);
            this.setSettings( settings );
        }

    }

    /**
     * Create default settings.
     * @return EventSettings consisting of default values.
     */
    private EventSettings defaultSettings()
    {
        EventSettings settings = new EventSettings();
        settings.setVersion( SETTINGS_CURRENT_VERSION );
        settings.setAlertRules( defaultAlertRules() );
        settings.setSyslogRules( defaultSyslogRules() );
        settings.setTriggerRules( defaultTriggerRules() );

        return settings;
    }

    /**
     * Return default alert rules.
     * @return List of AlertRule consisting of default alert rules.
     */
    private LinkedList<AlertRule> defaultAlertRules()
    {
        LinkedList<AlertRule> rules = new LinkedList<>();

        LinkedList<EventRuleCondition> conditions;
        EventRuleCondition condition1;
        EventRuleCondition condition2;
        EventRuleCondition condition3;
        AlertRule eventRule;

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WanFailoverEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "action", "=", "DISCONNECTED" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "WAN is offline", false, 0 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "load1", ">", "20" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "Server load is high", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "diskFreePercent", "<", ".2" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "Free disk space is low", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "memFreePercent", "<", ".05" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "Free memory is low", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SystemStatEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "swapUsedPercent", ">", ".25" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "Swap usage is high", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "SServerPort", "=", "22" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "Suspicious Activity: Client created many SSH sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "SServerPort", "=", "3389" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "Suspicious Activity: Client created many RDP sessions", true, 60, Boolean.TRUE, 20.0D, 60, "CClientAddr");
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*SessionEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "entitled", "=", "false" );
        conditions.add( condition2 );
        eventRule = new AlertRule( true, conditions, true, true, "License limit exceeded. Session not entitled", true, 60*24 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "False" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Malware Sites" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Malware Sites website visit detected", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "True" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Malware Sites" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Malware Sites website visit blocked", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "False" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Bot Netst" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Bot Nets website visit detected", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "True" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Bot Nets" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Bot Nets website visit blocked", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "False" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Phishing and Other Frauds" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Phishing and Other Frauds website visit detected", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*WebFilterEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "blocked", "=", "True" );
        conditions.add( condition2 );
        condition3 = new EventRuleCondition( "category", "=", "Phishing and Other Frauds" );
        conditions.add( condition3 );
        eventRule = new AlertRule( true, conditions, true, true, "Phishing and Other Frauds website visit blocked", false, 10 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*DeviceTableEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "key", "=", "add" );
        conditions.add( condition2 );
        if ( "i386".equals(System.getProperty("os.arch", "unknown")) || "amd64".equals(System.getProperty("os.arch", "unknown"))) {
            eventRule = new AlertRule( false, conditions, true, true, "New device discovered", false, 0 );
        } else {
            eventRule = new AlertRule( true, conditions, true, true, "New device discovered", false, 0 );
        }
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*QuotaEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "action", "=", "2" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "Host exceeded quota.", false, 0 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "protochain", "=", "*BITTORRE*" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "Host is using Bittorrent", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*HttpResponseEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "contentLength", ">", "1000000000" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "Host is doing large download", true, 60 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*CaptivePortalUserEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "event", "=", "FAILED" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "Failed Captive Portal login", false, 0 );
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*VirusHttpEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "clean", "=", "False" );
        conditions.add( condition2 );
        eventRule = new AlertRule( false, conditions, true, true, "HTTP virus blocked", false, 0 );
        rules.add( eventRule );

        return rules;
    }

    /**
     * Return default suslog rules.
     * @return List of SyslogRule consisting of default syslog rules.
     */
    private LinkedList<SyslogRule> defaultSyslogRules()
    {
        LinkedList<SyslogRule> rules = new LinkedList<>();

        LinkedList<EventRuleCondition> conditions;
        EventRuleCondition condition1;
        EventRuleCondition condition2;
        EventRuleCondition condition3;
        SyslogRule eventRule;

        conditions = new LinkedList<>();
        eventRule = new SyslogRule( true, conditions, true, true, "All events", false, 0 );
        rules.add( eventRule );

        return rules;
    }

    /**
     * Return default trigger rules.
     * @return List of TriggerRule consisting of default trigger rules.
     */
    private LinkedList<TriggerRule> defaultTriggerRules()
    {
        LinkedList<TriggerRule> rules = new LinkedList<>();

        LinkedList<EventRuleCondition> conditions;
        EventRuleCondition condition1;
        EventRuleCondition condition2;
        EventRuleCondition condition3;
        TriggerRule eventRule;

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*AlertEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "description", "=", "*Suspicious Activity*" );
        conditions.add( condition2 );
        eventRule = new TriggerRule( true, conditions, true, "Tag suspicious activity", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "cClientAddr" );
        eventRule.setTagName( "suspicious" );
        eventRule.setTagLifetimeSec( new Long(60*30) ); // 30 minutes
        rules.add( eventRule );
        
        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*");
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "category", "=", "Proxy" );
        conditions.add( condition2 );
        eventRule = new TriggerRule( false, conditions, true, "Tag proxy-using hosts", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "sessionEvent.localAddr" );
        eventRule.setTagName( "proxy-use" );
        eventRule.setTagLifetimeSec( new Long(60*30) ); // 30 minutes
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "application", "=", "BITTORRE" );
        conditions.add( condition2 );
        eventRule = new TriggerRule( false, conditions, true, "Tag bittorrent-using hosts", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "sessionEvent.CClientAddr" );
        eventRule.setTagName( "bittorrent-usage" );
        eventRule.setTagLifetimeSec( new Long(60*5) ); // 5 minutes
        rules.add( eventRule );

        conditions = new LinkedList<>();
        condition1 = new EventRuleCondition( "class", "=", "*ApplicationControlLogEvent*" );
        conditions.add( condition1 );
        condition2 = new EventRuleCondition( "category", "=", "BITTORRE" );
        conditions.add( condition2 );
        eventRule = new TriggerRule( false, conditions, true, "Tag bittorrent-using hosts", false, 0 );
        eventRule.setAction( TriggerRule.TriggerAction.TAG_HOST );
        eventRule.setTagTarget( "sessionEvent.localAddr" );
        eventRule.setTagName( "bittorrent-usage" );
        eventRule.setTagLifetimeSec( new Long(60*5) ); // 5 minutes
        rules.add( eventRule );
        
        return rules;
    }

    /**
     * Return default email settings values.
     * @return Map of string mapping email values to their defaults.
     */
    public Map<String,String> defaultEmailSettings()
    {
        Map<String, String> emailSettings = new HashMap<>();
        emailSettings.put( "emailSubject", DefaultEmailSubject );
        emailSettings.put( "emailBody", DefaultEmailBody );
        emailSettings.put( "emailConvert", "true");
        return emailSettings;
    }

    /**
     * Construct the email subject and body templates using localizaiton.
     */
    private void buildEmailTemplateDefaults(){
        DefaultEmailSubject = I18nUtil.tr("%system.company% Alert \"%alert.description%\" [%system.host%]", i18nMap);
        DefaultEmailBody = I18nUtil.tr("System: %system.company% [%system.host%]", i18nMap) +
                            "\r\n\r\n" +
                            I18nUtil.tr("Event: %event.class%", i18nMap) +
                            "\r\n\r\n" +
                            I18nUtil.tr("Event Time: %event.timestamp%.", i18nMap) +
                            "\r\n\r\n" +
                            I18nUtil.tr("Event Summary:", i18nMap) +
                            "\r\n" +
                            I18nUtil.tr("%event.summary%", i18nMap) +
                            "\r\n\r\n" +
                            I18nUtil.tr("Event Details:", i18nMap) +
                            "\r\n" +
                            I18nUtil.tr("%event.values.keyvalue%", i18nMap) +
                            "\r\n\r\n" +
                            I18nUtil.tr("This is an automated message sent because this event matched Alerts Rule \"%alert.description%\".", i18nMap);
    }

    /**
     * Construct email template variables.
     */
    @SuppressWarnings("serial")
    private void buildEmailTemplateVariables()
    {
        eventTemplateVariables = new HashMap<>();

        EventTemplateVariable etv;
        etv = new EventTemplateVariable("system.company", I18nUtil.tr("System company or organization name", i18nMap))
        {
            /**
             * Get company name value.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of company name.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                return UvmContextFactory.context().brandingManager().getCompanyName();
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("system.host", I18nUtil.tr("System hostname", i18nMap)){
            /**
             * Get hostname + domain.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of hostname + domain.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                String systemHost = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
                String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
                return systemHost + (  domainName == null ? "" : ( "." + domainName ) );
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("alert.description", I18nUtil.tr("Alert rule description", i18nMap)){
            /**
             * Get alert rule description.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of alert rule description.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                return rule.getDescription();
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("event.class", "Event class"){
            /**
             * Get event class name.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of class event name.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                return event.getClass().getSimpleName();
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("event.timestamp", I18nUtil.tr("Event timestamp", i18nMap)){
            /**
             * Get event timestamp.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of Timestamp.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                return event.getTimeStamp().toString();
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("event.summary", I18nUtil.tr("Event summary", i18nMap)){
            /**
             * Get event timestamp.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of Timestamp.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                return event.toSummaryString();
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("event.values.json", I18nUtil.tr("Event details in JSON format", i18nMap)){
            /**
             * Get event timestamp.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of Timestamp.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                JSONObject jsonObject = event.toJSONObject();
                cleanupJsonObject( jsonObject, convert );
                String jsonString = "";

                try {
                    jsonString = jsonObject.toString(4);
                } catch (org.json.JSONException e) {
                    logger.warn("Failed to pretty print.",e);
                    jsonString = jsonObject.toString();
                }
                return jsonString;
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);

        etv = new EventTemplateVariable("event.values.keyvalue", I18nUtil.tr("Event details in key=value format", i18nMap)){
            /**
             * Get event timestamp.
             * @param  rule  Matching AlertRule
             * @param  event Matching LogEvent
             * @param convert if true, attempt to convert to human readable
             * @return       String of Timestamp.
             */
            @Override
            public String getValue(AlertRule rule, LogEvent event, boolean convert)
            {
                JSONObject jsonObject = event.toJSONObject();
                cleanupJsonObject( jsonObject, convert );
                String jsonString = "";

                try {
                    jsonString = jsonObject.toString(4);
                } catch (org.json.JSONException e) {
                    logger.warn("Failed to pretty print.",e);
                    jsonString = jsonObject.toString();
                }

                Map<String,String> kvPairs = new HashMap<>();

                int maxKeyLength = 0;
                String[] kv;
                String key;
                String subKey = null;
                for(String line: jsonString.split("\n")){
                    line = line.substring(0, line.length() - 1).replaceAll("\"", "");
                    if( line.trim().equals("{") ||
                        line.trim().equals("}") ||
                        line.trim().equals("")) {
                        subKey = null;
                        continue;
                    }
                    kv = line.trim().split(":", 2);
                    key = kv[0].trim();
                    if(kv.length > 1 && kv[1].equals("")){
                        subKey = key;
                    }else if(subKey != null){
                        key = subKey + " " + key;
                    }
                    if(key.length() > maxKeyLength){
                        maxKeyLength = key.length();
                    }
                    kvPairs.put(key, kv.length > 1 ? kv[1].trim() : "");
                }
                SortedSet<String> sortedKeys = new TreeSet<>(kvPairs.keySet());

                List<String> kvStrings = new LinkedList<>();
                for(String k : sortedKeys){
                    String kvString;
                    if(kvPairs.get(k).equals("")){
                        kvString = String.format("%-" + maxKeyLength+ "s", k);
                    }else{
                        String showKey = k;
                        if(showKey.indexOf(' ') != -1){
                            showKey = k.substring(showKey.indexOf(' '));
                        }
                        kvString = String.format("%-" + maxKeyLength+ "s = %s", showKey, kvPairs.get(k));
                    }
                    kvStrings.add(kvString);
                }

                return String.join("\n", kvStrings);
            }
        };
        eventTemplateVariables.put(etv.getName(), etv);
    }

    /**
     * Add event to writer queue.
     * @param event LogEvent to add to writer queue.
     */
    public void logEvent( LogEvent event )
    {
        synchronized (classesToProcess) {
            if(classesToProcess.matcher(event.getClass().getName()).matches()){
                localEventWriter.inputQueue.offer(event);
            }
        }
        remoteEventWriter.inputQueue.offer(event);
    }

    /**
     * Process event through alerts, triggers, and syslog.
     * @param event LogEvent to process.
     */
    private void runEvent( LogEvent event )
    {
        if(event.getClass().getSimpleName().equals(PREVIEW_EVENT_CLASS_NAME)){
            synchronized( this ) {
                mostRecentPreviewEvent = event;
            }
        }

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

    /**
     * Process event through alert rules.
     * @param event LogEvent to process.
     */
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

    /**
     * Process event through trigger rules.
     * @param event LogEvent to process.
     */
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

            String target = findAttribute( jsonObject, rule.getTagTarget() );
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
                if ( rule == null ) break;
                logger.debug("Tagging host " + target + " with tag \"" + rule.getTagName() + "\"");
                host.addTag( new Tag( rule.getTagName(), System.currentTimeMillis()+(rule.getTagLifetimeSec()*1000) ));
                break;
            case UNTAG_HOST:
                logger.debug("Untagging host " + target + " with tag \"" + rule.getTagName() + "\"");
                if ( host == null ) break;
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
                if ( user == null ) break;
                user.addTag( new Tag( rule.getTagName(), System.currentTimeMillis()+(rule.getTagLifetimeSec()*1000) ) );
                break;
            case UNTAG_USER:
                logger.debug("Untagging user " + target + " with tag \"" + rule.getTagName() + "\"");
                if ( user == null ) break;
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
                if ( device == null ) break;
                device.addTag( new Tag( rule.getTagName(), System.currentTimeMillis()+(rule.getTagLifetimeSec()*1000) ) );
                break;
            case UNTAG_DEVICE:
                logger.debug("Untagging device " + target + " with tag \"" + rule.getTagName() + "\"");
                if ( device == null ) break;
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

    /**
     * Process event through syslog rules.
     * @param event LogEvent to process.
     */
    private void runSyslogRules( LogEvent event )
    {
        if ( event == null )
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
            if ( rule.getSyslog() ) {
                try {
                    SyslogManagerImpl.sendSyslog( event );
                } catch (Exception exn) {
                    logger.warn("failed to send syslog", exn);
                }
            }
        }
    }

    /**
     * Retreive an attribute value using the attribute name from the object.
     * @param  json         JSONObject to search.
     * @param  name         String of key to find.
     * @return              String of matching value.  Null if not found.
     */
    private static String findAttribute( JSONObject json, String name )
    {
        String s = null;
        if ( (s = findAttributeRecursive( json, name )) != null )
            return s;
        if ( ( name != null ) && !name.contains(".") )
            if ( (s = findAttributeFlatten( json, name, 3 )) != null )
                return s;
        return s;
    }

    /**
     * This looks for a specific JSON attribute
     * foo.bar.baz returns json['foo']['bar']['baz']
     * @param  json JSONObject to search.
     * @param  name String of key to find.
     * @return              String of matching value.  Null if not found.
     */
    private static String findAttributeRecursive( JSONObject json, String name )
    {
        if ( json == null || name == null ) return null;

        try {
            String[] parts = name.split("\\.",2);
            if ( parts.length < 1 )
                return null;

            String fieldName = parts[0];

            Object o = null;
            try {o = json.get(fieldName);} catch(Exception exc) {}
            if ( o == null )
                return null;

            if ( parts.length > 1 ) {
                String subName = parts[1];
                return findAttributeRecursive( new JSONObject(o), subName );
            } else {
                return o.toString();
            }
        } catch (Exception e) {
            logger.warn("Failed to find attribute: " + name,e);
            return null;
        }
    }

    /**
     * This looks through JSONObjects recursively to find any attribute with the specified name
     * It looks up to maxDepth levels to prevent cycles
     * @param  json JSONObject to search.
     * @param  name String of key to find.
     * @param maxDepth integer of maximum depth to search.
     * @return              String of matching value.  Null if not found.
     */
    private static String findAttributeFlatten( JSONObject json, String name, int maxDepth )
    {
        if ( json == null || name == null ) return null;
        if ( maxDepth < 1 ) return null;

        //logger.info("findAttributeFlatten( " + name + " , " + json + ")");
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
                        String s = findAttributeFlatten(obj,name,maxDepth-1);
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

    /**
     * Send this event as an email alert notification.
     * @param  rule  Matching alert rule.
     * @param  event LogEvent to send.
     * @return       boolean if true, alert generated and sent, false if not sent.
     */
    private static boolean sendEmailForEvent( AlertRule rule, LogEvent event )
    {
        if(rule.frequencyCheck() == false){
            return false;
        }

        Map<String, String> email = emailAlertFormat(rule, event, UvmContextFactory.context().eventManager().getSettings().getEmailSubject(), UvmContextFactory.context().eventManager().getSettings().getEmailBody(), UvmContextFactory.context().eventManager().getSettings().getEmailConvert());
        LinkedList<String> alertRecipients = new LinkedList<>();

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
                UvmContextFactory.context().mailSender().sendMessage( recipients, email.get("emailSubject"), email.get("emailBody"));
            } catch ( Exception e) {
                logger.warn("Failed to send mail.",e);
            }
        }

        return true;
    }

    /**
     * Make json formatted event more suitable for users.
     * By default, leave values as-is.
     * @param jsonObject JSONObject to process.
     */
    private static void cleanupJsonObject( JSONObject jsonObject )
    {
        cleanupJsonObject( jsonObject, false);
    }

    /**
     * Make json formatted event more suitable for users:
     * * Remove unncessessary fields.
     * * Recursively clean.
     * @param jsonObject JSONObject to process.
     * @param human
     */
    private static void cleanupJsonObject( JSONObject jsonObject, boolean human )
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

            try{
                String ipAddress = jsonObject.getString(key);
                if(IPMatcher.JAVA_IPADDR_REGEX.matcher(ipAddress).matches()){
                    jsonObject.put(key, ipAddress.substring(1));
                }
            }catch(Exception e){}

            if(human){
                if(key.indexOf("Percent") > -1){
                    try{
                        jsonObject.put(key, Integer.toString((int)Math.round(jsonObject.getDouble(key) * 100)) + "%");
                    }catch(Exception e){}
                }else{
                    try{
                        long value = jsonObject.getLong(key);
                        if(value > 0){
                            jsonObject.put(key, StringUtil.longToHumanReadable(value));
                        }
                    }catch(Exception e){}
                }
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
     * This thread waits on the inputQueue
     */
    private class LocalEventWriter implements Runnable
    {
        private volatile Thread thread;
        private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<>();

        /**
         * Run event queue.
         */
        public void run()
        {
            thread = Thread.currentThread();
            long lastLoggedWarningTime = 0;

            /**
             * Loop indefinitely and continue running event rules
             */
            while (thread != null) {
                synchronized( this ) {
                    try {
                        // only log this warning once every 10 seconds
                        if (inputQueue.size() > 20000 && System.currentTimeMillis() - lastLoggedWarningTime > 10000 ) {
                            logger.warn("Large local input queue size: " + inputQueue.size());
                            lastLoggedWarningTime = System.currentTimeMillis();
                        }

                        runEvent( inputQueue.take() );

                    } catch (Exception e) {
                        logger.warn("Failed to run event rules.", e);
                        try {this.wait(1000);} catch (Exception exc) {}
                    } 
                }
            }
        }

        /**
         * Start the thread.
         */
        protected void start()
        {
            UvmContextFactory.context().newThread(this).start();
        }

        /**
         * Stop the thread.
         */
        protected void stop()
        {
            Thread tmp = thread;
            thread = null; /* thread will exit if thread is null */
            if (tmp != null) {
                tmp.interrupt();
            }
        }
    }

    /**
     * This thread waits on the inputQueue
     */
    private class RemoteEventWriter implements Runnable
    {
        private volatile Thread thread;
        private final BlockingQueue<LogEvent> inputQueue = new LinkedBlockingQueue<>();

        /**
         * Run event queue.
         */
        public void run()
        {
            thread = Thread.currentThread();
            long lastLoggedWarningTime = 0;

            /**
             * Loop indefinitely and continue running event rules
             */
            while (thread != null) {
                synchronized( this ) {
                    try {
                        // only log this warning once every 10 seconds
                        if (inputQueue.size() > 20000 && System.currentTimeMillis() - lastLoggedWarningTime > 10000 ) {
                            logger.warn("Large remote input queue size: " + inputQueue.size());
                            lastLoggedWarningTime = System.currentTimeMillis();
                        }

                        UvmContextFactory.context().hookManager().callCallbacks( HookManager.REPORTS_EVENT_LOGGED, inputQueue.take());

                    } catch (Exception e) {
                        logger.warn("Failed to run event rules.", e);
                        try {this.wait(1000);} catch (Exception exc) {}
                    }
                }
            }
        }

        /**
         * Start the thread.
         */
        protected void start()
        {
            UvmContextFactory.context().newThread(this).start();
        }

        /**
         * Stop the thread.
         */
        protected void stop()
        {
            Thread tmp = thread;
            thread = null; /* thread will exit if thread is null */
            if (tmp != null) {
                tmp.interrupt();
            }
        }
    }

    /**
     * Class for event template variables.
     */
    @SuppressWarnings("serial")
    public class EventTemplateVariable implements Serializable, JSONString
    {
        private static final char NAME_PREFIX_SUFFIX = '%';
        private String name;
        private String description;
        private Pattern nameMatcher = null;

        public EventTemplateVariable(String name, String description)
        {
            // this.name = NAME_PREFIX_SUFFIX + name + NAME_PREFIX_SUFFIX;
            this.setName(name);
            this.description = description;
        }

        public String getName() { return name; }
        public void setName( String name ) {
            this.name = NAME_PREFIX_SUFFIX + name + NAME_PREFIX_SUFFIX;
            this.nameMatcher = Pattern.compile(this.name);
        }

        public Pattern getNameMatcher(){ return nameMatcher; };

        public String getDescription() { return description; }
        public void setDescription( String description ) { this.description = description; }

        /**
         * [getValue description]
         * @return [description]
         */
        public String getValue(AlertRule rule, LogEvent event, boolean convert){ return ""; }

        public String toJSONString()
        {
            JSONObject jO = new JSONObject(this);
            return jO.toString();
        }
    }


}
