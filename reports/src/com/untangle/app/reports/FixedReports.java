/**
 * $Id: ReportsHandler.java,v 1.00 2015/11/25 11:56:09 cblaise Exp $
 */
package com.untangle.app.reports;

import org.apache.log4j.Logger;

import com.untangle.uvm.MailSender;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.util.I18nUtil;

import com.untangle.uvm.WebBrowser;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;

import java.net.URLEncoder;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

/**
 * Generate fixed reports from django-like templates
 */
@SuppressWarnings("serial")

public class FixedReports
{
    private static final Logger logger = Logger.getLogger( FixedReports.class );

    public static final String REPORTS_FIXED_TEMPLATE_FILENAME =  System.getProperty("uvm.lib.dir") + "/reports/templates/reports.html";
    public static final int DEFAULT_BROWSER_WIDTH = 800;
    public static final int DEFAULT_BROWSER_HEIGHT = 400;
    public static final int MOBILE_BROWSER_WIDTH = 350;
    public static final int MOBILE_BROWSER_HEIGHT = 350;

    private StringBuilder messageText = null;

    private StringBuilder currentInputLine = null;
    private StringBuilder currentOutputLine = null;

    private List<Map<MailSender.MessagePartsField,String>> messageParts;
    private WebBrowser webbrowser = null;
    private Date startDate = null;
    private Date endDate = null;

    I18nUtil i18nUtil = null;
    private static final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    public enum ParsePass{
        PRE,
        POST
    }
    ParsePass currentParsePass;

    public enum Tag {
        _SYSTEM,
        _CYCLE,
        VARIABLE,
        TRANS,
        FOR,
        ENDFOR,
        IF,
        ELSE,
        ENDIF,
        WITH,
        ENDWITH,
        CYCLE_INITIALIZE,
        CYCLE_NEXT,
        COMMENT_BEGIN,
        COMMENT_END
    }

    public enum Filter{
        FIRST,
        DISTINCT,
        FORMAT,
        IN,
        ORDER
    }

    public enum ConditionalE {
        LOGICAL,
        EQUALITY
    }

    private static final Map<Tag, Pattern> TagPatterns;
    private static final Map<Filter, Pattern> FilterPatterns;
    private static final Pattern NonGreedyVariablePattern;
    private static final Pattern NumericOnlyPattern;
    private static final Map<ParsePass, String> ParsePassActiveVariables;
    private static final ArrayList<String> ConfigCategories;
    private static final Map<ConditionalE,Pattern> ConditionalPatterns;
    public static final ArrayList<String> ReservedReports;

    static {
        TagPatterns = new HashMap<>();

        TagPatterns.put(Tag.VARIABLE, Pattern.compile("\\{\\{\\s*(.+)\\s*\\}\\}"));
        TagPatterns.put(Tag.TRANS, Pattern.compile("\\{\\%\\s*trans \"([^\"]+)\"\\s*\\%\\}"));
        TagPatterns.put(Tag.FOR, Pattern.compile("\\{\\%\\s*for (.+?) in (.+?)\\s*\\%\\}"));
        TagPatterns.put(Tag.ENDFOR, Pattern.compile("\\{\\%\\s*endfor\\s*\\%\\}"));
        TagPatterns.put(Tag.IF, Pattern.compile("\\{\\%\\s*if (.+?)\\s*\\%\\}"));
        TagPatterns.put(Tag.ELSE, Pattern.compile("\\{\\%\\s*else\\s*\\%\\}"));
        TagPatterns.put(Tag.ENDIF, Pattern.compile("\\{\\%\\s*endif\\s*\\%\\}"));
        TagPatterns.put(Tag.WITH, Pattern.compile("\\{\\%\\s*with (.+?)\\=(.+?)\\s*\\%\\}"));
        TagPatterns.put(Tag.ENDWITH, Pattern.compile("\\{\\%\\s*endwith\\s*\\%\\}"));

        TagPatterns.put(Tag.COMMENT_BEGIN, Pattern.compile("\\<\\!\\-\\-\\s*"));
        TagPatterns.put(Tag.COMMENT_END, Pattern.compile("\\s*\\-\\-\\>"));

        TagPatterns.put(Tag.CYCLE_INITIALIZE, Pattern.compile("\\{\\%\\s*cycle (.+?) as (.+?) \\s*\\%\\}"));
        TagPatterns.put(Tag.CYCLE_NEXT, Pattern.compile("\\{\\%\\s*cycle ([^\\s]+?) \\s*\\%\\}"));

        FilterPatterns = new HashMap<>();
        FilterPatterns.put(Filter.FIRST, Pattern.compile("first"));
        FilterPatterns.put(Filter.DISTINCT, Pattern.compile("distinct\\=([^,]+)"));
        FilterPatterns.put(Filter.FORMAT, Pattern.compile("format\\=([^,]+),(.+)"));
        FilterPatterns.put(Filter.IN, Pattern.compile("in\\=([^,]+),(.+)"));
        FilterPatterns.put(Filter.ORDER, Pattern.compile("order\\=([^,]+),(.+)"));

        NonGreedyVariablePattern = Pattern.compile("\\{\\{\\s*(.+?)\\s*\\}\\}");

        NumericOnlyPattern = Pattern.compile("-?\\d+(.\\d+)?");

        ParsePassActiveVariables = new HashMap<>();
        ParsePassActiveVariables.put(ParsePass.POST, "url");

        ConfigCategories = new ArrayList<>();
        ConfigCategories.add("Hosts");
        ConfigCategories.add("Devices");
        ConfigCategories.add("Network");
        ConfigCategories.add("Administration");
        ConfigCategories.add("Events");
        ConfigCategories.add("System");
        ConfigCategories.add("Shield");

        // This would be better as an external file for easier modification and not
        // hanging onto the memory.
        ReservedReports = new ArrayList<>();
        // Ad Blocker Summary
        ReservedReports.add("ad-blocker-WvH1wCQQ0D");
        // Ads Blocked
        ReservedReports.add("ad-blocker-nvhtmu6LXi");
        // Admin Logins
        //ReservedReports.add("Administration-tFb0iLvxHE");
        // Application Control Summary
        ReservedReports.add("application-control-upl31dqKb1");
        // Top Applications Usage
        ReservedReports.add("application-control-OAI5zmhxOM");
        // Application Control Lite Summary
        ReservedReports.add("application-control-lite-upl31dqKb1");
        // Detection Statistics
        ReservedReports.add("application-control-lite-9Yyq8iXZJ5");
        // Bandwidth Control Summary
        ReservedReports.add("bandwidth-control-upl31dqKb1");
        // Bandwidth Usage
        ReservedReports.add("bandwidth-control-StzlzfZAp8");
        // Captive Portal Summary
        ReservedReports.add("captive-portal-upl31dqKb1");
        // Activity Summary
        ReservedReports.add("captive-portal-psXTQbdE");
        // Configuration Backup Summary
        ReservedReports.add("configuration-backup-eN8Ot9wh");
        // Backup Usage (all)
        ReservedReports.add("configuration-backup-HF3qFZ9M");
        // Devices Additions
        //ReservedReports.add("device-table-UkYvElV11f");
        // Devices Updates
        //ReservedReports.add("device-table-WGQUSYhIck");
        // Directory Connector Summary
        ReservedReports.add("directory-connector-upl31dqKb1");
        // User Notification API Events
        ReservedReports.add("directory-connector-D6IabIxIrC");
        // Firewall Summary
        ReservedReports.add("firewall-upl31dqKb1");
        // Scanned Sessions
        ReservedReports.add("firewall-8bTqxKxxUK");
        // Hosts Active
        ReservedReports.add("host-viewer-pfRvYDKKQx");
        // Hosts Additions
        //ReservedReports.add("host-viewer-UkYvElV11f");
        // Hosts Updates
        //ReservedReports.add("host-viewer-WGQUSYhIck");
        // IPsec VPN Summary
        ReservedReports.add("ipsec-vpn-upl31dqKb1");
        // Hourly Tunnel Traffic
        ReservedReports.add("ipsec-7y1o6zC1Ez");
        // Intrusion Prevention Summary
        ReservedReports.add("intrusion-prevention-kt095LB6");
        // Intrusion Detection (all)
        ReservedReports.add("intrusion-prevention-pYviv7Cg");
        // Network Summary
        ReservedReports.add("network-tn9iaE74pK");
        // Network Data Usage
        ReservedReports.add("network-aGUe5wYZ1x");
        // Sessions
        ReservedReports.add("network-8bTqxKxxUK");
        // Bandwidth Usage
        ReservedReports.add("network-StzlzfZAp8");
        // OpenVPN Summary
        ReservedReports.add("openvpn-upl31dqKb1");
        // OpenVPN Bandwidth Usage
        ReservedReports.add("openvpn-StzlzfZAp8");
        // Phish Blocker Summary
        ReservedReports.add("phish-blocker-DniRBEni");
        // Email Usage (all)
        ReservedReports.add("phish-blocker-iZV0Z13m");
        // Policy Manager Summary
        ReservedReports.add("policy-manager-upl31dqKb1");
        // Top Policy Usage
        ReservedReports.add("policy-manager-hWC6KjOc8Y");
        // SSL Inspector Summary
        ReservedReports.add("ssl-inspector-ggDy9pSApA");
        // Scanned Sessions
        ReservedReports.add("ssl-inspector-F10QTQJPXF");
        // Scanned Sessions
        //ReservedReports.add("shield-2ObNkapIEq");
        // Spam Blocker Summary
        ReservedReports.add("spam-blocker-gnmDTFRS");
        // Email Usage (all)
        ReservedReports.add("spam-blocker-exreIeeR");
        // Spam Ratio
        ReservedReports.add("spam-blocker-QuhTJ1ude8");
        // Spam Blocker Lite Summary
        ReservedReports.add("spam-blocker-lite-DniRBEni");
        // Email Usage (all)
        ReservedReports.add("spam-blocker-lite-iZV0Z13m");
        // Spam Ratio
        ReservedReports.add("spam-blocker-lite-QuhTJ1ude8");
        // CPU Load
        ReservedReports.add("system-LJnwhWuJiN");
        // Memory Usage
        ReservedReports.add("system-fgQnUn1Tle");
        // Disk Usage
        ReservedReports.add("system-6iYMGsnldQ");
        // Swap Usage Ratio
        ReservedReports.add("system-N63OfrLqbS");
        // Highest Active Hosts
        ReservedReports.add("system-lL959lz7qu");
        // Web Traffic Summary
        ReservedReports.add("threat-prevention-tpupl31dqKb1h");
        // Web Top Scanned Threats
        ReservedReports.add("threat-prevention-tpsAsiftrbsss");
        // Non-Web Top Scanned Threats (by client)
        ReservedReports.add("threat-prevention-tpAsifWhYotrbsc");
        // Virus Blocker FTP Summary
        ReservedReports.add("virus-blocker-ugosjuGk");
        // Virus Blocker Email Summary
        ReservedReports.add("virus-blocker-dR0pxxoH");
        // Virus Blocker Web Summary
        ReservedReports.add("virus-blocker-bCgxepqj");
        // Web Usage (all)
        ReservedReports.add("virus-blocker-9gTFTMGF");
        // FTP Usage (all)
        ReservedReports.add("virus-blocker-JJ05hQYG");
        // Email Usage (all)
        ReservedReports.add("virus-blocker-R61SMfc9");
        // Virus Blocker Lite FTP Summary
        ReservedReports.add("virus-blocker-lite-pi3IfwzM");
        // Virus Blocker Lite Email Summary
        ReservedReports.add("virus-blocker-lite-CRxmUhVM");
        // Virus Blocker Lite Web Summary
        ReservedReports.add("virus-blocker-lite-omMJnSjI");
        // Web Usage (all)
        ReservedReports.add("virus-blocker-lite-Zj70iUtK");
        // FTP Usage (all)
        ReservedReports.add("virus-blocker-lite-4v7yTaQa");
        // Email Usage (all)
        ReservedReports.add("virus-blocker-lite-lBdJV59j");
        // WAN Balancer Summary
        ReservedReports.add("wan-balance-upl31dqKb1");
        // WAN Failover Summary
        ReservedReports.add("wan-failover-upl31dqKb1");
        // Web Cache Summary
        ReservedReports.add("web-cache-q97vptQHbv");
        // Cache Hit/Miss Statistics
        ReservedReports.add("webcache-sYa4T0zsOs");
        // Web Filter Summary
        ReservedReports.add("web-filter-q97vptQHbv");
        // Web Usage
        ReservedReports.add("web-filter-h0jelsttGp");
        // Top Domains Usage
        ReservedReports.add("web-filter-2nx8FA4VCB");
        // Web Monitor Summary
        ReservedReports.add("web-monitor-q97vptQHbv");
        // Web Usage
        ReservedReports.add("web-monitor-h0jelsttGp");
        // Top Domains Usage
        ReservedReports.add("web-monitor-2nx8FA4VCB");

        // Order matters when processing
        ConditionalPatterns = new LinkedHashMap<>();
        ConditionalPatterns.put(ConditionalE.LOGICAL, Pattern.compile("(.+?)\\s+(not\\s+|)(and|or)\\s+(.+)"));
        ConditionalPatterns.put(ConditionalE.EQUALITY, Pattern.compile("(.+?)\\s+(not\\s+|\\!|)(in|\\=|\\=\\=|\\<|\\>)\\s+(.+)"));
    }

