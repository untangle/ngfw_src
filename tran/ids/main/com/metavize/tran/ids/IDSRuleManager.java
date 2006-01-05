package com.metavize.tran.ids;

import java.util.regex.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.argon.SessionEndpoints;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.PortRange;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.MvvmContextFactory;

public class IDSRuleManager {

    public static final boolean TO_SERVER = true;
    public static final boolean TO_CLIENT = false;


    public static List<IDSVariable> immutableVariables = new ArrayList<IDSVariable>(); 
    static {
        immutableVariables.add(new IDSVariable("$EXTERNAL_NET",IDSStringParser.EXTERNAL_IP,"Magic EXTERNAL_NET token"));
        immutableVariables.add(new IDSVariable("$HOME_NET",IDSStringParser.HOME_IP,"Magic HOME_NET token"));
    }
    public static List<IDSVariable> defaultVariables = new ArrayList<IDSVariable>(); 
    static {
        defaultVariables.add(new IDSVariable("$HTTP_SERVERS", "$HOME_NET","Addresses of possible local HTTP servers"));
        defaultVariables.add(new IDSVariable("$HTTP_PORTS", "80","Port that HTTP servers run on"));
        defaultVariables.add(new IDSVariable("$SSH_PORTS", "22","Port that SSH servers run on"));
        defaultVariables.add(new IDSVariable("$SMTP_SERVERS", "$HOME_NET","Addresses of possible local SMTP servers"));
        defaultVariables.add(new IDSVariable("$TELNET_SERVERS", "$HOME_NET","Addresses of possible local telnet servers"));
        defaultVariables.add(new IDSVariable("$SQL_SERVERS", "!any","Addresses of local SQL servers"));
        defaultVariables.add(new IDSVariable("$ORACLE_PORTS", "1521","Port that Oracle servers run on"));
        defaultVariables.add(new IDSVariable("$AIM_SERVERS", "[64.12.24.0/24,64.12.25.0/24,64.12.26.14/24,64.12.28.0/24,64.12.29.0/24,64.12.161.0/24,64.12.163.0/24,205.188.5.0/24,205.188.9.0/24]","Addresses of possible AOL Instant Messaging servers"));
    }

    private List<IDSRuleHeader> knownHeaders = new ArrayList<IDSRuleHeader>();
    private Map<Long,IDSRule> knownRules = new HashMap<Long,IDSRule>();

    private static Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");

    private IDSDetectionEngine engine;

    private static final Logger logger = Logger.getLogger(IDSRuleManager.class);

    public IDSRuleManager(IDSDetectionEngine engine) {
        this.engine = engine;
    }

    public void onReconfigure() {
        for(IDSRule rule : knownRules.values()) {
            rule.remove(true);
        }
    }

    public void updateRule(IDSRule rule) throws ParseException {
        long id = rule.getKeyValue();
        IDSRule inMap = knownRules.get(id);
        if(inMap != null) {
            rule.remove(false);
            if(rule.getModified()) {
                //Delete previous rule
                IDSRuleHeader header = inMap.getHeader();
                header.removeSignature(inMap.getSignature());

                if(header.signatureListIsEmpty()) {
                    logger.info("removing header");
                    knownRules.remove(id);
                    knownHeaders.remove(header);
                }

                //Add the modified rule
                logger.debug("Adding modified rule");
                addRule(rule);
            }
        }

        else {
            logger.debug("Does not contain - adding");
            addRule(rule);
        }
        //remove all rules with remove == true
        rule.setModified(false);
    }

//    public boolean addRule(String rule, Long key) throws ParseException {
  //      IDSRule test = new IDSRule(rule,"Not set", "Not set");
    //    test.setLog(true);
      //  test.setKeyValue(key);
        //return addRule(test);
    //}

    public boolean addRule(IDSRule rule) throws ParseException {
        String ruleText = rule.getText();

        String noVarText = substituteVariables(ruleText);
        String ruleParts[] = IDSStringParser.parseRuleSplit(noVarText);

        IDSRuleHeader header = IDSStringParser.parseHeader(ruleParts[0], rule.getAction());
        if (header == null)
            throw new ParseException("Unable to parse header of rule " + ruleParts[0]);
        IDSRuleSignature signature = IDSStringParser.parseSignature(ruleParts[1], rule.getAction(), rule, false);

        signature.setToString(ruleParts[1]);

        if(!signature.remove() && !rule.disabled()) {
            for(IDSRuleHeader headerTmp : knownHeaders) {
                if(headerTmp.equals(header)) {
                    headerTmp.addSignature(signature);

                    rule.setHeader(headerTmp);
                    rule.setSignature(signature);
                    rule.setClassification(signature.getClassification());
                    rule.setURL(signature.getURL());
                    //logger.debug("add rule (known header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
                    knownRules.put(rule.getKeyValue(),rule);
                    return true;
                }
            }

            header.addSignature(signature);
            knownHeaders.add(header);

            rule.setHeader(header);
            rule.setSignature(signature);
            rule.setClassification(signature.getClassification());
            rule.setURL(signature.getURL());
            //logger.debug("add rule (new header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
            knownRules.put(rule.getKeyValue(),rule);
            return true;
        }
        //rule.setSignature(signature); //Update UI description
        return false;
    }

