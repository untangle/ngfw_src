/**
 * $Id$
 */
package com.untangle.node.ips;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.SessionTuple;

public class IpsRuleManager
{
    public static final boolean TO_SERVER = true;
    public static final boolean TO_CLIENT = false;

    private static final Pattern variablePattern = Pattern.compile("\\$[^ \n\r\t]+");

    private final List<IpsRuleHeader> headers = new ArrayList<IpsRuleHeader>();
    private final Map<IpsRuleHeader, Set<IpsRuleSignature>> signatures
        = new HashMap<IpsRuleHeader, Set<IpsRuleSignature>>();

    private final IpsNodeImpl ips;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    public IpsRuleManager(IpsNodeImpl ips)
    {
        this.ips = ips;
        // note the sequence of constructor calls:
        //   IpsNodeImpl -> IpsDetectionEngine -> IpsRuleManager
        // - IpsRuleManager cannot retrieve the IpsDetectionEngine object
        //   from IpsNodeImpl here
        //   (IpsNodeImpl is creating an IpsDetectionEngine object and
        //    thus, in the process of creating this IpsRuleManager object too
        //    so IpsNodeImpl does not have an IpsDetectionEngine object
        //    to return to this IpsRuleManager object right now)
        // - IpsRuleManager must wait for IpsNodeImpl to create and save
        //   an IpsDetectionEngine object
    }

    // static methods ---------------------------------------------------------

    public static Set<IpsVariable> getImmutableVariables()
    {
        Set<IpsVariable> s = new HashSet<IpsVariable>();
        s.add(new IpsVariable("$EXTERNAL_NET",IpsStringParser.EXTERNAL_IP,"Magic EXTERNAL_NET token"));
        s.add(new IpsVariable("$HOME_NET",IpsStringParser.HOME_IP,"Magic HOME_NET token"));

        return s;
    }

    public static Set<IpsVariable> getDefaultVariables()
    {
        Set<IpsVariable> s = new HashSet<IpsVariable>();
        s.add(new IpsVariable("$HTTP_SERVERS", "$HOME_NET","Addresses of possible local HTTP servers"));
        s.add(new IpsVariable("$HTTP_PORTS", "80","Port that HTTP servers run on"));
        s.add(new IpsVariable("$SSH_PORTS", "22","Port that SSH servers run on"));
        s.add(new IpsVariable("$SMTP_SERVERS", "$HOME_NET","Addresses of possible local SMTP servers"));
        s.add(new IpsVariable("$TELNET_SERVERS", "$HOME_NET","Addresses of possible local telnet servers"));
        s.add(new IpsVariable("$SQL_SERVERS", "!any","Addresses of local SQL servers"));
        s.add(new IpsVariable("$ORACLE_PORTS", "1521","Port that Oracle servers run on"));
        s.add(new IpsVariable("$AIM_SERVERS", "[64.12.24.0/24,64.12.25.0/24,64.12.26.14/24,64.12.28.0/24,64.12.29.0/24,64.12.161.0/24,64.12.163.0/24,205.188.5.0/24,205.188.9.0/24]","Addresses of possible AOL Instant Messaging servers"));

        return s;
    }

    // public methods ---------------------------------------------------------

    public void clear()
    {
        signatures.clear();
        headers.clear();
    }

    public boolean addRule(IpsRule rule) throws ParseException
    {
        String ruleText = rule.getText();

        String noVarText = substituteVariables(ruleText);
        String ruleParts[] = IpsStringParser.parseRuleSplit(noVarText);

        IpsRuleHeader header = IpsStringParser.parseHeader(ruleParts[0], rule.trans_getAction());
        if (header == null) {
            throw new ParseException("Unable to parse header of rule " + ruleParts[0]);
        }

        IpsRuleSignature signature = IpsRuleSignature
            .parseSignature(ips, rule, ruleParts[1], rule.trans_getAction(), false,
                            ruleParts[1]);

        if(!signature.remove() && !rule.disabled()) {
            for(IpsRuleHeader headerTmp : headers) {
                if(headerTmp.matches(header)) {
                    addSignature(headerTmp, signature);

                    rule.trans_setClassification(signature.getClassification());
                    rule.trans_setURL(signature.getURL());
                    //logger.debug("add rule (known header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
                    return true;
                }
            }

            addSignature(header, signature);
            headers.add(header);

            rule.trans_setClassification(signature.getClassification());
            rule.trans_setURL(signature.getURL());
            //logger.debug("add rule (new header), rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
            return true;
        }

        // even though rule is removed or disabled,
        // set some rule stuff for gui to display
        // (but don't add this rule to knownRules)
        //rule.setSignature(signature); //Update UI description
        rule.trans_setClassification(signature.getClassification());
        rule.trans_setURL(signature.getURL());
        //logger.debug("skipping rule, rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
        return false;
    }