    private static final Pattern Conditional = Pattern.compile("(.+?)\\s+(not\\s+|\\!\\s+)(\\=\\=|in)\\s+(.+)");
    private static final Pattern ConditionalLogical = Pattern.compile("(.+?)\\s+(not\\s+|)(and|or)\\s+(.+)");

    /**
     * Variable context
     */
    class variableContext
    {
        Tag tag;
        Object object;
        String name;
        int index;

        /**
         * Initialize this current variable context.
         *
         * @param tag
         *  Tag for the context.
         * @param name
         *  Name of variable.
         * @param object
         *  Variable object.
         */
        public variableContext(Tag tag, String name, Object object)
        {
            this.tag = tag;
            this.name = name;
            this.object = object;
            this.index = -1;
        }

        /**
         * Display context as a string for debugging purposes.
         *
         * @return
         *  String of tag and name.
         */
        public String toString(){
            return tag + ":" + name;
        }
    }

    /**
     * Conditional context
     */
    class conditionalContext
    {
        Boolean match;

        /**
         * Initialize conditional context
         *
         * @param match
         *  true to match, false to not match.
         */
        public conditionalContext(Boolean match)
        {
            this.match = match;
        }

        /**
         * Get match value.
         * 
         * @return
         *  true if match is true, false otherwise.
         */
        public Boolean getMatch(){
            return match;
        }
    }

    /**
     * Parse context.
     */
    class parseContext
    {
        Boolean allowOutput = true;
        Boolean ignoreLine = true;
        Boolean buildLoopBuffer = false;
        Boolean inComment = false;

        int loopsSeen = 0;
        StringBuilder loopBuffer = null;

        Object variableObject;
        String variableName;
        int variableIndex;

        /**
         * Initialize this context.
         */
        public parseContext()
        {
            loopBuffer = new StringBuilder();
        }

        List<variableContext> variables = new ArrayList<>();
        List<conditionalContext> conditionals = new ArrayList<>();

        /**
         * Add variable to this context.
         * 
         * @param tag
         *  Tag for this variable.
         * @param name
         *  Name of variable.
         * @param object
         *  Variable object.
         */
        public void addVariable(Tag tag, String name, Object object){

            /* Replace if found */
            variableContext vc = null;
            for( int i = 0; i < variables.size(); i ++){
                vc = variables.get(i);
                if(vc.tag == tag && vc.name.equals(name)){
                    variables.set(i, new variableContext(tag, name, object));
                    return;
                }
            }
            /* Otherwise, add*/
            variables.add(new variableContext(tag, name, object));
        }

        /**
         * Remove the variable specified by tag.
         *
         * @param tag
         *  Variable to remove.
         */
        public void removeVariable(Tag tag){
            for(variableContext vc : variables){
                if(vc.tag == tag){
                    variables.remove(vc);
                    break;
                }
            }
        }

        /**
         * Get the variale by tag.
         *
         * @param tag
         *  Tag to match.
         * @return
         *  variableContext object or null if not found.
         */
        public variableContext getVariableContext(Tag tag){
            for(variableContext vc: variables){
                if(vc.tag.equals(tag)){
                    return vc;
                }
            }
            return null;
        }

        /**
         * Get the variale by tag and name.
         *
         * @param tag
         *  Tag to match.
         * @param name
         *  Variable name to match.
         * @return
         *  variableContext object or null if not found.
         */
        public variableContext getVariableContext(Tag tag, String name){
            for(variableContext vc: variables){
                if(vc.tag.equals(tag) && vc.name.equals(name)){
                    return vc;
                }
            }
            return null;
        }

