package com.untangle.node.util;

import java.net.InetAddress;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.IPMatcher;

public class UrlMatchingUtil
{
	private static final Logger logger = Logger.getLogger(UrlMatchingUtil.class);

	/**
	 * normalize the hostname
	 * 
	 * @param host
	 *            host of the URL
	 * @return the normalized string for that hostname, or null if param is null
	 */
	public static String normalizeHostname(String oldhost)
	{
		if (null == oldhost)
			return null;

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
	 *            a <code>String</code> value
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
	 * normalizes a domain name removes extra "http://" or "www." or "." at the
	 * beginning
	 */
	public static String normalizeDomain(String dom)
	{
		String url = dom.toLowerCase();
		String uri = url.startsWith("http://") ? url.substring("http://".length()) : url;

		while (0 < uri.length() && ('.' == uri.charAt(0))) {
			uri = uri.substring(1);
		}

		if (uri.startsWith("www.")) {
			uri = uri.substring("www.".length());
		}

		return uri;
	}

	/**
	 * Finds a matching active rule from the ruleset that matches the given
	 * value
	 */
	public static GenericRule findMatchingRule(List<GenericRule> rules, String domain, String uri)
	{
		String value = normalizeDomain(domain) + uri;

		logger.debug("findMatchRule: rules = '" + rules + "', value = '" + value + "' (normalized from '" + domain
				+ uri + ")");

		for (GenericRule rule : rules) {
			if (rule.getEnabled() != null && !rule.getEnabled())
				continue;

			Object regexO = rule.attachment();
			Pattern regex = null;

			/**
			 * If the regex is not attached to the rule, compile a new one and
			 * attach it Otherwise just use the regex already compiled and
			 * attached to the rule
			 */
			if (regexO == null || !(regexO instanceof Pattern)) {
				String re = GlobUtil.urlGlobToRegex(rule.getString());

				logger.debug("Compile  rule: " + re);
				try {
					regex = Pattern.compile(re);
				} catch (Exception e) {
					logger.warn("Failed to compile regex: " + re, e);
					// Use a regex that will never match anything
					regex = Pattern.compile("a^");
				}
				rule.attach(regex);
			} else {
				regex = (Pattern) regexO;
			}

			/**
			 * Check the match
			 */
			try {
				logger.debug("Checking rule: " + rule.getString() + " (re: " + regex + ") against " + value);

				if (regex.matcher(value).matches()) {
					logger.debug("findMatchRule: ** matches pattern '" + regex + "'");
					return rule; // done, we do not care if others match too
				} else {
					logger.debug("findMatchRule: ** does not match '" + regex + "'");
				}
			} catch (PatternSyntaxException e) {
				logger.error("findMatchRule: ** invalid pattern '" + regex + "'");
			}

		}

		return null;
	}

	/**
	 * checkSiteList checks the host+uri against the provided list
	 * 
	 * @param host
	 *            host of the URL
	 * @param uri
	 *            URI of the URL
	 * @return the rule that matches, null if DNE
	 */
	public static GenericRule checkSiteList(String host, String uri, List<GenericRule> rulesList)
	{
		String dom;
		for (dom = host; null != dom; dom = nextHost(dom)) {
			GenericRule sr = findMatchingRule(rulesList, dom, uri);

			if (sr != null) {
				return sr;
			}
		}

		return null;
	}

	/**
	 * checkClientPassList checks the clientIp against the client pass list
	 * 
	 * @param clientIp
	 *            IP of the host
	 * @return the rule that matches, null if DNE
	 */
	public static GenericRule checkClientList(InetAddress clientIp, List<GenericRule> rulesList)
	{
		for (GenericRule rule : rulesList) {
			if (rule.getEnabled() != null && !rule.getEnabled())
				continue;

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

			if (rule.getEnabled() && matcher.isMatch(clientIp)) {
				logger.debug("LOG: " + clientIp + " in client pass list");
				return rule;
			}
		}

		return null;
	}

}