    // This is how a rule gets created
    public IpsRule createRule(String text, String category)
    {
        if(text == null || text.length() <= 0 || text.charAt(0) == '#') {
            logger.warn("Ignoring empty rule: " + text.substring(0,20));
            return null;
        }

        // Take off the action.
        int firstSpace = text.indexOf(' ');
        if (firstSpace >= 0 && firstSpace < text.length() - 1) {
            text = text.substring(firstSpace + 1);
        }

        IpsRule rule = new IpsRule(text, category, "The signature failed to load");

        text = substituteVariables(text);
        try {
            String ruleParts[]   = IpsStringParser.parseRuleSplit(text);
            IpsRuleHeader header = IpsStringParser.parseHeader(ruleParts[0], rule.trans_getAction());
            if (header == null) {
                logger.warn("Ignoring rule with bad header: " + text.substring(0,20));
                return null;
            }

            IpsRuleSignature signature  = IpsRuleSignature.parseSignature(ips, rule, ruleParts[1], rule.trans_getAction(), true, null);

            if(signature.remove()) {
                logger.warn("Ignoring rule with bad sig: " + text.substring(0,20));
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
            rule.trans_setClassification(signature.getClassification());
            rule.trans_setURL(signature.getURL());
            //logger.debug("create rule, rc: " + rule.getClassification() + ", rurl: " + rule.getURL());
        } catch(ParseException e) {
            logger.error("Parsing exception for rule: " + text, e);
            return null;
        }
        return rule;
    }

    public List<IpsRuleHeader> matchingPortsList(int port, boolean toServer)
    {
        List<IpsRuleHeader> returnList = new ArrayList<IpsRuleHeader>();
        for(IpsRuleHeader header : headers) {
            if(header.portMatches(port, toServer)) {
                returnList.add(header);
            }
        }
        return returnList;
    }

    public Set<IpsRuleSignature> matchesHeader(SessionTuple sess,
                                               boolean sessInbound,
                                               boolean forward)
    {
        return matchesHeader(sess, sessInbound, forward, headers);
    }

    public Set<IpsRuleSignature> matchesHeader(SessionTuple sess,
                                               boolean sessInbound,
                                               boolean forward,
                                               List<IpsRuleHeader> matchList)
    {
        Set<IpsRuleSignature> returnSet = new HashSet<IpsRuleSignature>();
        //logger.debug("Total List size: "+matchList.size());

        for(IpsRuleHeader header : matchList) {
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

    public List<IpsRuleHeader> getHeaders()
    {
        return headers;
    }

    public void clearRules()
    {
        signatures.clear();
        headers.clear();
    }

    private String substituteVariables(String string)
    {
        Matcher match = variablePattern.matcher(string);
        if(match.find()) {
            IpsDetectionEngine engine = null;
            if (ips != null)
                engine = ips.getEngine();
            Set<IpsVariable> varSet, imVarSet;
            /* This is null when initializing settings, but the
             * settings are initialized with these values so using the
             * defaults is harmless */
            if(engine == null || engine.getSettings() == null) {
                logger.debug("engine.getSettings() is null");
                imVarSet = getImmutableVariables();
                varSet = getDefaultVariables();
            } else {
                imVarSet = engine.getSettings().grabImmutables();
                varSet = engine.getSettings().grabVariables();
            }
            for(IpsVariable var : imVarSet) {
                string = string.replaceAll("\\"+var.getVariable(),var.getDefinition());
            }
            for(IpsVariable var : varSet) {
                // Special case == allow regular variables to refer to immutable variables
                String def = var.getDefinition();
                Matcher submatch = variablePattern.matcher(def);
                if (submatch.find()) {
                    for(IpsVariable subvar : imVarSet) {
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
    }

    // private methods ---------------------------------------------------------

    private void addSignature(IpsRuleHeader header, IpsRuleSignature signature)
    {
        Set<IpsRuleSignature> s = signatures.get(header);
        if (null == s) {
            s = new HashSet<IpsRuleSignature>();
            signatures.put(header, s);
        }
        s.add(signature);
    }

    private Set<IpsRuleSignature> getSignatures(IpsRuleHeader header)
    {
        Set<IpsRuleSignature> s = signatures.get(header);
        if (null == s) {
            return Collections.emptySet();
        } else {
            return s;
        }
    }
}
