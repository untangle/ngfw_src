/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.*;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.SessionEndpoints;
import org.apache.log4j.Logger;

public class IPSRuleManager
{

    public static final boolean TO_SERVER = true;
    public static final boolean TO_CLIENT = false;

    private static final Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");

    private final List<IPSRuleHeader> knownHeaders = new ArrayList<IPSRuleHeader>();
    private final Map<Integer,IPSRule> knownRules = new HashMap<Integer,IPSRule>();

    private final Map<IPSRuleHeader, Set<IPSRuleSignature>> signatures
        = new HashMap<IPSRuleHeader, Set<IPSRuleSignature>>();

    private final IPSNodeImpl ips;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public IPSRuleManager(IPSNodeImpl ips)
    {
        this.ips = ips;
        // note the sequence of constructor calls:
        //   IPSNodeImpl -> IPSDetectionEngine -> IPSRuleManager
        // - IPSRuleManager cannot retrieve the IPSDetectionEngine object
        //   from IPSNodeImpl here
        //   (IPSNodeImpl is creating an IPSDetectionEngine object and
        //    thus, in the process of creating this IPSRuleManager object too
        //    so IPSNodeImpl does not have an IPSDetectionEngine object
        //    to return to this IPSRuleManager object right now)
        // - IPSRuleManager must wait for IPSNodeImpl to create and save
        //   an IPSDetectionEngine object
    }

    // static methods ---------------------------------------------------------

    public static List<IPSVariable> getImmutableVariables()
    {
        List<IPSVariable> l = new ArrayList<IPSVariable>();
        l.add(new IPSVariable("$EXTERNAL_NET",IPSStringParser.EXTERNAL_IP,"Magic EXTERNAL_NET token"));
        l.add(new IPSVariable("$HOME_NET",IPSStringParser.HOME_IP,"Magic HOME_NET token"));

        return l;
    }

    public static List<IPSVariable> getDefaultVariables()
    {
        List<IPSVariable> l = new ArrayList<IPSVariable>();
        l.add(new IPSVariable("$HTTP_SERVERS", "$HOME_NET","Addresses of possible local HTTP servers"));
        l.add(new IPSVariable("$HTTP_PORTS", "80","Port that HTTP servers run on"));
        l.add(new IPSVariable("$SSH_PORTS", "22","Port that SSH servers run on"));
        l.add(new IPSVariable("$SMTP_SERVERS", "$HOME_NET","Addresses of possible local SMTP servers"));
        l.add(new IPSVariable("$TELNET_SERVERS", "$HOME_NET","Addresses of possible local telnet servers"));
        l.add(new IPSVariable("$SQL_SERVERS", "!any","Addresses of local SQL servers"));
        l.add(new IPSVariable("$ORACLE_PORTS", "1521","Port that Oracle servers run on"));
        l.add(new IPSVariable("$AIM_SERVERS", "[64.12.24.0/24,64.12.25.0/24,64.12.26.14/24,64.12.28.0/24,64.12.29.0/24,64.12.161.0/24,64.12.163.0/24,205.188.5.0/24,205.188.9.0/24]","Addresses of possible AOL Instant Messaging servers"));

        return l;
    }

    // public methods ---------------------------------------------------------

    public void onReconfigure()
    {
        for(IPSRule rule : knownRules.values()) {
            rule.remove(true);
        }
    }

