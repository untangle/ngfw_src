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

package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import com.untangle.node.http.RequestLineToken;
import com.untangle.node.token.Header;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.IPMaddrRule;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.MimeTypeRule;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public abstract class Blacklist
{
    private static final Pattern IP_PATTERN = Pattern
        .compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    private final Logger logger = Logger.getLogger(Blacklist.class);

    private final WebFilterBase node;

    private final Map<InetAddress, Set<String>> hostWhitelists
        = new HashMap<InetAddress, Set<String>>();

    private volatile WebFilterSettings settings;
    private volatile String[] blockedUrls = new String[0];
    private volatile String[] passedUrls = new String[0];

    // constructors -----------------------------------------------------------

    public Blacklist(WebFilterBase node)
    {
        this.node = node;
    }

    // abstract methods -------------------------------------------------------

    public abstract void open();
    public abstract void close();

    // blacklist methods ------------------------------------------------------

    public void configure(WebFilterSettings settings)
    {
        this.settings = settings;
    }

    public synchronized void reconfigure()
    {
        blockedUrls = makeCustomList(settings.getBlockedUrls());
        passedUrls = makeCustomList(settings.getPassedUrls());

        doReconfigure();
    }

    // protected methods ------------------------------------------------------

    protected abstract void doReconfigure();

    protected abstract boolean getLookupSubdomains();

    // package protected methods ----------------------------------------------

    void addWhitelistHost(InetAddress addr, String site)
    {
        Set<String> wl;
        synchronized (hostWhitelists) {
            wl = hostWhitelists.get(addr);
            if (null == wl) {
                wl = new HashSet<String>();
                hostWhitelists.put(addr, wl);
            }
        }

        synchronized (wl) {
            wl.add(site);
        }
    }

    /**
     * For each InetAddress in the map, remove the associated
     * host-bypassed sites.
     *
     * @param map a Map<InetAddress, List<String>>
     */
    void removeUnblockedSites(Map<InetAddress, List<String>> map)
    {
    logger.info("about to remove host-bypassed sites for "  + map.size() + " host(s)");

    InetAddress addr;
    List<String> bypassedSites;
    Set<String> hostSites;

    synchronized(hostWhitelists) {
        for (Map.Entry<InetAddress, List<String>> entry : map.entrySet()) {
        addr = entry.getKey();
        logger.info(".. for address '" + addr + "'");
        bypassedSites = entry.getValue();

        hostSites = hostWhitelists.get(addr);

        for (String site : bypassedSites) {
            if (hostSites.contains(site)) {
            logger.info(".... removing unblocked site " + site);
            hostSites.remove(site);
            if (hostSites.isEmpty()) {
                logger.info(".... '" + addr + "' has no more unblocked sites");
                hostWhitelists.remove(addr);
                break;
            }
            }
        }
        }
    }
    }

    /**
     * Remove all the unblocked sites for all the clients.
     */
    void removeAllUnblockedSites() {
        hostWhitelists.clear();
    }

    public String checkRequest(InetAddress clientIp, int port,
                               RequestLineToken requestLine, Header header)
    {
        return checkRequest(clientIp, port, requestLine, header, null);
    }

    /**
     * Checks if the request should be blocked, giving an appropriate
     * response if it should.
     *
     * @param clientIp IP That made the request.
     * @param port Port that the request was made to.
     * @param requestLine
     * @param header
     * @param event This is the new sessions request associated with this request, (or null if this is later.)
     * @return an HTML response.
     */
    public String checkRequest(InetAddress clientIp, int port,
                               RequestLineToken requestLine, Header header,
                               TCPNewSessionRequestEvent event)
    {
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("/+", "/"));
        } catch (URISyntaxException e) {
            logger.fatal("Could not parse URI '" + uri + "'");
        }

        String description;

        String path = uri.getPath();
        path = null == path ? "" : uri.getPath().toLowerCase();

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }
        host = normalizeHostname(host);

        logger.debug("checkRequest: " + host + uri);

        // check client IP address pass list
        description = isClientPassListed(clientIp);
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), Action.PASS, Reason.PASS_CLIENT,
                 description, node.getVendor());
            logger.info(hbe);
            return null;
        }

        // check passlisted rules
        description = isSitePassListed(host,uri);
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), Action.PASS,
                 Reason.PASS_URL, description, node.getVendor());
            logger.debug("LOG: in pass list: " + requestLine.getRequestLine());
            node.log(hbe, host, port, event);
            return null;
        }

        // check bypasses
        if (isSiteBypassed(host, uri, clientIp)) {
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), Action.PASS,
                 Reason.PASS_BYPASS, "unblocked",
                 node.getVendor());
            logger.debug("LOG: in bypass list: " + requestLine.getRequestLine());
            node.log(hbe, host, port, event);
            return null;
        }

        // only check block all IP hosts on http traffic
        if (80 == port && settings.getBaseSettings().getBlockAllIpHosts()) {
            if (null == host || IP_PATTERN.matcher(host).matches()) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_IP_HOST, host, node.getVendor());
                logger.debug("LOG: block all IPs: " + requestLine.getRequestLine());
                node.log(hbe, host, port, event);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().
                    languageManager().getTranslations("untangle-node-webfilter");

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     I18nUtil.tr("host name is an IP address ({0})", host, i18nMap),
                     clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            }
        }

        // check in WebFilterSettings
        String nonce = checkBlacklist(clientIp, host, port, requestLine, event);

        if (null != nonce) {
            return nonce;
        }

        // Check Extensions
        for (StringRule rule : settings.getBlockedExtensions()) {
            String exn = "."+rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking extension " + exn);
                }
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_EXTENSION, exn, node.getVendor());
                logger.debug("LOG: in extensions list: " + requestLine.getRequestLine());
                node.log(hbe, host, port, event);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().
                    languageManager().getTranslations("untangle-node-webfilter");

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     I18nUtil.tr("extension ({0})", exn, i18nMap), clientIp,
                     node.getNodeTitle());
                return node.generateNonce(bd);
            }
        }

        WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(),
                                                null, null, null,
                                                node.getVendor(), true);
        node.log(hbe, host, port, event);

        return null;
    }

    public String checkResponse(InetAddress clientIp,
                                RequestLineToken requestLine, Header header)
    {
        if (null == requestLine) {
            return null;
        }

        String contentType = header.getValue("content-type");
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("/+", "/"));
        } catch (URISyntaxException e) {
            logger.fatal("Could not parse URI '" + uri + "'");
        }
        String host = normalizeHostname(requestLine.getRequestLine().getUrl().getHost());

        if (isClientPassListed(clientIp) != null)
            return null;
        if (isSitePassListed(host,uri) != null)
            return null;
        if (isSiteBypassed(host,uri,clientIp))
            return null;

        logger.debug("checkResponse: " + host + uri + " content: " + contentType);

        // check mime-type list
        for (MimeTypeRule rule : settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType, node.getVendor());
                logger.debug("LOG: in mimetype list: " + requestLine.getRequestLine());
                node.log(hbe);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().
                    languageManager().getTranslations("untangle-node-webfilter");

                WebFilterBlockDetails bd = new WebFilterBlockDetails
                    (settings, host, uri.toString(),
                     I18nUtil.tr("Mime-Type ({0})", contentType, i18nMap),
                     clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            }
        }

        return null;
    }

    // protected methods ------------------------------------------------------

    protected abstract String checkBlacklistDatabase(String dom, int port,
                                                     String uri);

    protected abstract void updateToCurrentCategories(WebFilterSettings s);

    protected WebFilterSettings getSettings()
    {
        return settings;
    }

    protected String mostSpecificCategory(List<String> catNames)
    {
        String catName = null;

        if (catNames != null) {
            for (String cn : catNames) {
                BlacklistCategory bc = getSettings().getBlacklistCategory(cn);

                if (null != bc && (bc.getBlock() || bc.getLog())) {
                    catName = cn;
                    if (bc.getBlock()) {
                        break;
                    }
                }
            }
        }

        return catName;
    }

    // private methods --------------------------------------------------------

    /**
     * isSitePassListed checks the host+uri against the pass list
     *
     * @param host host of the URL
     * @param uri URI of the URL
     * @return description of the rule that passlist rule, null if DNE
     */
    private String isSitePassListed(String host, URI uri)
    {
        String dom;
        for (dom = host ; null != dom ; dom = nextHost(dom)) {
            StringRule sr = findCategory(passedUrls, dom + uri, settings.getPassedUrls());
            String category = null == sr ? null : sr.getDescription();

            if (null != category) {
                logger.debug("LOG: "+host+uri+" in pass list");
                return category;
            }
        }

        return null;
    }

    /**
     * isClientPassListed checks the clientIp against the client pass list
     *
     * @param clientIp IP of the host
     * @return description of the rule that passlist rule, null if DNE
     */
    private String isClientPassListed(InetAddress clientIp)
    {
        for (IPMaddrRule rule : settings.getPassedClients()) {
            if (rule.getIpMaddr().contains(clientIp) && rule.isLive()) {
                logger.debug("LOG: "+clientIp+" in client pass list");
                return rule.getDescription();
            }
        }

        return null;
    }

    /**
     * isSiteBypassed checks the host+uri against the current bypasses for clientIp
     *
     * @param host host of the URL
     * @param uri URI of the URL
     * @param clientIp IP of the host
     * @return true if the site has been explicitly bypassed for that user, false otherwise
     */
    private boolean isSiteBypassed(String host, URI uri, InetAddress clientIp)
    {
        String dom;
        for (dom = host ; null != dom ; dom = nextHost(dom)) {
            if (isUserWhitelistedDomain(dom, clientIp)) {
                logger.debug("LOG: "+host+uri+" in bypass list for "+ clientIp);
                return true;
            }
        }

        return false;
    }

    /**
     * normalize the hostname
     *
     * @param host host of the URL
     * @return the normalized string for that hostname, or null if param is null
     */
    private String normalizeHostname(String oldhost)
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

    private String checkBlacklist(InetAddress clientIp, String host, int port,
                                  RequestLineToken requestLine, TCPNewSessionRequestEvent event)
    {
        URI reqUri = requestLine.getRequestUri();

        String uri;
        if (reqUri.isAbsolute()) {
            host = reqUri.getHost();
            uri = reqUri.normalize().getRawPath();
        } else {
            uri = reqUri.normalize().toString();
        }

        uri = uri.replaceAll("/+", "/");

        logger.debug("checkBlacklist: " + host + uri);

        BlacklistCategory category = findBestCategory(host, port, uri);

        StringRule stringRule;
        if (null == category || !category.getBlock()) {
            stringRule = findBestRule(host, uri, port, requestLine, event);
        } else {
            stringRule = null;
        }

        if (category != null && category.getBlock()) {
            Action a = Action.BLOCK;
            Reason reason = Reason.BLOCK_CATEGORY;
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), a, reason,
                 category.getDisplayName(), node.getVendor());
            node.log(hbe, host, port,event);

            WebFilterBlockDetails bd = new WebFilterBlockDetails
                (settings, host, uri, category.getDescription(), clientIp,
                 node.getNodeTitle());
            return node.generateNonce(bd);
        } else if (stringRule != null && stringRule.isLive()) {
            Action a = Action.BLOCK;
            Reason reason = Reason.BLOCK_URL;
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), a, reason,
                 stringRule.getDescription(), node.getVendor());
            node.log(hbe, host, port, event);

            WebFilterBlockDetails bd = new WebFilterBlockDetails
                (settings, host, uri, stringRule.getDescription(), clientIp,
                 node.getNodeTitle());
            return node.generateNonce(bd);
        } else if (category != null) {
            Action a = Action.PASS;
            Reason reason = Reason.BLOCK_CATEGORY;
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), a, reason,
                 category.getDisplayName(), node.getVendor());
            node.log(hbe, host, port, event);
            node.incrementPassLogCount();
            return null;
        } else if (stringRule != null) {
            Action a = Action.PASS;
            Reason reason = Reason.BLOCK_URL;
            node.incrementPassLogCount();
            WebFilterEvent hbe = new WebFilterEvent
                (requestLine.getRequestLine(), a, reason,
                 stringRule.getDescription(), node.getVendor());
            node.log(hbe, host, port, event);
            return null;
        } else {
            return null;
        }
    }

    private BlacklistCategory findBestCategory(String host, int port,
                                               String uri)
    {
        BlacklistCategory category = null;
        boolean blockFound = false;

        boolean checkSubdomains = getLookupSubdomains();

        String dom = host;
        while (!blockFound && null != dom) {
            String sCat = checkBlacklistDatabase(dom, port, uri);
            BlacklistCategory bc;
            if (null != sCat) {
                bc = settings.getBlacklistCategory(sCat);
            } else {
                bc = null;
            }

            if (null != bc) {
                category = bc;
                blockFound = category.getBlock();
            }

            if (!checkSubdomains) {
                break;
            }

            dom = nextHost(dom);
        }

        return category;
    }

    private StringRule findBestRule(String host, String uri, int port,
                                    RequestLineToken requestLine, TCPNewSessionRequestEvent event)
    {
        StringRule stringRule = null;
        boolean blockFound = false;

        String dom = host;
        while (!blockFound && null != dom) {
            String url = dom + uri;

            StringRule sr = findCategory(blockedUrls, url,
                                         settings.getBlockedUrls());

            if (null != sr) {
                stringRule = sr;
                blockFound = stringRule.isLive();
                if (!blockFound) {
                    Action a = Action.PASS;
                    Reason reason = Reason.BLOCK_URL;
                    WebFilterEvent hbe = new WebFilterEvent
                        (requestLine.getRequestLine(), a, reason,
                         stringRule.getDescription(), node.getVendor());
                    node.log(hbe, host, port, event);
                }
            }

            dom = nextHost(dom);
        }

        return stringRule;
    }

    private StringRule findCategory(CharSequence[] strs, String val,
                                    Set<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private int findMatch(CharSequence[] strs, String val)
    {
	logger.debug("findMatch: strs = '" + Arrays.asList(strs) +
		     "', val = '" + val  + "'");

        if (null == val || null == strs) {
            return -1;
        }

        val = normalizeDomain(val);

	// we should probably do the "transform globbing into regex"
	// once and for all, at the time they are entered by the
	// administrator. To achieve that we'd simply need another
	// field in the settings.u_string_rule, for instance "pattern".
	// --Seb, 11/3/2009
	String re;
	CharSequence str;
	for (int i = 0; i < strs.length; i++) {
	    str = strs[i];
	    // transform globbing operators into regex ones
	    re = str.toString();
	    re = re.replaceAll(Pattern.quote("."), "\\.");
	    re = re.replaceAll(Pattern.quote("*"), ".*");
	    re = re.replaceAll(Pattern.quote("?"), ".");
	    // possibly some path after a domain name... People
	    // specifying 'google.com' certainly want to block
	    // '"google.com/whatever"
	    re = re + "(/.*)?";

	    // match
	    try {
		if (Pattern.matches(re, val)) {
		    logger.debug("findMatch: ** matches pattern '" + re + "'");
		    return i; // done, we do not care if others match too
		} else {
		    logger.debug("findMatch: ** does not match '" + re + "'");		
		}
	    } catch (PatternSyntaxException e) {
		logger.error("findMatch: ** invalid pattern '" + re + "'");		
	    }
	}
	return -1; // no matches at all
    }

    private StringRule lookupCategory(CharSequence match,
                                      Set<StringRule> rules)
    {
        for (StringRule rule : rules) {
            String uri = normalizeDomain(rule.getString());

            if ((rule.isLive() || rule.getLog()) && match.equals(uri)) {
                return rule;
            }
        }

        return null;
    }

    /**
     * Gets the next domain stripping off the lowest level domain from
     * host. Does not return the top level domain. Returns null when
     * no more domains are left.
     *
     * <b>This method assumes trailing dots are stripped from host.</b>
     *
     * @param host a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String nextHost(String host)
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

    private String[] makeCustomList(Set<StringRule> rules)
    {
        List<String> strings = new ArrayList<String>(rules.size());
        for (StringRule rule : rules) {
            if (rule.isLive()) {
                String uri = normalizeDomain(rule.getString());
                strings.add(uri);
            }
        }
        return strings.toArray(new String[strings.size()]);
    }

    private String normalizeDomain(String dom)
    {
        String url = dom.toLowerCase();
        String uri = url.startsWith("http://")
            ? url.substring("http://".length()) : url;

        while (0 < uri.length()
               && ('.' == uri.charAt(0))) {
            uri = uri.substring(1);
        }

        if (uri.startsWith("www.")) {
            uri = uri.substring("www.".length());
        }

        return uri;
    }

    private boolean isUserWhitelistedDomain(String domain, InetAddress clientAddr)
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            Set<String> l = hostWhitelists.get(clientAddr);
            if (null == l) {
                return false;
            } else {
                return findMatch(l, domain);
            }
        }
    }

    private boolean findMatch(Set<String> rules, String domain)
    {
        for (String d = domain; null != d; d = nextHost(d)) {
            if (rules.contains(d)) {
                return true;
            }
        }

        return false;
    }
}