        /**
         * Multi-level conditional support in a context 
         * 
         * @param match
         *  true to match, false to not match.
         */
        public void pushConditional(Boolean match){
            conditionals.add(new conditionalContext(match));
        }

        /**
         * If current level match is what we want and nested is true, we match
         *
         * @param wantMatch
         *  The match to find.
         * @return
         *  true if found, false if not.
         */
        public Boolean getCurrentConditionalMatch(Boolean wantMatch){
            Boolean walkMatch = true;
            Boolean match = true;
            for(conditionalContext cc : conditionals){
                if(cc == conditionals.get(conditionals.size()-1)){
                    match = (wantMatch == cc.getMatch());
                }else if(cc.getMatch() == false){
                    walkMatch = false;
                }
            }
            return match && walkMatch;
        }

        /**
         * Remove last conditional.
         */
        public void popConditional(){
            conditionals.remove(conditionals.get(conditionals.size()-1));
        }

        /**
         * Return variable's object by name,
         *
         * @param name
         *  Name of variable find.
         * @return
         *  Variable object or null if not found.
         */
        public Object getVariable(String name){
            for(variableContext vc: variables){
                if(vc.name.equals(name)){
                    if(vc.index != -1){
                        return ((List) vc.object).get(vc.index);
                    }else{
                        return vc.object;
                    }
                }
            }
            return null;
        }

        /**
         * Set varaible's object.
         * 
         * @param name
         *  Name of variaable.
         * @param obj
         *  Variable object.
         */
        public void setVariable(String name, Object obj)
        {
            variableName = name;
            variableObject = obj;
            variableIndex = 0;
        }

        /**
         * Unset current variable name and object.
         */
        public void unsetVariable()
        {
            variableName = null;
            variableObject = null;
        }

        /**
         * Add line to the loopBuffer.
         *
         * @param line
         *  Line to add.
         */
        public void addToBuffer(String line){
            if(ignoreLine == false && getInComment() == false){
                loopBuffer.append(line);
            }
        } 

        /**
         * Toggle the value of being inside a comment.
         *
         * @param value
         *  true if in comment, false if not.
         */
        public void setInComment(Boolean value){
            inComment = value;
        }
        /**
         * Get current status of bing in a comment.
         * @return
         *  true if in comment, false if not.
         */
        public Boolean getInComment(){
            return inComment;
        }
    }
    private List<parseContext> parseContextStack;

    /**
     * Check for current context match.
     *
     * @param wantMatch
     *  true if should match, false if not.
     * @return
     *  true if match found
     */
    private boolean getParseContextStackMatch(Boolean wantMatch)
    {
        Boolean match = true;

        for(parseContext pc: parseContextStack){
            match &= pc.getCurrentConditionalMatch(wantMatch);
        }

        return match;
    }

    /**
     * Selecting system information in a general way. 
     * Selector is formatted like fields[,arguments][|filters]
     */
    class selector
    {
        List<String> fields = null;
        List<String> arguments = null;
        List<String> filters = null;
        String selectorString = null;

        /**
         * Iniitalize selector from string.
         *
         * @param selectorString
         *  String containing selector.
         */
        public selector(String selectorString)
        {
            this.selectorString = selectorString;
            /*
             * Parse variables
             */
            Matcher tag = NonGreedyVariablePattern.matcher(selectorString);
            while( tag.find()){
                Object variable = getVariable(new selector(tag.group(1)));
                selectorString = 
                    selectorString.substring(0,selectorString.indexOf(tag.group())) + 
                    variable.toString() +
                    selectorString.substring(selectorString.indexOf(tag.group()) + tag.group().length());
            }

            filters = new ArrayList<>(Arrays.asList(selectorString.split("\\|")));
            arguments = new ArrayList<>(Arrays.asList(filters.get(0).split("\\,")));
            fields = new ArrayList<>(Arrays.asList(arguments.get(0).split("\\.")));
            filters.remove(0);
            arguments.remove(0);
        }

        /**
         * Display selector as its original string value.
         *
         * @return 
         *  Originally passed selector string.
         */
        public String toString(){
            return selectorString;
        }

    }

    /**
     * Get the configuration categories.
     *
     * @return
     *  List of category strings.
     */
    public List<String> getConfigCategories()
    {
        return FixedReports.ConfigCategories;
    }

    private ReportsManager reportsManager;

    /**
     * Initialize fixed reports instance with a web browser instance if supported.
     * @return instance of fixed reports/
     */
    public FixedReports(){
        webbrowser = null;
        if(WebBrowser.exists()){
            try{
                webbrowser = new WebBrowser(1, 5, DEFAULT_BROWSER_WIDTH, DEFAULT_BROWSER_HEIGHT, 8);
            }catch(Exception e){
                logger.warn("Unable to start WebBrowser instance",e);
                webbrowser = null;
            }
        }

    }

    /**
     * When destroying the instance, shut down the web browser.
     */
    public void destroy(){
        if(webbrowser != null){
            webbrowser.close();
        }
        webbrowser = null;
    }

    /**
     * Create and send fixed reports based on current day.
     *
     * @param emailTemplate
     *  EmailTemplate to process.
     * @param users
     *  Email addresses that will receive generatewd report.
     * @param reportsUrl
     *  URL to include in the message.
     * @param reportsManager
     *  ReportsManager object.
     */
    public void generate(EmailTemplate emailTemplate, List<ReportsUser> users, String reportsUrl, ReportsManager reportsManager)
    {
        generate(emailTemplate, users, reportsUrl, reportsManager, 0, 0);
    }