    // This is how a rule gets created
    public IDSRule createRule(String text, String category) {
        if(text == null || text.length() <= 0 || text.charAt(0) == '#') {
            logger.debug("Ignoring empty rule: " + text);
            return null;
        }

        // Take off the action.
        String action = null;
        int firstSpace = text.indexOf(' ');
        if (firstSpace >= 0 && firstSpace < text.length() - 1) {
            action = text.substring(0, firstSpace);
            text = text.substring(firstSpace + 1);
        }

        IDSRule rule = new IDSRule(text, category, "The signature failed to load");

        text = substituteVariables(text);
        try {
            String ruleParts[]          = IDSStringParser.parseRuleSplit(text);
            IDSRuleHeader header        = IDSStringParser.parseHeader(ruleParts[0], rule.getAction());
            if (header == null) {
                logger.debug("Ignoring rule with bad header: " + text);
                return null;
            }
            IDSRuleSignature signature  = IDSStringParser.parseSignature(ruleParts[1], rule.getAction(), rule, true);

            if(signature.remove()) {
                logger.debug("Ignoring rule with bad sig: " + text);
                return null;
            }
            String msg = signature.getMessage();
            // remove the category since it's redundant
            int catlen = category.length();
            if (msg.length() > catlen) {
                String beginMsg = msg.substring(0, catlen);
                if (beginMsg.equalsIgnoreCase(category))
                    msg = msg.substring(catlen).trim();
            }
            rule.setDescription(msg);
            rule.setSignature(signature);
            rule.setClassification(signature.getClassification());
            rule.setURL(signature.getURL());
            //logger.debug("create rule, rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
        }
        catch(ParseException e) { 
            logger.error("Parsing exception for rule: " + text, e);
            return null;
        }
        return rule;
    }

    public List<IDSRuleHeader> matchingPortsList(int port, boolean toServer) {
        List<IDSRuleHeader> returnList = new ArrayList();
        for(IDSRuleHeader header : knownHeaders) {
            if(header.portMatches(port, toServer)) {
                returnList.add(header);
            }
        }
        return returnList;
    }

    public List<IDSRuleSignature> matchesHeader(SessionEndpoints sess, boolean sessInbound, boolean forward) {
        return matchesHeader(sess, sessInbound, forward, knownHeaders);
    }

    public List<IDSRuleSignature> matchesHeader(SessionEndpoints sess, boolean sessInbound, boolean forward, List<IDSRuleHeader> matchList) {
        List<IDSRuleSignature> returnList = new ArrayList();
        //logger.debug("Total List size: "+matchList.size()); /** *****************************************/

        for(IDSRuleHeader header : matchList) {
            if(header.matches(sess, sessInbound, forward)) {
                // logger.debug("Header matches: " + header);
                returnList.addAll(header.getSignatures());
            } else {
                // logger.debug("Header doesn't match: " + header);
            }
        }
        //logger.debug("Signature List Size: "+returnList.size()); /** *****************************************/
        return returnList;
    }

    /*For debug yo*/
    public List<IDSRuleHeader> getHeaders() {
        return knownHeaders;
    }

    public void clear() {
        knownHeaders.clear();
    }

    private String substituteVariables(String string) {
        //string = string.replaceAll("\\$HOME_NET","10.0.0.1/24");
        //string = string.replaceAll("\\$EXTERNAL_NET","!10.0.0.1/24");

        Matcher match = variablePattern.matcher(string);
        if(match.find()) {
            List<IDSVariable> varList, imVarList;
            if(engine.getSettings() == null) {
                imVarList = immutableVariables;
                varList = defaultVariables;
            } else {
                imVarList = (List<IDSVariable>) engine.getSettings().getImmutableVariables();
                varList = (List<IDSVariable>) engine.getSettings().getVariables();
            }
            for(IDSVariable var : imVarList) {
                string = string.replaceAll("\\"+var.getVariable(),var.getDefinition());
            } 
            for(IDSVariable var : varList) {
                // Special case == allow regular variables to refer to immutable variables
                String def = var.getDefinition();
                Matcher submatch = variablePattern.matcher(def);
                if (submatch.find()) {
                    for(IDSVariable subvar : imVarList) {
                        def = def.replaceAll("\\"+subvar.getVariable(),subvar.getDefinition());
                    }
                }
                string = string.replaceAll("\\"+var.getVariable(),def);
            } 
        }
        return string;
    }

    public void dumpRules()
    {
        for(IDSRule rule : knownRules.values()) {
            logger.debug(rule.getHeader() + " /// " + rule.getSignature().toString());
        }
    }
}