    public void updateRule(IPSRule rule) throws ParseException
    {
        int sid = rule.getSid();
        IPSRule inMap = knownRules.get(sid);
        if(inMap != null) {
            rule.remove(false);
            if(rule.getModified()) {
                //Delete previous rule
                IPSRuleHeader header = inMap.getHeader();
                removeSignature(header, inMap.getSignature());

                if(signatureListIsEmpty(header)) {
                    logger.info("removing header");
                    knownRules.remove(sid);
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

    public boolean addRule(IPSRule rule) throws ParseException
    {
        String ruleText = rule.getText();

        String noVarText = substituteVariables(ruleText);
        String ruleParts[] = IPSStringParser.parseRuleSplit(noVarText);

        IPSRuleHeader header = IPSStringParser.parseHeader(ruleParts[0], rule.getAction());
        if (header == null) {
            throw new ParseException("Unable to parse header of rule " + ruleParts[0]);
        }

        IPSRuleSignature signature = IPSRuleSignature
            .parseSignature(ips, rule, ruleParts[1], rule.getAction(), false,
                            ruleParts[1]);

        if(!signature.remove() && !rule.disabled()) {
            for(IPSRuleHeader headerTmp : knownHeaders) {
                if(headerTmp.matches(header)) {
                    addSignature(headerTmp, signature);

                    rule.setHeader(headerTmp);
                    rule.setSignature(signature);
                    rule.setClassification(signature.getClassification());
                    rule.setURL(signature.getURL());
                    //logger.debug("add rule (known header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
                    knownRules.put(rule.getSid(),rule);
                    return true;
                }
            }

            addSignature(header, signature);
            knownHeaders.add(header);

            rule.setHeader(header);
            rule.setSignature(signature);
            rule.setClassification(signature.getClassification());
            rule.setURL(signature.getURL());
            //logger.debug("add rule (new header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
            knownRules.put(rule.getSid(),rule);
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
    public IPSRule createRule(String text, String category) {
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

        IPSRule rule = new IPSRule(text, category, "The signature failed to load");

        text = substituteVariables(text);
        try {
            String ruleParts[]   = IPSStringParser.parseRuleSplit(text);
            IPSRuleHeader header = IPSStringParser.parseHeader(ruleParts[0], rule.getAction());
            if (header == null) {
                logger.warn("Ignoring rule with bad header: " + text);
                return null;
            }

            IPSRuleSignature signature  = IPSRuleSignature
                .parseSignature(ips, rule, ruleParts[1], rule.getAction(),
                                true, null);

            if(signature.remove()) {
                logger.warn("Ignoring rule with bad sig: " + text);
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
        } catch(ParseException e) {
            logger.error("Parsing exception for rule: " + text, e);
            return null;
        }
        return rule;
    }

    public List<IPSRuleHeader> matchingPortsList(int port, boolean toServer)
    {
        List<IPSRuleHeader> returnList = new ArrayList();
        for(IPSRuleHeader header : knownHeaders) {
            if(header.portMatches(port, toServer)) {
                returnList.add(header);
            }
        }
        return returnList;
    }

    public Set<IPSRuleSignature> matchesHeader(SessionEndpoints sess,
                                               boolean sessInbound,
                                               boolean forward)
    {
        return matchesHeader(sess, sessInbound, forward, knownHeaders);
    }

    public Set<IPSRuleSignature> matchesHeader(SessionEndpoints sess,
                                               boolean sessInbound,
                                               boolean forward,
                                               List<IPSRuleHeader> matchList)
    {
        Set<IPSRuleSignature> returnSet = new HashSet();
        //logger.debug("Total List size: "+matchList.size());

        for(IPSRuleHeader header : matchList) {
            if(header.matches(sess, sessInbound, forward)) {
                // logger.debug("Header matches: " + header);
                returnSet.addAll(getSignatures(header));
            } else {
                // logger.debug("Header doesn't match: " + header);
            }
        }
        //logger.debug("Signature List Size: "+returnList.size());
        return returnSet;
    }

    public List<IPSRuleHeader> getHeaders()
    {
        return knownHeaders;
    }

    public void clear()
    {
        knownHeaders.clear();
    }

    private String substituteVariables(String string)
    {
        Matcher match = variablePattern.matcher(string);
        if(match.find()) {
            IPSDetectionEngine engine = null;
            if (ips != null)
                engine = ips.getEngine();
            List<IPSVariable> varList, imVarList;
            /* This is null when initializing settings, but the
             * settings are initialized with these values so using the
             * defaults is harmless */
            if(engine == null || engine.getSettings() == null) {
                logger.debug("engine.getSettings() is null");
                imVarList = getImmutableVariables();
                varList = getDefaultVariables();
            } else {
                imVarList = (List<IPSVariable>) engine.getSettings().getImmutableVariables();
                varList = (List<IPSVariable>) engine.getSettings().getVariables();
            }
            for(IPSVariable var : imVarList) {
                string = string.replaceAll("\\"+var.getVariable(),var.getDefinition());
            }
            for(IPSVariable var : varList) {
                // Special case == allow regular variables to refer to immutable variables
                String def = var.getDefinition();
                Matcher submatch = variablePattern.matcher(def);
                if (submatch.find()) {
                    for(IPSVariable subvar : imVarList) {
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
        for(IPSRule rule : knownRules.values()) {
            logger.debug(rule.getHeader() + " /// " + rule.getSignature().toString());
        }
    }

    // private methods ---------------------------------------------------------

    private void addSignature(IPSRuleHeader header, IPSRuleSignature signature)
    {
        Set<IPSRuleSignature> s = signatures.get(header);
        if (null == s) {
            s = new HashSet<IPSRuleSignature>();
            signatures.put(header, s);
        }
        s.add(signature);
    }

    private void removeSignature(IPSRuleHeader header,
                                 IPSRuleSignature signature)
    {
        Set<IPSRuleSignature> s = signatures.get(header);
        if (null != s) {
            s.remove(signature);
            if (s.isEmpty()) {
                signatures.remove(header);
            }
        }
    }

    private Set<IPSRuleSignature> getSignatures(IPSRuleHeader header)
    {
        Set<IPSRuleSignature> s = signatures.get(header);
        if (null == s) {
            return Collections.emptySet();
        } else {
            return s;
        }
    }

    private boolean signatureListIsEmpty(IPSRuleHeader header)
    {
        Set<IPSRuleSignature> s = signatures.get(header);
        return null == s ? true : s.isEmpty();
    }
}
