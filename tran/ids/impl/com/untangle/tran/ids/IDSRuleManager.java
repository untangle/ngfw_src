/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.ids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.*;

import com.untangle.mvvm.api.SessionEndpoints;
import com.untangle.mvvm.tran.ParseException;
import org.apache.log4j.Logger;

public class IDSRuleManager {

    public static final boolean TO_SERVER = true;
    public static final boolean TO_CLIENT = false;

    private static final Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");

    private final List<IDSRuleHeader> knownHeaders = new ArrayList<IDSRuleHeader>();
    private final Map<Long,IDSRule> knownRules = new HashMap<Long,IDSRule>();

    private final IDSTransformImpl ids;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public IDSRuleManager(IDSTransformImpl ids)
    {
        this.ids = ids;
        // note the sequence of constructor calls:
        //   IDSTransformImpl -> IDSDetectionEngine -> IDSRuleManager
        // - IDSRuleManager cannot retrieve the IDSDetectionEngine object
        //   from IDSTransformImpl here
        //   (IDSTransformImpl is creating an IDSDetectionEngine object and
        //    thus, in the process of creating this IDSRuleManager object too
        //    so IDSTransformImpl does not have an IDSDetectionEngine object
        //    to return to this IDSRuleManager object right now)
        // - IDSRuleManager must wait for IDSTransformImpl to create and save
        //   an IDSDetectionEngine object
    }

    // static methods ---------------------------------------------------------

    public static List<IDSVariable> getImmutableVariables()
    {
        List<IDSVariable> l = new ArrayList<IDSVariable>();
        l.add(new IDSVariable("$EXTERNAL_NET",IDSStringParser.EXTERNAL_IP,"Magic EXTERNAL_NET token"));
        l.add(new IDSVariable("$HOME_NET",IDSStringParser.HOME_IP,"Magic HOME_NET token"));

        return l;
    }

    public static List<IDSVariable> getDefaultVariables()
    {
        List<IDSVariable> l = new ArrayList<IDSVariable>();
        l.add(new IDSVariable("$HTTP_SERVERS", "$HOME_NET","Addresses of possible local HTTP servers"));
        l.add(new IDSVariable("$HTTP_PORTS", "80","Port that HTTP servers run on"));
        l.add(new IDSVariable("$SSH_PORTS", "22","Port that SSH servers run on"));
        l.add(new IDSVariable("$SMTP_SERVERS", "$HOME_NET","Addresses of possible local SMTP servers"));
        l.add(new IDSVariable("$TELNET_SERVERS", "$HOME_NET","Addresses of possible local telnet servers"));
        l.add(new IDSVariable("$SQL_SERVERS", "!any","Addresses of local SQL servers"));
        l.add(new IDSVariable("$ORACLE_PORTS", "1521","Port that Oracle servers run on"));
        l.add(new IDSVariable("$AIM_SERVERS", "[64.12.24.0/24,64.12.25.0/24,64.12.26.14/24,64.12.28.0/24,64.12.29.0/24,64.12.161.0/24,64.12.163.0/24,205.188.5.0/24,205.188.9.0/24]","Addresses of possible AOL Instant Messaging servers"));

        return l;
    }

    // public methods ---------------------------------------------------------

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
        } else {
            logger.debug("Adding new rule");
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

        IDSRuleSignature signature = IDSStringParser.parseSignature(ids, ruleParts[1], rule.getAction(), rule, false);
        signature.setToString(ruleParts[1]);

        if(!signature.remove() && !rule.disabled()) {
            for(IDSRuleHeader headerTmp : knownHeaders) {
                if(headerTmp.matches(header)) {
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

        // even though rule is removed or disabled,
        // set some rule stuff for gui to display
        // (but don't add this rule to knownRules)
        //rule.setSignature(signature); //Update UI description
        rule.setClassification(signature.getClassification());
        rule.setURL(signature.getURL());
        //logger.debug("skipping rule, rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
        return false;
    }

    // This is how a rule gets created
    public IDSRule createRule(String text, String category) {
        if(text == null || text.length() <= 0 || text.charAt(0) == '#') {
            logger.warn("Ignoring empty rule: " + text);
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
            String ruleParts[]   = IDSStringParser.parseRuleSplit(text);
            IDSRuleHeader header = IDSStringParser.parseHeader(ruleParts[0], rule.getAction());
            if (header == null) {
                logger.warn("Ignoring rule with bad header: " + text);
                return null;
            }
            IDSRuleSignature signature  = IDSStringParser.parseSignature(ids, ruleParts[1], rule.getAction(), rule, true);

            if(signature.remove()) {
                logger.warn("Ignoring rule with bad sig: " + text);
                return null;
            }
            String msg = signature.getMessage();
            signature.setMessage(msg);
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
        } catch(ParseException e) {
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
            IDSDetectionEngine engine = null;
            if (ids != null)
                engine = ids.getEngine();
            List<IDSVariable> varList, imVarList;
            if(engine == null || engine.getSettings() == null) { // XXX only true for testing.
                logger.warn("engine.getSettings() is null");
                imVarList = getImmutableVariables();
                varList = getDefaultVariables();
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