    /**
     * Create and send fixed reports based on explicit start and stop timestamps.
     *
     * @param emailTemplate
     *  EmailTemplate to process.
     * @param users
     *  Email addresses that will receive generatewd report.
     * @param reportsUrl
     *  URL to include in the message.
     * @param reportsManager
     *  ReportsManager object.
     * @param startTimestamp
     *  Beginning timestamp.
     * @param stopTimestamp
     *  Ending timestamp
     */
    public void generate(EmailTemplate emailTemplate, List<ReportsUser> users, String reportsUrl, ReportsManager reportsManager, long startTimestamp, long stopTimestamp)
    {
        this.reportsManager = reportsManager;

        Map<String, String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
        i18nUtil = new I18nUtil(i18nMap);

        String interval = "";
        String intervalDescription = "";
        Calendar c = Calendar.getInstance();

        if(startTimestamp != 0){
            c.setTimeInMillis(startTimestamp);
        }
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        Date currentDate = c.getTime();

        if (emailTemplate.getInterval() == 2419200) {
            // Monthly
            interval = i18nUtil.tr("Monthly");

            if(startTimestamp != 0){
                startDate = c.getTime();
                c.add(Calendar.MONTH, 1);
                c.set(Calendar.DAY_OF_MONTH, 1);
                endDate = c.getTime();
            }else{
                c.set(Calendar.DAY_OF_MONTH, 1);
                endDate = c.getTime();
                c.add(Calendar.DATE, -1);
                c.set(Calendar.DAY_OF_MONTH, 1);
                startDate = c.getTime();
            }

            intervalDescription = dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate);
        } else if (emailTemplate.getInterval() == 2) {
            // Month to Date
            interval = i18nUtil.tr("Month to Date");

            if(startTimestamp != 0){
                startDate = c.getTime();
                c.setTimeInMillis(stopTimestamp);
                endDate = c.getTime();
            }else{
                endDate = c.getTime();
                c.set(Calendar.DAY_OF_MONTH, 1);
                startDate = c.getTime();
            }

            intervalDescription = dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate);
        } else if(emailTemplate.getInterval() == 604800) {
            // Weekly, Sunday through Sunday
            interval = i18nUtil.tr("Weekly");

            if(startTimestamp != 0){
                startDate = c.getTime();
                c.setTimeInMillis(startTimestamp + ( 86400000 * 7) );
                endDate = c.getTime();
            }else{
                c.set(Calendar.DAY_OF_WEEK, emailTemplate.getIntervalWeekStart() + 1);
                endDate = c.getTime();
                c.add(Calendar.DATE, -7);
                startDate = c.getTime();
            }
            intervalDescription = dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate);
        } else if(emailTemplate.getInterval() == 1) {
            // Week to Date
            interval = i18nUtil.tr("Week to Date");

            if(startTimestamp != 0){
                startDate = c.getTime();
                c.setTimeInMillis(stopTimestamp);
                endDate = c.getTime();
            }else{
                endDate = c.getTime();
                c.set(Calendar.DAY_OF_WEEK, emailTemplate.getIntervalWeekStart() + 1);
                startDate = c.getTime();
            }
            intervalDescription = dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate);
        } else if(emailTemplate.getInterval() == 86400) {
            // Daily
            interval = i18nUtil.tr("Daily");

            if(startTimestamp != 0){
                startDate = c.getTime();
                c.add(Calendar.DATE, 1);
                endDate = c.getTime();
            }else{
                c.add(Calendar.DATE, - 1);
                startDate = c.getTime();
                c.add(Calendar.DAY_OF_MONTH, 1);
                endDate = c.getTime();
            }
            intervalDescription = dateFormatter.format(startDate);
        } else {
            // Daily
            interval = i18nUtil.tr("Daily");
            c.add(Calendar.DATE, - 1);
            startDate = c.getTime();
            c.add(Calendar.DAY_OF_MONTH, 1);
            endDate = c.getTime();
            intervalDescription = dateFormatter.format(startDate);
        }

        if ( (startTimestamp == 0 ) && (currentDate.compareTo(endDate) != 0) ){
            logger.warn("Skipping report " + emailTemplate.getTitle() + " because its not its end date: " + endDate);
            return;
        }

        String title = emailTemplate.getTitle() + 
            ": " + interval + " (" + intervalDescription + ")" + 
            (emailTemplate.getMobile() == true ? " " + i18nUtil.tr("Mobile") : "");

        // Determine users lists with/without online access for url inclusion
        List<String> recipientsWithoutOnlineAccess = new ArrayList<>();
        List<String> recipientsWithOnlineAccess = new ArrayList<>();
        for(ReportsUser user: users){
            List<String> emailAddresses = null;
            if(user.getEmailAddress().equals("admin")){
                emailAddresses = reportsManager.getAdminEmailAddresses();
            }else{
                emailAddresses = new ArrayList<>();
                emailAddresses.add(user.getEmailAddress());
            }
            if(user.getOnlineAccess() == true){
                recipientsWithOnlineAccess.addAll(emailAddresses);
            }else{
                recipientsWithoutOnlineAccess.addAll(emailAddresses);
            }
        }
        if( (recipientsWithOnlineAccess.size() == 0 ) && 
            (recipientsWithoutOnlineAccess.size() == 0) ){
            return;
        }

        logger.warn("Generating report for \"" + title + "\"");

        if(webbrowser != null){
            Integer browserWidth = DEFAULT_BROWSER_WIDTH;
            Integer browserHeight = DEFAULT_BROWSER_HEIGHT;
            if(emailTemplate.getMobile() == true){
                browserWidth = MOBILE_BROWSER_WIDTH;
                browserHeight = MOBILE_BROWSER_HEIGHT;
            }
            webbrowser.resize( browserWidth, browserHeight);
        }

        File fixedReportTemplateFile = new File(REPORTS_FIXED_TEMPLATE_FILENAME);

        List<String> allowedReportTypes = new ArrayList<>();
        for(ReportEntry.ReportEntryType r : ReportEntry.ReportEntryType.values()){
            if(r.name().equals("EVENT_LIST")){
                continue;
            }
            if(webbrowser == null &&
                r.name().indexOf("_GRAPH") > -1 ){
                continue;
            }
            allowedReportTypes.add(r.name());
        }

        messageParts = new ArrayList<>();
        messageText = new StringBuilder();
        messageText.append(i18nUtil.tr("HTML Report enclosed.") + "\n\n");

        List<StringBuilder> inputLines = new ArrayList<>();
        List<StringBuilder> outputLines = new ArrayList<>();

        // Read template to input
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(fixedReportTemplateFile), "UTF-8"));

            String line;
            while ((line = reader.readLine()) != null) {
                inputLines.add(new StringBuilder(line));
            }

        } catch (IOException e) {
            logger.warn("IOException: ",e);
        } finally {
            try {
                if(reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                logger.warn("cannot close: ", e);

            }
        }

        Map<String,Object> variableKeyValues = new HashMap<>();
        variableKeyValues.put("startDate", startDate);
        variableKeyValues.put("endDate", endDate);
        variableKeyValues.put("title", title);
        variableKeyValues.put("emailTemplate", emailTemplate);
        variableKeyValues.put("FixedReports", this);
        variableKeyValues.put("allowedReportTypes", allowedReportTypes);

        // It would be "better" if the template language could process this directly from the objects...
        List<String> enabledConfigIds = emailTemplate.getEnabledConfigIds();
        if(enabledConfigIds != null && enabledConfigIds.size() > 0 && enabledConfigIds.get(0).equals("_recommended")){
            variableKeyValues.put("enabledConfigIds", ReservedReports);
        }else{
            variableKeyValues.put("enabledConfigIds", enabledConfigIds);
        }
        List<String> enabledAppIds = emailTemplate.getEnabledAppIds();
        if(enabledAppIds != null && enabledAppIds.size() > 0 && enabledAppIds.get(0).equals("_recommended")){
            variableKeyValues.put("enabledAppIds", ReservedReports);
        }else{
            variableKeyValues.put("enabledAppIds", enabledAppIds);
        }

        currentParsePass = ParsePass.PRE;
        parseBuffer(inputLines, outputLines, variableKeyValues);
        inputLines = outputLines;

        currentParsePass = ParsePass.POST;
        if(recipientsWithoutOnlineAccess.size() > 0 ){
            variableKeyValues.put("url", "");
            outputLines = new ArrayList<>();
            parseBuffer(inputLines, outputLines, variableKeyValues);
            sendEmail(recipientsWithoutOnlineAccess, outputLines);
        }

        if(recipientsWithOnlineAccess.size() > 0 ){
            variableKeyValues.put("url", reportsUrl);
            outputLines = new ArrayList<>();
            parseBuffer(inputLines, outputLines, variableKeyValues);
            sendEmail(recipientsWithOnlineAccess, outputLines);
        }
    }

    /**
     * Send report email to recipients
     * 
     * @param recipientsList
     *  List of email addresses.
     * @param htmlOutput
     *  Generated report HTML output.
     */
    void sendEmail(List<String> recipientsList, List<StringBuilder> htmlOutput){
        StringBuilder messageHtml = new StringBuilder();
        for(StringBuilder s: htmlOutput){
            if(s.toString().trim().isEmpty()){
                continue;
            }
            messageHtml.append(s.toString().trim() + "\n");
        }
        messageHtml.append("\n\n");

        String[] recipients = new String[recipientsList.size()];
        recipients = recipientsList.toArray(recipients);

        String hostName = UvmContextFactory.context().networkManager().getNetworkSettings().getHostName();
        String domainName = UvmContextFactory.context().networkManager().getNetworkSettings().getDomainName();
        String fullName = hostName + (  domainName == null ? "" : ("."+domainName));

        String subject = getVariable(new selector("title")).toString() + " [" + fullName + "]";

        List<Map<MailSender.MessagePartsField,String>> mp = new ArrayList<>();
        for(int i = 0; i < messageParts.size(); i++ ){
            mp.add(messageParts.get(i));
        }

        Map<MailSender.MessagePartsField,String> part = new HashMap<>();
        part.put(MailSender.MessagePartsField.TEXT, messageText.toString());
        mp.add(part);
        part = new HashMap<>();
        part.put(MailSender.MessagePartsField.HTML, messageHtml.toString());
        mp.add(part);

        UvmContextFactory.context().mailSender().sendMessage(recipients, subject, mp);

    }

    /**
     * Process the current buffer in a new context instance.
     *
     * @param inputLines
     *  List of input to process.
     * @param outputLines
     *  List of processed lines.
     * @param variableKeyValues
     *  Variables and their values to process (add to context)
     */
    void parseBuffer(List<StringBuilder> inputLines, List<StringBuilder> outputLines, Map<String,Object> variableKeyValues){
        parseContextStack = new ArrayList<>();
        parseContext context = new parseContext();
        parseContextStack.add(context);

        for(Map.Entry<String, Object> variable : variableKeyValues.entrySet()) {
            context.addVariable(Tag._SYSTEM, variable.getKey(), variable.getValue());
        }
 
        for( StringBuilder line : inputLines ){
            currentInputLine = line;
            currentOutputLine = new StringBuilder();
            parse(line.toString());
            outputLines.add(currentOutputLine);
        }
    }

    /**
     * Process buffer within current context stack.
     * 
     * @param buffer
     *  Buffer to process.
     */
    void parse(String buffer)
    {
        int contextIndex = parseContextStack.size() - 1;

        parseContext parseContext = parseContextStack.get(contextIndex);

        Matcher tag = null;
        String search = null;
        String replace = null;
        Tag key;
        for(String line : buffer.split("\\n")){
            parseContext.ignoreLine = false;

            /*
             * Perform translations
             */
            for(Map.Entry<Tag, Pattern> syntax : TagPatterns.entrySet()) {
                if(syntax.getKey() == Tag.TRANS){
                    tag = syntax.getValue().matcher(line);

                    if( parseContext.getInComment()){
                        continue;
                    }

                    while(tag.find()){
                        StringBuilder newLine = new StringBuilder();
                        newLine.append(line.substring(0,line.indexOf(tag.group())));
                        newLine.append(i18nUtil.tr(tag.group(1).trim()));
                        newLine.append(line.substring(line.indexOf(tag.group()) + tag.group().length()) + "\n");
                        line = newLine.toString();
                    }
                }
            }

            /*
             * Parse syntax
             */
            for(Map.Entry<Tag, Pattern> syntax : TagPatterns.entrySet()) {
                try{
                    tag = syntax.getValue().matcher(line);
                    while( tag.find()){
                        key = syntax.getKey();

                        if((key != Tag.COMMENT_END) &&
                            parseContext.getInComment()){
                            parseContext.ignoreLine = true;
                            continue;
                        }

                        switch(syntax.getKey()){
                            case VARIABLE:
                                if(parseContext.allowOutput && 
                                    parseContext.ignoreLine == false && 
                                    parseContext.buildLoopBuffer == false && 
                                    isVariableParseActive(tag.group(1).trim()) != false){
                                    try{
                                        currentOutputLine.append(line.substring(0,line.indexOf(tag.group())));
                                        insertVariable(line, new selector(tag.group(1).trim()));
                                        currentOutputLine.append(line.substring(line.indexOf(tag.group()) + tag.group().length()));
                                    }catch(Exception e){
                                        logger.warn("parse: Unable to insert variable:" + tag.group(1).trim() );
                                    }
                                    parseContext.ignoreLine = true;
                                }
                                break;
                            case FOR:
                                if( parseContext.buildLoopBuffer == true){
                                    parseContext.loopsSeen++;
                                }else{
                                    parseContext.buildLoopBuffer = true;
                                    parseContext.ignoreLine = true;
                                    parseContext.addVariable(Tag.FOR, tag.group(1), getVariable(new selector(tag.group(2))));
                                }
                                break;
                            case ENDFOR:
                                if( parseContext.loopsSeen > 0 ){
                                    parseContext.loopsSeen--;
                                }else{
                                    parseContext.ignoreLine = true;
                                    parseContext.buildLoopBuffer = false;
                                
                                    variableContext vc = parseContext.getVariableContext(Tag.FOR);
                                    int collectionSize = (vc.object == null)  ? 0 : ((List) vc.object).size();
                                    for(int i = 0; i < collectionSize; i++){
                                        vc.index = i;

                                        parseContext newContext = new parseContext();
                                        parseContextStack.add(newContext);
                                        parse(parseContext.loopBuffer.toString());

                                        parseContextStack.remove(parseContextStack.size()-1);
                                    }
                                    parseContext.loopBuffer = null;
                                    parseContext.loopBuffer = new StringBuilder();
                                    parseContext.removeVariable(Tag.FOR);
                                }

                                break;
                            case IF:
                                if( parseContext.buildLoopBuffer == false){
                                    parseContext.pushConditional(parseCondition(tag.group(1)));
                                    if(getParseContextStackMatch(null) == false){
                                        parseContext.allowOutput = getParseContextStackMatch(true);
                                        parseContext.ignoreLine = true;
                                    }
                                }
                                break;
                            case ELSE:
                                if( parseContext.buildLoopBuffer == false){
                                    if(getParseContextStackMatch(null) == false){
                                        parseContext.conditionals.get(parseContext.conditionals.size()-1).match = !parseContext.conditionals.get(parseContext.conditionals.size()-1).match;
                                        parseContext.allowOutput = getParseContextStackMatch(true);
                                        parseContext.ignoreLine = true;
                                    }
                                }
                                break;
                            case ENDIF:
                                if( parseContext.buildLoopBuffer == false){
                                    if(getParseContextStackMatch(null) == false){
                                        parseContext.ignoreLine = true;
                                        parseContext.popConditional();
                                        parseContext.allowOutput = getParseContextStackMatch(true);
                                    }else{
                                        parseContext.popConditional();                                        
                                    }
                                }
                                break;

                            case WITH:
                                if( parseContext.buildLoopBuffer == false && parseContext.allowOutput){
                                    parseContext.addVariable(Tag.WITH, tag.group(1), getVariable(new selector(tag.group(2))));
                                    parseContext.ignoreLine = true;
                                }
                                break;

                            case ENDWITH:
                                if( parseContext.buildLoopBuffer == false){
                                    parseContext.removeVariable(Tag.WITH);
                                    parseContext.ignoreLine = true;
                                }
                                break;

                            case CYCLE_INITIALIZE:
                                if(parseContext.buildLoopBuffer == false){
                                    parseContext.ignoreLine = true;
                                    insertVariableCycle(tag);
                                }
                                break;

                            case CYCLE_NEXT:
                                if(parseContext.buildLoopBuffer == false && parseContext.allowOutput){
                                    parseContext.ignoreLine = true;
                                    nextVariableCycle(tag);
                                }
                                break;

                            case COMMENT_BEGIN:
                                parseContext.setInComment(true);
                                parseContext.ignoreLine = true;
                                break;

                            case COMMENT_END:
                                parseContext.setInComment(false);
                                parseContext.ignoreLine = true;
                                break;
                        }
                    }
                }catch(Exception e){
                    logger.warn("Cannot process tag [" + tag + "]: Exception: ",e);
                }
            }
            if(parseContext.allowOutput && 
                parseContext.ignoreLine == false){
                if(parseContext.buildLoopBuffer){
                    parseContext.addToBuffer(line + "\n");
                }else{
                    if(!parseContext.getInComment()){
                        currentOutputLine.append(line);
                    }
                }
            }
        }
    }

    /**
     * Determine if variable is active in this pass.
     * If defined and not in this pass, return false. Return true otherwise.
     *
     * @param name
     *  Name of variable to find.
     * @return
     *  true if variable is active, false otherwise.
     */
    private Boolean isVariableParseActive(String name){
        Boolean active = true;
        for(Map.Entry<ParsePass, String> ParsePassVariable : ParsePassActiveVariables.entrySet()) {
            if(ParsePassVariable.getValue().equals(name)){
                active = (ParsePassVariable.getKey() == currentParsePass);
            }
        }
        return active;
    }

    /**
     * Proces conditional for IF statements
     * 
     * @param condition
     *  Condition to break down.
     * @return
     *  true if condition matched, false otherwise.
     */
    // TODO: !!! ?? try to merge code with filter conditional
    private Boolean parseCondition(String condition)
    {
        Boolean match = false;

        String left;
        Boolean negation;
        String operation;
        String right;

        // if(condition.indexOf("uniqueId") > -1){
        //     logger.warn("parseCondition, condition="+ condition);
        // }

        Matcher tags = null;
        int startPosition = 0;
        Boolean tagFound = false;
        for(Map.Entry<ConditionalE, Pattern> syntax : ConditionalPatterns.entrySet()) {
            tags = syntax.getValue().matcher(condition);
            if(tags.find(startPosition) == true){
                tagFound = true;
                startPosition = tags.end();
                left = tags.group(1).trim();
                negation = (tags.group(2).trim().isEmpty() == false);
                operation = tags.group(3).trim();
                right = tags.group(4).trim();
                // if(condition.indexOf("uniqueId") > -1){
                //     logger.warn("parseCondition, left=[" + left + "], negation=[" + negation + "], operation=[" + operation + "], right=[" + right + "]");
                // }
                switch(syntax.getKey()){
                    case LOGICAL:
                        Boolean leftLogicalMatch = true;
                        Boolean rightLogicalMatch = true;

                        leftLogicalMatch = parseCondition(left);
                        rightLogicalMatch = parseCondition(right);
                        // if(condition.indexOf("uniqueId") > -1){
                        //     logger.warn("parseConditional, logical: leftConditionalMatch=" + leftLogicalMatch + ", rightConditionalMatch=" + rightLogicalMatch);
                        // }

                        if(operation.equals("and")){
                            match = (leftLogicalMatch && rightLogicalMatch);
                        }else if(operation.equals("or")){
                            match = (leftLogicalMatch || rightLogicalMatch);
                        }
                        break;

                    case EQUALITY:
                        // if(condition.indexOf("uniqueId") > -1){
                        //     logger.warn("parseCondition, conditional: left=[" + left + "], negation=["+negation+"], operation=[" + operation + "], right=[" + right + "]");
                        // }

                        if(right.equals("\"\"")){
                            /* Empty string */
                            right = "";
                        }else if(
                            (right.length() > 3) &&
                            (right.charAt(0) == '"') && 
                            (right.charAt(right.length()-1) == '"') ){
                            /* Quoted string */
                            right = right.substring(1,right.length() -1);
                        }

                        List<String> fields = new ArrayList<>(Arrays.asList(left.split("\\.")));
                        fields.remove(0);
                        if(isVariableParseActive(left) == false){
                            // Variable non-active for this pass.  It's proper to short circult everything here.
                            return null;
                        }
                        Object leftVariable = getVariable(new selector(left));
                        Object rightVariable = getVariable(new selector(right));
                        // logger.warn("parseConditional: left=" + left + ", leftVariable=" + leftVariable);

                        if(leftVariable != null){
                            if(leftVariable.getClass().isEnum()){
                                // All enums will be string comparisions
                                leftVariable = leftVariable.toString();
                            }
                            if( operation.equals("==") || operation.equals("=") ){
                                // !!! is this comparision hokey/bad form?
                                if(leftVariable.getClass().getName().equals("java.lang.Boolean")){
                                    if((Boolean) leftVariable.equals(Boolean.valueOf(right))){
                                        match = true;
                                    }
                                }else{
                                    if(rightVariable != null && leftVariable.toString().equals(rightVariable)){
                                        match = true;
                                    }else if(leftVariable.toString().equals(right)){
                                        match = true;
                                    }
                                    // logger.warn("parseConditional: leftVariable=" + leftVariable + ", right=" + right + ", match=" +match );

                                // !!! Also non-equality numeric checks.
                                }
                            }else if(operation.equals("in")){
                                if(((List) rightVariable).indexOf((String) leftVariable) > -1){
                                    match = true;
                                }
                            }else if(operation.equals("<")){
                                if(rightVariable == null){
                                    rightVariable = right;
                                }
                                if(Integer.parseInt((String)leftVariable) < Integer.parseInt((String)rightVariable)){
                                    match = true;
                                }
                                // logger.warn("parseCondition: ["+condition+"] leftVariable=" + leftVariable + ", rightVariable=" + rightVariable + ", match=" + match);
                            }else if(operation.equals(">")){
                                if((Integer) rightVariable > (Integer) leftVariable){
                                    match = true;
                                }
                            }
                        }
                        break;

                }
                if(negation){
                    match = !match;
                }
            }
            if(startPosition == condition.length()){
                break;
            }
        }
        if(tagFound == false){
            logger.warn("parseConditional: Unknown conditional syntax ["+condition+", startPosition="+startPosition+"]");
        }

        // if(condition.indexOf("uniqueId") > -1){
        //     logger.warn("parseCondition, match=" + match);
        // }
        return match;
    }

    /**
     * Add variables as buffered writes.  
     * Most variables are single string so this may seem like overkill but others like files 
     * are too big to keep in memory.
     *
     * @param line
     *  Line containing variable.
     * @param variableSelector
     *  Variable selector to add.
     */
    private void insertVariable(String line, selector variableSelector)
    {
        if(variableSelector.fields.get(0).equals("attachment")){
            insertVariableAttachment(variableSelector);
        }else{
            Object variable = getVariable(variableSelector);
            try{
                currentOutputLine.append(variable.toString());
            }catch(Exception e){
                logger.warn("insertVariable: Unable to insert variable:" + variableSelector );
            }
        }
    }

    /**
     * Add a file to the current location as-is.
     *
     * @param variableSelector
     *  Selector for the variableobject.
     */
    private void insertVariableAttachment(selector variableSelector)
    {
        Boolean base64 = false;
        String id = null;

        for(String filter: variableSelector.filters){
            if(filter.startsWith("id=")){
                int separator = filter.indexOf("=");
                if(separator != -1){
                    id = filter.substring(separator + 1);
                }
            }
        }

        String filename = variableSelector.arguments.get(0);

        if(variableSelector.arguments.get(0).equals("chart")){
            filename = getChart(getVariable(new selector(variableSelector.arguments.get(1))), id);
        }

        File f = new File(filename);
        if(f.exists() == false){
            logger.warn("insertVariableAttachment: Could not find file " + filename);
            return;
        }

        Boolean duplicate = false;            
        for(int i = 0; i < messageParts.size(); i++ ){
            if(messageParts.get(i).get(MailSender.MessagePartsField.FILENAME).equals(filename)){
                duplicate = true;
            }
        }
        if( duplicate == false){
            Map<MailSender.MessagePartsField,String> attachment = new HashMap<>();
            attachment.put(MailSender.MessagePartsField.FILENAME, filename);
            if(id != null){
                attachment.put(MailSender.MessagePartsField.CID, id);
                currentOutputLine.append("cid:" + id);
            }
            messageParts.add(attachment);
        }
    }

    /**
     * Add new cycle variable to current context
     *
     * @param argumentValues
     *  Matcher to process.
     */
    private void insertVariableCycle(Matcher argumentValues)
    {
        ArrayList<String> values = null;
        if(argumentValues.group(1).indexOf("...") > -1){
            ArrayList<String> startEnd = new ArrayList<>(Arrays.asList(argumentValues.group(1).split("\\.\\.\\.")));
            values = new ArrayList<>(Integer.parseInt(startEnd.get(1)) - Integer.parseInt(startEnd.get(0)));
            for(Integer i = Integer.parseInt(startEnd.get(0)); i < Integer.parseInt(startEnd.get(1)); i++){
                values.add(Integer.toString(i));
            }
        }else{
            values = new ArrayList<>(Arrays.asList(argumentValues.group(1).split("\\s")));
        }
        String variableName = argumentValues.group(2);

        int contextIndex = parseContextStack.size() - 1;
        parseContext parseContext = parseContextStack.get(contextIndex);
        parseContext.addVariable(Tag._CYCLE, variableName, values);

        variableContext vc = parseContext.getVariableContext(Tag._CYCLE, variableName);
        vc.index = 0;
    }

    /**
     * Look for cycle variable in context stack and if found, loop
     * 
     * @param argumentValues
     *  Matcher to process.
     */
    private void nextVariableCycle(Matcher argumentValues)
    {
        for(int contextIndex = parseContextStack.size() - 1; contextIndex > -1; contextIndex--){
            String variableName = argumentValues.group(1);
            // int contextIndex = parseContextStack.size() - 1;
            parseContext parseContext = parseContextStack.get(contextIndex);

            variableContext vc = parseContext.getVariableContext(Tag._CYCLE, variableName);
            if( vc != null ){
                if(vc.index < (((List) vc.object).size() - 1 )){
                    vc.index++;
                }else{
                    vc.index = 0;
                }
            }
        }
    }

    /**
     * Get a variable from its selector.  
     * This will also recurse to pull arguments into itself.
     *
     * @param variableSelector
     *  Find the specified variable.
     * @return
     *  Object value of the variable.
     */
    private Object getVariable(selector variableSelector)
    {
        Method method = null;
        Object object = null;
        Class<?>[] argumentTypes = null;
        Object[] argumentValues = null;

        // if(variableSelector.fields.get(0).isEmpty()){
        //     return null;
        // }

        if(!variableSelector.fields.get(0).isEmpty() &&
            variableSelector.fields.get(0).charAt(0) == '[' &&
            variableSelector.fields.get(0).charAt(variableSelector.fields.get(0).length() - 1) == ']' ){
            /* 
             * Create arbitary list variable.
             */
            return createVariableList(variableSelector.fields.get(0));
        }

        /*
         * Look at the first selector field to determine of an object should be pulled from the VM
         * or context stack.
         */ 
        object = (Object) UvmContextFactory.context();
        int fieldIndex = 0;
        try{
            method = object.getClass().getMethod(variableSelector.fields.get(0));
        }catch(java.lang.NoSuchMethodException e){
            object = null;
            for( int i = parseContextStack.size() - 1; i >= 0; i--){
                object = parseContextStack.get(i).getVariable(variableSelector.fields.get(0));
                if(object != null){
                    break;
                }
            }
        }

        /*
         * Walk the field list on the current object.
         */
        for(; fieldIndex < variableSelector.fields.size(); fieldIndex++){
            /*
             * If about to try final field, process arguments.
             */
            if((fieldIndex == variableSelector.fields.size() - 1) &&
                variableSelector.arguments.size() > 0){
                argumentTypes = null;
                argumentValues = null;
                /*
                 * Get the method's argument type list
                 */
                if(object == null){
                    continue;
                }
                for(Method m: object.getClass().getMethods()){
                    if(variableSelector.fields.get(fieldIndex).equals(m.getName()) &&
                        (m.getParameterTypes().length == variableSelector.arguments.size())){

                        /* Also check that argument types match as best we can. */
                        argumentTypes = m.getParameterTypes();
                        Boolean methodMatch = true;
                        for(int a = 0; a < variableSelector.arguments.size(); a++){
                            Matcher matcher = NumericOnlyPattern.matcher(variableSelector.arguments.get(a));
                            if((matcher.matches() == false) && 
                                (argumentTypes[a].getName().equals("java.lang.Integer") ||
                                 argumentTypes[a].getName().equals("java.lang.Long"))){
                                methodMatch = false;
                                break;
                            }
                        }
                        if(methodMatch == true){
                            break;
                        }
                    }
                }

                /* Not found in VM so create new one for context stack processing*/
                if(argumentTypes == null){
                    argumentTypes = new Class<?>[variableSelector.arguments.size()];
                }

                argumentValues = new Object[variableSelector.arguments.size()];

                /*
                 * Build companion argument value list
                 */
                int argumentIndex = 0;
                for(String argument: variableSelector.arguments){
                    if(argumentTypes[argumentIndex] == null){
                        /* For non VM objects, set argument type to string. */
                        argumentTypes[argumentIndex] = String.class;
                    }

                    /*
                     * Recurse ourself to see if this argument refers to another context variable.
                     */
                    Object argumentValue = getVariable(new selector(variableSelector.arguments.get(argumentIndex)));
                    if(argumentValue != null){
                        argumentValues[argumentIndex] = argumentValue;
                    }else{
                        /*
                         * Otherwise process variable as-is and try to coerse to match the type.
                         */
                        // argumentValue = variableSelector.arguments.get(argumentIndex);
                        argumentValue = variableSelector.arguments.get(argumentIndex);
                        if(argumentValue.equals("null")){
                            argumentValues[argumentIndex] = null;
                        }else if(argumentValue.getClass() != argumentTypes[argumentIndex]){
                            if(argumentTypes[argumentIndex].getName().equals("int")){
                                argumentValues[argumentIndex] = Integer.valueOf((String)argumentValue);
                            }
                        }else{
                            argumentValues[argumentIndex] = argumentValue;
                        }
                    }
                    argumentIndex++;                    
                }
            }

            /*
             * Call into VM object path, otherwise back into context stack.
             */
            try{
                if(argumentTypes == null){
                    method = object.getClass().getMethod(variableSelector.fields.get(fieldIndex));
                    object = method.invoke(object);
                }else{
                    method = object.getClass().getMethod(variableSelector.fields.get(fieldIndex),argumentTypes);
                    object = method.invoke(object, argumentValues);
                }
            }catch(java.lang.NoSuchMethodException e){
                for( int c = parseContextStack.size() - 1; c >= 0; c--){
                    object = parseContextStack.get(c).getVariable(variableSelector.fields.get(0));
                    if(object != null){
                        break;
                    }
                }
            }catch(java.lang.NullPointerException e){
                /*
                 * this is ok because you asked for a null....
                 */
            }catch(Exception e){
                logger.warn("Unable to get variable: " + variableSelector );
            }
        }

        /*
         * If selector has defined variables, process them
         */
        if(object != null &&
           variableSelector.filters.size() > 0){
            if(object.getClass().getName().contains(".LinkedList") || 
                object.getClass().getName().contains(".ArrayList")){

                for(int i = ((List) object).size() -1; i >= 0; i--){
                    if(!filterMatch(((List)object).get(i), variableSelector.filters)){
                        ((List) object).remove(i);
                    }
                }

                object = filterProcess(object, variableSelector.filters);
            }else if(object.getClass().getName().contains(".String")){
                object = filterProcess(object,variableSelector.filters);
            }

            // !!! how to tell unknown filters?
        }

        return object;
    }

    /**
     * Create arbitrary string-based list from stringList specifier which supports
     * following string formats:
     * "quote" unquoted "quoted with spaces"
     *
     * @param stringList
     *  String to process.
     * @return
     *  Variable.
     */
    private Object createVariableList(String stringList){
        List<String> variableList = new ArrayList<>();

        stringList = stringList.substring(1,stringList.length()-1).trim();
        for(String element: stringList.split("\\s+")){
            if(element.charAt(0) == '"' && element.charAt(element.length()-1) == '"'){
                element = element.substring(1,element.length()-1);
                variableList.add(element);
            }else if(element.charAt(0) == '"' && element.charAt(element.length()-1) != '"'){
                element = element.substring(1);
                variableList.add(element);
            }else if(element.charAt(0) != '"' && element.charAt(element.length()-1) == '"'){
                element = element.substring(0,element.length() -1);
                // Hackish.  Should preserve actual number of spaces
                int lastIndex = variableList.size() - 1;
                variableList.set(lastIndex, variableList.get(lastIndex) + " " +  element);
            }else{
                variableList.add(element);
            }
        }

        return variableList;
    }

    /**
     * Similar to condtional except use object methods for comparison.
     * (e.g., "getType=TEXT" to only pull text reports)
     *
     * @param object
     *  Object to match via filters.
     * @param filters
     *  String list of filters.
     * @return
     *  true if match, false if not.
     */
    Boolean filterMatch(Object object, List<String> filters){
        if(filters.size() == 0){
            return true;
        }

        Boolean match = false;
        Method method = null;
        Object tObject;

        String left;
        String operation;
        String right;

        Boolean filterMatchFound = false;
        Matcher tag;
        for(String filter: filters){
            tag = Conditional.matcher(filter);

            while( tag.find()){
                filterMatchFound = true;
                left = tag.group(1);
                operation = tag.group(2);
                right = tag.group(3);

                try{
                    method = object.getClass().getMethod(left);
                    tObject = method.invoke(object);

                    if(operation.equals("==")){
                        if(tObject.toString().equals(right)){
                            match = true;
                        }
                    }
                    // !!! Other operations...
                }catch(Exception e){
                    logger.warn("Unable to process filter match:" + e);
                }
            }

        }
        if(filterMatchFound == false){
            return true;
        }
        return match;
    }

    /**
     * Modify the object.
     * 
     * @param object
     *  Object to modify.
     * @param filters
     *  Filters to modify with.
     * @return
     *  Object of filtered result
     */
    Object filterProcess(Object object, List<String> filters){
        Matcher filterMatcher;
        for(String filter: filters){
            for (Map.Entry<Filter, Pattern> syntax : FilterPatterns.entrySet()) {
                try{
                    filterMatcher = syntax.getValue().matcher(filter);
                    while( filterMatcher.find()){
                        switch(syntax.getKey()){
                            case FIRST:
                                object = ((List) object).get(0);
                                break;
                            case DISTINCT:
                                object = filterProcessDistinct(object, new selector(filterMatcher.group(1)));
                                break;
                            case FORMAT:
                                object = filterProcessFormat(object, (JSONObject) getVariable(new selector(filterMatcher.group(1))), getVariable(new selector(filterMatcher.group(2))));
                                break;
                            case IN:
                                object = filterProcessIn(object, new selector(filterMatcher.group(1)), getVariable(new selector(filterMatcher.group(2))));
                                break;
                            case ORDER:
                                object = filterProcessOrder(object, new selector(filterMatcher.group(1)), new selector(filterMatcher.group(2)));
                                break;
                        }
                    }
                }catch(Exception e){
                    logger.warn("Unable to process filter:" + e);
                }
            }
        }
        return object;
    }

    /**
     * Process a template through an argument list
     * 
     * Additionally, order results according to sortOrder list.
     * Results are expected to be in JSONObject format and sortOrder expected to be
     * in String[] format.
     *
     * Basically used to sort report result list in textColumn format since results
     * are not guaranteed to be in order.  Attempt to look at last word in each
     * entry, expecting the format to be in SQL format to name column like "select ... as resultName"
     *
     * @param template
     *  Template to process.
     * @param arguments
     *  Arguments on the object.
     * @param sortOrder
     *  Sort order.
     * @return
     *  Filtered object.
     */
    Object filterProcessFormat(Object template, JSONObject arguments, Object sortOrder){
        /*
         * It's not a guaranteee that the arguments list will match the number of
         * template arguments (e.g.,.SQL will leave fields blank instead of empty unless
         * extra processing is done).  Determine how many we'll need and after building
         * replacement map, add the extras we need with known defaults for reports ("0")
         */
        int maximumTemplateArguments = 0;
        int argumentIndex = 0;
        while(true){
            if(((String)template).indexOf("{" + Integer.toString(maximumTemplateArguments) + "}") == -1){
                break;
            }
            maximumTemplateArguments++;
        }

        Map<String,String> replacements = new HashMap<>();

        ArrayList<String> sortOrderList = null;
        if(sortOrder.getClass().isArray()){
            sortOrderList = new ArrayList<>(Arrays.asList((String[]) sortOrder));
        }
        String orderName = null;
        int lastSpaceIndex = -1;
        int resultsNameIndex;
        for(int i = 0; i < sortOrderList.size(); i++){
            orderName = sortOrderList.get(i);
            lastSpaceIndex= orderName.lastIndexOf(" ");
            if(lastSpaceIndex != -1){
                orderName = orderName.substring(lastSpaceIndex + 1);
            }
            if(arguments.names() != null){
                for( argumentIndex = 0; argumentIndex < arguments.names().length(); argumentIndex++){
                    try{
                        if(arguments.names().getString(argumentIndex).equals(orderName)){
                            replacements.put("{" + Integer.toString(i) + "}", arguments.get(arguments.names().getString(argumentIndex)).toString() );
                        }
                    }catch(Exception e){
                        logger.warn("Unable to process argument entry " + Integer.toString(argumentIndex) + " :" + e);
                    }
                }
            }
        }

        while(argumentIndex < maximumTemplateArguments){
            replacements.put("{" + Integer.toString(argumentIndex) + "}", "0" );
            argumentIndex++;
        }

        String formatted = (String) template;
        for (Map.Entry<String,String> sr : replacements.entrySet()) {
            formatted = formatted.replace(sr.getKey(), sr.getValue());
        }

        return (Object) formatted;
    }

    /**
     * Process the list and filter out duplicates.
     *
     * Simplisitic in string comparisions are expected and no method arguments are allowed.
     * Use case is to eliminate apps with the same name (e.g.,multiple policies with same app)
     *
     * @param incomings
     *  Incoming to filter.
     * @param filterSelector
     *  Filter selector.
     * @return
     *  Filtered object.
     */
    Object filterProcessDistinct(Object incomings, selector filterSelector){
        List<Object> outgoings = new ArrayList<>();
        List<Object> seens = new ArrayList<>();

        Method method = null;
        Object object = null;

        int fieldIndex;
        boolean filterSeen;
        for(int i = 0; i < ((List) incomings).size(); i++){
            filterSeen = false;
            object = ((List) incomings).get(i);
            for(fieldIndex = 0; fieldIndex < filterSelector.fields.size(); fieldIndex++){
                try{
                    /* No arguments allowed at this time. */
                    method = object.getClass().getMethod(filterSelector.fields.get(fieldIndex));
                    object = method.invoke(object);
                }catch(Exception e){
                    logger.warn("Unable to get variable: " + filterSelector );
                    break;
                }
            }
            for(Object seen : seens){
                if(seen.toString().equals(object.toString())){
                    filterSeen = true;
                }
            }
            if( filterSeen == false){
                seens.add(object);
                outgoings.add(((List) incomings).get(i));
            }

        }

        return (Object) outgoings;
    }

    /**
     * Process the list and include only those in the specified list.
     *
     * One use for this is to pull only reports within the allowed report type list.
     *
     * @param incomings
     *  Incoming to filter.
     * @param filterSelector
     *  Filter selector.
     * @param checklist
     *  Checklist to include.
     * @return
     *  Filtered object.
     */
    Object filterProcessIn(Object incomings, selector filterSelector, Object checklist){
        List<Object> outgoings = new ArrayList<>();

        Method method = null;
        Object object = null;

        int fieldIndex;
        for(int i = 0; i < ((List) incomings).size(); i++){
            object = ((List) incomings).get(i);
            for(fieldIndex = 0; fieldIndex < filterSelector.fields.size(); fieldIndex++){
                try{
                    /* No arguments allowed at this time. */
                    method = object.getClass().getMethod(filterSelector.fields.get(fieldIndex));
                    object = method.invoke(object);
                }catch(Exception e){
                    logger.warn("Unable to get variable: " + filterSelector );
                    break;
                }
            }
            if( ((List) checklist).indexOf(object.toString()) > -1 ){
                outgoings.add(((List) incomings).get(i));
            }
        }

        return (Object) outgoings;
    }

    /**
     * Process the list to order by the specified field.
     *
     * One use is to sort all reports so that TEXT types are first.
     * Another it to sort by category order
     *
     * @param incomings
     *  Incoming to filter.
     * @param filterSelector
     *  Filter selector.
     * @param orderSelector
     *  Order to use.
     * @return
     *  Filtered object.
     */
    @SuppressWarnings("unchecked")
    Object filterProcessOrder(Object incomings, selector filterSelector, selector orderSelector){
        ArrayList<?> orderList = (ArrayList<?>) getVariable(orderSelector);
        if(orderList == null){
            orderList = new ArrayList<>();
            ((ArrayList<String>) orderList).add(orderSelector.fields.get(0));
        }
        Collections.sort((ArrayList<String>)orderList);

        LinkedHashMap<String,ArrayList<Object>> sortedOutgoings = new LinkedHashMap<>();
        for(String orderString: (ArrayList<String>) orderList){
            sortedOutgoings.put(orderString, new ArrayList<Object>());
        }

        List<Object> otherOutgoings = new ArrayList<>();

        Method method = null;
        Object object = null;

        int fieldIndex;
        Boolean sortedMatch = false;
        String orderKey;
        ArrayList<Object> orderedList;
        for(int i = 0; i < ((List) incomings).size(); i++){
            object = ((List) incomings).get(i);
            for(fieldIndex = 0; fieldIndex < filterSelector.fields.size(); fieldIndex++){
                try{
                    /* No arguments allowed at this time. */
                    method = object.getClass().getMethod(filterSelector.fields.get(fieldIndex));
                    object = method.invoke(object);
                }catch(Exception e){
                    logger.warn("Unable to get variable: " + filterSelector );
                    break;
                }
            }

            sortedMatch = false;
            for(Map.Entry<String,ArrayList<Object>> entry : sortedOutgoings.entrySet()){
                orderKey = entry.getKey();
                if(orderKey.equals(object.toString())){
                    sortedMatch = true;
                    orderedList = entry.getValue();
                    orderedList.add(((List) incomings).get(i));
                    break;
                }
            }
            if(sortedMatch == false){
                otherOutgoings.add(((List) incomings).get(i));
            }
        }

        List<Object> outgoings = new ArrayList<>();
        for(Map.Entry<String,ArrayList<Object>> entry : sortedOutgoings.entrySet()){
            if(entry.getValue().size() > 0){
                outgoings.addAll(entry.getValue());
            }
        }
        if(otherOutgoings.size() > 0){
            outgoings.addAll(otherOutgoings);
        }
        return (Object) outgoings;
    }

    /**
     * Perform query with WebBrowser against specialized reports mode
     * and return saved filename.
     *
     * @param reportUniqueId
     *  ReportId to create.
     * @param id
     *  Unique HTML identifier for the image.
     * @return
     *  Filename from WebBrowser.
     */
    String getChart(Object reportUniqueId, String id){
        String filename;
        if(id != null){
            filename = webbrowser.getTempDirectory() + "/" + id + ".png";
        }else{
            try{
                filename = webbrowser.getTempDirectory() + "/" + URLEncoder.encode((String) reportUniqueId, "UTF-8") + ".png";
            }catch(Exception e){
                filename = webbrowser.getTempDirectory() + "/image.png";
            }
        }

        try {
            String url = "http://127.0.0.1/reports/?reportChart=1" +
                "&reportUniqueId=" + URLEncoder.encode((String) reportUniqueId, "UTF-8") +
                "&startDate=" + URLEncoder.encode(Long.toString(startDate.getTime()), "UTF-8") +
                "&endDate=" + URLEncoder.encode(Long.toString(endDate.getTime()), "UTF-8");
            webbrowser.openUrl(url);
            webbrowser.waitForElement(WebBrowser.FIND_KEYS.CLASS, "highcharts-legend", 120);
            Thread.sleep(1000);
            webbrowser.takeScreenshot(filename);
        } catch (org.openqa.selenium.TimeoutException e) {
            webbrowser.waitForElement(WebBrowser.FIND_KEYS.CLASS, "highcharts-no-data", 120);
            webbrowser.takeScreenshot(filename);
        } catch (Exception e) {
            logger.warn("Exception",e);
            webbrowser.takeScreenshot(filename);
        }

        webbrowser.clearCache();

        return filename;
    }
}
