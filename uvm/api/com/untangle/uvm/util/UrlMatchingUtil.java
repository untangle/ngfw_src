/**
 * $Id$
 */

package com.untangle.uvm.util;

import java.net.InetAddress;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.app.IPMatcher;
import com.untangle.uvm.app.UrlMatcher;

/**
 * URI matching utility
 */
public class UrlMatchingUtil
{
    private static final Logger logger = Logger.getLogger(UrlMatchingUtil.class);

    /**
     * normalize the hostname
     * 
     * @param oldhost
     *        host of the URL
     * @return the normalized string for that hostname, or null if param is null
     */
    public static String normalizeHostname(String oldhost)
    {
        if (null == oldhost) return null;

        // lowercase name
        String host = oldhost.toLowerCase();

        // remove dots at end
        while (0 < host.length() && '.' == host.charAt(host.length() - 1)) {
            host = host.substring(0, host.length() - 1);
        }

        return host;
    }

    /**
     * Gets the next domain stripping off the lowest level domain from host.
     * Does not return the top level domain. Returns null when no more domains
     * are left.
     * 
     * <b>This method assumes trailing dots are stripped from host.</b>
     * 
     * @param host
     *        a <code>String</code> value
     * @return a <code>String</code> value
     */
    public static String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (-1 == i) {
            return null;
        } else {
            int j = host.indexOf('.', i + 1);
            if (-1 == j) { // skip tld
                return null;
            }

            return host.substring(i + 1);
        }
    }

    /**
     * checkSiteList checks the host+uri against the provided list
     *
     * @param domain
     *        host of the URL
     * @param uri
     *        URI of the URL
     * @param rules
     *        The list of rules
     * @return the rule that matches, null if DNE6
     */
    public static GenericRule checkSiteList(String domain, String uri, List<GenericRule> rules)
    {
        for (GenericRule rule : rules) {
            if (rule.getEnabled() != null && !rule.getEnabled()) continue;

            Object matcherO = rule.attachment();
            UrlMatcher matcher = null;

            /**
             * If the matcher is not attached to the rule, initialize a new one
             * and attach it. Otherwise just use the matcher already initialized
             * and attached to the rule
             */
            if (matcherO == null || !(matcherO instanceof UrlMatcher)) {
                matcher = UrlMatcher.getMatcher(rule.getString());
                rule.attach(matcher);
            } else {
                matcher = (UrlMatcher) matcherO;
            }

            if (matcher.isMatch(domain, uri)) {
                logger.debug("LOG: " + domain + uri + " in site list");
                return rule;
            }
        }

        return null;
    }

    /**
     * checkClientPassList checks the clientIp against the client pass list
     * 
     * @param clientIp
     *        IP of the host
     * @param rulesList
     *        The list of rules
     * @return the rule that matches, null if DNE
     */
    public static GenericRule checkClientList(InetAddress clientIp, List<GenericRule> rulesList)
    {
        return checkClientServerList(clientIp, null, rulesList);
    }
    /**
     * checkClientServerPassList checks the clientIp and serverIP against the client pass list
     * 
     * @param clientIp
     *        IP of the client
     * @param serverIp
     *        IP of the server
     * @param rulesList
     *        The list of rules
     * @return the rule that matches, null if DNE
     */
    public static GenericRule checkClientServerList(InetAddress clientIp, InetAddress serverIp, List<GenericRule> rulesList)
    {
        for (GenericRule rule : rulesList) {
            if (rule.getEnabled() != null && !rule.getEnabled()) continue;

            Object matcherO = rule.attachment();
            IPMatcher matcher = null;

            /**
             * If the matcher is not attached to the rule, initialize a new one
             * and attach it. Otherwise just use the matcher already initialized
             * and attached to the rule
             */
            if (matcherO == null || !(matcherO instanceof IPMatcher)) {
                matcher = new IPMatcher(rule.getString());
                rule.attach(matcher);
            } else {
                matcher = (IPMatcher) matcherO;
            }

            if (matcher.isMatch(clientIp) ||
                ( serverIp != null && matcher.isMatch(serverIp)) ) {
                return rule;
            }
        }

        return null;
    }
}
