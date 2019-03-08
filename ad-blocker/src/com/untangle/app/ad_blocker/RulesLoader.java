/**
 * $Id$
 */
package com.untangle.app.ad_blocker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.jabsorb.JSONSerializer;

import com.untangle.app.ad_blocker.cookies.CookieElement;
import com.untangle.uvm.app.GenericRule;

/**
 * A utility class to parse, create, and load the ad-blocker rules from the source files
 */
public class RulesLoader
{
    public static final String RULE_FILE = System.getProperty("uvm.lib.dir")
            + "/ad-blocker/adblock_easylist_2_0.txt";
    public static final String RULE_FILE_BACKUP = System.getProperty("uvm.lib.dir")
            + "/ad-blocker/adblock_easylist_2_0_backup.txt";
    private static final String COOKIE_LIST_GHOSTERY = System.getProperty("uvm.lib.dir")
            + "/ad-blocker/ghostery-lsos.json";
    private static final Logger logger = Logger.getLogger(RulesLoader.class);
    
    private static final String LAST_UPDATE_LINE = "! Last modified:";

    /**
     * RulesLoader can not be instantiated
     * Use the static methods only
     */
    private RulesLoader() {}

    /**
     * Read the easylist source files and load the rules into the provided settings
     * @param settings The settings object
     */
    static void loadRules(AdBlockerSettings settings)
    {
        HashMap<String,GenericRule> currentRulesMap = new HashMap<>();
        for ( GenericRule rule : settings.getRules() ) {
            currentRulesMap.put( rule.getString(), rule);
        }

        LinkedList<GenericRule> rules = new LinkedList<>();
        BufferedReader in = null;
        try {
            File file = new File(RULE_FILE);
            in = new BufferedReader(new FileReader(file));
            String line;
            boolean block = false;
            
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(LAST_UPDATE_LINE)){
                    settings.setLastUpdate(line.substring(LAST_UPDATE_LINE.length()));
                    continue;
                }
                // ignore comments and type options
                if (line.startsWith("!") || line.indexOf("$") >= 0 || line.indexOf("##") >= 0 || line.indexOf("#@#") >= 0 || line.startsWith("[")) {
                    continue;
                }
                String sig = null;
                boolean blocked = false;
                boolean enabled = true;

                // @@ means a pass rule
                if (line.startsWith("@@")) {
                    sig = line.replaceFirst("@@", "");
                    blocked = false;
                } else {
                    sig = line;
                    blocked = true;
                }

                // if rule is is current rules, use the current "enabled" and "blocked" flag in case the admin changed it
                GenericRule currentRule = currentRulesMap.get( sig );
                if ( currentRule != null ) {
                    blocked = currentRule.getBlocked();
                    enabled = currentRule.getEnabled();
                }

                GenericRule rule = new GenericRule( sig, enabled );
                rule.setBlocked( blocked );
                
                rules.add(rule);
            }
        } catch (IOException e) {
            logger.error("Unable to read ad blocking rules", e);
        } finally {
            if(in != null){
                try {
                    in.close();
                }catch(Exception e){
                    logger.error("Unable to close read ad blocking rules", e);
                }
            }
        }

        settings.setRules(rules);
    }

    /**
     * Read the ghostery cookie source files and load the rules into the provided settings
     * @param settings The settings object
     */
    static void loadCookieListGhostery(AdBlockerSettings settings)
    {
        List<GenericRule> cookieRules = new LinkedList<>();

        HashMap<String,GenericRule> currentCookiesMap = new HashMap<>();
        for ( GenericRule rule : settings.getCookies() ) {
            currentCookiesMap.put( rule.getString(), rule );
        }
        
        BufferedReader in = null;
        try {
            File file = new File(COOKIE_LIST_GHOSTERY);
            in = new BufferedReader(new FileReader(file));
            String line;
            while ((line = in.readLine()) != null) {
                line = line.replaceFirst("\\{", "{ \"javaClass\" : \"com.untangle.app.ad_blocker.cookies.CookieElement\", ");
                JSONSerializer serializer = new JSONSerializer();

                serializer.setFixupDuplicates(false);
                serializer.setMarshallNullAttributes(false);
                serializer.registerDefaultSerializers();

                CookieElement cookieElement = (CookieElement) serializer.fromJSON(line);
                String cookie = cookieElement.getPattern();
                /* ignore / at end if present */
                if (cookie.charAt(cookie.length() - 1) == '/')
                    cookie = cookie.substring(0, cookie.length() - 1);
                cookie = cookie.replaceAll("\\(", "");
                cookie = cookie.replaceAll("\\)", "");
                cookie = cookie.replaceAll("\\\\.", ".");
                StringTokenizer st = new StringTokenizer(cookie, "|");
                while (st.hasMoreTokens()) {
                    String c = st.nextToken().trim();
                    if (c.startsWith("."))
                        c = c.substring(1);

                    boolean enabled = true;

                    /* If a rule already exists with that value, use the "enabled" value from it */
                    GenericRule currentRule = currentCookiesMap.get(c);
                    if ( currentRule != null )
                        enabled = currentRule.getEnabled();
                    
                    cookieRules.add(new GenericRule(c, c, null, null, enabled));
                }
            }
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw new RuntimeException(e);
            logger.error("could not read cookie list: " + COOKIE_LIST_GHOSTERY, e);
        }finally{
            if(in != null){
                try{
                    in.close();
                }catch(Exception e){
                    logger.error("could not close cookie list: " + COOKIE_LIST_GHOSTERY, e);
                }
            }
        }
        settings.setCookies(cookieRules);
    }
}
