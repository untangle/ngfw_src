/**
 * $HeadURL$
 */

package com.untangle.node.web_filter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.uvm.LanguageManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.UrlMatcher;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.HeaderToken;
import com.untangle.node.http.HttpEventHandler;

/**
 * This is the core functionality of web filter It decides if a site should be
 * blocked, passed, logged, etc based on the settings and categorization.
 */
public abstract class DecisionEngine
{
    private Map<String, String> i18nMap;
    Long i18nMapLastUpdated = 0L;

    private final Logger logger = Logger.getLogger(getClass());

    /**
     * This regex matches any URL that is IP based - http://1.2.3.4/
     */
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

    /**
     * This is the base node that owns this decision engine
     */
    private final WebFilterBase node;

    /**
     * Users are able to "unblock" sites if the admin allows it. Unblocked sites
     * are temporary and only stored in memory This map stores a list of
     * unblocked sites by IP address
     */
    private final Map<InetAddress, HashSet<String>> unblockedDomains = new HashMap<InetAddress, HashSet<String>>();

    public DecisionEngine(WebFilterBase node)
    {
        this.node = node;
    }

    /**
     * This must be overridden by the specific implementation of the Decision
     * Engine It must return a list of categories (strings) for a given URL
     */
    protected abstract List<String> categorizeSite(String dom, int port, String uri);

    /**
     * Checks if the request should be blocked, giving an appropriate response
     * if it should.
     * 
     * @param clientIp
     *            IP That made the request.
     * @param port
     *            Port that the request was made to.
     * @param requestLine
     * @param header
     * @param event
     *            This is the new sessions request associated with this request,
     *            (or null if this is later.)
     * @return an HTML response (null means the site is passed and no response
     *         is given).
     */
    public String checkRequest(NodeTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        Boolean isFlagged = false; /*
                                    * this stores whether this visit should be
                                    * flagged for any reason
                                    */
        Reason reason = Reason.DEFAULT; /*
                                         * this stores the corresponding reason
                                         * for the flag/block
                                         */
        GenericRule bestCategory = null;
        String requestMethod = null;
        URI uri = null;

        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("(?<!:)/+", "/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
        }

        if (sess != null) {
            // first we attach the request method
            String rmethod = requestLine.getMethod().toString();
            if (rmethod != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_REQUEST_METHOD, rmethod);

            // start with the URI from the request
            String furi = uri.toString();
            String fpath = null;
            String fname = null;
            String fext = null;
            int loc;
            try {
                // extract the full file path ignoring all params
                fpath = (new URI(uri.getPath())).toString();

                // find the last slash to extract the file name
                loc = fpath.lastIndexOf("/");
                if (loc != -1) fname = fpath.substring(loc + 1);

                // find the last dot to extract the file extension
                loc = fname.lastIndexOf(".");
                if (loc != -1) fext = fname.substring(loc + 1);

                if (fpath != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_PATH, fpath);
                if (fname != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_NAME, fname);
                if (fext != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_REQUEST_FILE_EXTENSION, fext);

            } catch (URISyntaxException e) {
            }
        }

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }

        host = UrlMatchingUtil.normalizeHostname(host);

        // start by getting the category for the request and attach to session
        bestCategory = checkCategory(sess, clientIp, host, port, requestLine);

        // tag the session with the metadata
        if (sess != null) {
            sess.globalAttach(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_ID, bestCategory.getId());
            sess.globalAttach(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME, bestCategory.getName());
            sess.globalAttach(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION, bestCategory.getDescription());
            sess.globalAttach(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_FLAGGED, bestCategory.getFlagged());
            sess.globalAttach(NodeSession.KEY_WEB_FILTER_BEST_CATEGORY_BLOCKED, bestCategory.getBlocked());
        }

        // check client IP pass list
        // If a client is on the pass list is is passed regardless of any other settings
        GenericRule rule = UrlMatchingUtil.checkClientList(clientIp, node.getSettings().getPassedClients());
        String description = (rule != null) ? rule.getDescription() : null;
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_CLIENT, description, node.getName());
            node.logEvent(hbe);
            return null;
        }

        // check server IP pass list
        // If a site/URL is on the pass list is is passed regardless of any other settings
        rule = UrlMatchingUtil.checkSiteList(host, uri.toString(), node.getSettings().getPassedUrls());
        description = (rule != null) ? rule.getDescription() : null;
        if (description != null) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_URL, description, node.getName());
            logger.debug("LOG: in pass list: " + requestLine.getRequestLine());
            node.logEvent(hbe);
            return null;
        }

        // restrict google applications
        // If restricting google app access, verify google domain and add restrict header.
        if (node.getSettings().getRestrictGoogleApps()) {
            UrlMatcher matcher = new UrlMatcher("*google*");
            if (matcher.isMatch(host, uri.toString())) {
                header.addField("X-GoogApps-Allowed-Domains", node.getSettings().getRestrictGoogleAppsDomain());
            }
        }

        String refererHeader = header.getValue("referer");
        if (node.getSettings().getPassReferers() && (refererHeader != null)) {
            try {
                URI refererUri = new URI(refererHeader.replaceAll("(?<!:)/+", "/"));
                String refererHost = refererUri.getHost();
                if (refererHost == null) {
                    refererHost = host;
                }
                refererHost = UrlMatchingUtil.normalizeHostname(refererHost);

                rule = UrlMatchingUtil.checkSiteList(refererHost, refererUri.getPath().toString(), node.getSettings().getPassedUrls());
                description = (rule != null) ? rule.getDescription() : null;
                if (description != null) {
                    WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_REFERER_URL, description, node.getName());
                    logger.debug("LOG: Referer in pass list: " + requestLine.getRequestLine());
                    node.logEvent(hbe);
                    return null;
                }
            } catch (URISyntaxException e) {
                logger.warn("Could not parse referer URI '" + refererHeader + "' " + e.getClass());
            }
        }

        // check unblocks
        // if a site/URL is unblocked already for this specific IP it is passed regardless of any other settings
        if (checkUnblockedSites(host, uri, clientIp)) {
            String bestCategoryStr = null;
            if (bestCategory != null) bestCategoryStr = bestCategory.getName();
            if (bestCategoryStr == null) {
                updateI18nMap();
                bestCategoryStr = I18nUtil.tr("Unblocked", i18nMap);
            }

            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_UNBLOCK, bestCategoryStr, node.getName());
            logger.debug("LOG: in unblock list: " + requestLine.getRequestLine());
            node.logEvent(hbe);
            return null;
        }

        // if this is HTTP traffic and the request is IP-based and block IP-based browsing is enabled, block this traffic
        if (port == 80 && node.getSettings().getBlockAllIpHosts()) {
            if (host == null || IP_PATTERN.matcher(host).matches()) {
                updateI18nMap();
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_IP_HOST, host, node.getName());
                logger.debug("LOG: block all IPs: " + requestLine.getRequestLine());
                node.logEvent(hbe);

                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), I18nUtil.tr("Host name is an IP address ({0})", host, i18nMap), clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            }
        }

        // Check Block lists
        GenericRule urlRule = checkUrlList(host, uri.toString(), requestLine);
        if (urlRule != null) {
            if (urlRule.getBlocked()) {
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), urlRule.getDescription(), clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            } else if (!isFlagged && urlRule.getFlagged()) {
                isFlagged = true;
                reason = Reason.BLOCK_URL;
            }
        }

        // check the filter rules
        WebFilterRule filterRule = checkFilterRules(sess, "REQUEST");

        /**
         * The filter rules take priority over category so if we find a block or
         * flag rule we log the event and pass or block right here
         */
        if ((filterRule != null) && (filterRule.getBlocked())) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.FILTER_RULE, filterRule.getDescription(), node.getName());
            node.logEvent(hbe);
            WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), filterRule.getDescription(), clientIp, node.getNodeTitle());
            return node.generateNonce(bd);
        } else if ((filterRule != null) && (filterRule.getFlagged())) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.TRUE, Reason.FILTER_RULE, filterRule.getDescription(), node.getName());
            node.logEvent(hbe);
            return null;
        }

        /**
         * We did the category lookup earlier and didn't hit any of the pass or
         * block lists and don't have a filter rule match so we use the category
         * to make the final pass/block/flag decisions
         */

        if (bestCategory != null) {
            if (!isFlagged && bestCategory.getFlagged()) {
                isFlagged = true;
                reason = Reason.BLOCK_CATEGORY;
            }

            if (bestCategory.getBlocked()) reason = Reason.BLOCK_CATEGORY;

            if (sess != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_FLAGGED, isFlagged);

            /**
             * Always log an event if the site was categorized
             */
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), bestCategory.getBlocked(), isFlagged, reason, bestCategory.getName(), node.getName());
            node.logEvent(hbe);

            /**
             * If the site was blocked return the nonce
             */
            if (bestCategory.getBlocked()) {
                updateI18nMap();
                String blockReason = I18nUtil.tr(bestCategory.getName(), i18nMap) + " - " + I18nUtil.tr(bestCategory.getDescription(), i18nMap);
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), blockReason, clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            } else {
                return null;
            }
        }

        // No category was found (this should happen rarely as most will return an "Uncategorized" category)
        // Since nothing matched, just log it and return null to allow the visit
        WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, isFlagged, reason, I18nUtil.tr("None", i18nMap), node.getName());
        node.logEvent(hbe);
        return null;
    }

    /**
     * This checks a given response (some items such as mime-type are only known
     * when the response comes) If for any reason the visit is block a nonce is
     * returned. Otherwise null is return and the response is passed
     */
    public String checkResponse(NodeTCPSession sess, InetAddress clientIp, RequestLineToken requestLine, HeaderToken header)
    {
        if (requestLine == null) {
            return null;
        }

        // grab and attach response stuff so it can be used by the filter rules
        String contentType = header.getValue("content-type");
        String fileName = HttpEventHandler.findContentDispositionFilename(header);
        if (sess != null) {
            if (contentType != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_RESPONSE_CONTENT_TYPE, contentType);
            if (fileName != null) sess.globalAttach(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_NAME, fileName);

            // find the last dot to extract the file extension
            int loc = fileName.lastIndexOf(".");
            if (loc != -1) sess.globalAttach(NodeSession.KEY_WEB_FILTER_RESPONSE_FILE_EXTENSION, fileName.substring(loc + 1));
        }

        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("/+", "/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
        }

        String host = UrlMatchingUtil.normalizeHostname(requestLine.getRequestLine().getUrl().getHost());

        // check all of our the block and pass lists 
        if (UrlMatchingUtil.checkClientList(clientIp, node.getSettings().getPassedClients()) != null) return null;
        if (UrlMatchingUtil.checkSiteList(host, uri.toString(), node.getSettings().getPassedUrls()) != null) return null;
        if (checkUnblockedSites(host, uri, clientIp)) return null;

        logger.debug("checkResponse: " + host + uri + " content: " + contentType);

        // not in any of the block or pass lists so check the filter rules
        WebFilterRule filterRule = checkFilterRules(sess, "RESPONSE");

        if ((filterRule != null) && (filterRule.getBlocked())) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.FILTER_RULE, filterRule.getDescription(), node.getName());
            node.logEvent(hbe);
            WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), filterRule.getDescription(), clientIp, node.getNodeTitle());
            return node.generateNonce(bd);
        } else if (filterRule != null && (filterRule.getFlagged())) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.TRUE, Reason.FILTER_RULE, filterRule.getDescription(), node.getName());
            node.logEvent(hbe);
        }

        return null;
    }

    /**
     * Add a specify site to the unblocked list for the specified IP
     */
    public void addUnblockedSite(InetAddress addr, String site)
    {
        HashSet<String> wl;
        synchronized (unblockedDomains) {
            wl = unblockedDomains.get(addr);
            if (null == wl) {
                wl = new HashSet<String>();
                unblockedDomains.put(addr, wl);
            }
        }

        synchronized (wl) {
            wl.add(site);
        }
    }

    /**
     * For each InetAddress in the map, remove the associated host-unblocked
     * sites.
     * 
     * @param map
     *            a Map<InetAddress, List<String>>
     */
    public void removeUnblockedSites(Map<InetAddress, List<String>> map)
    {
        logger.info("about to remove host-unblocked sites for " + map.size() + " host(s)");

        InetAddress addr;
        List<String> unblockedSites;
        HashSet<String> hostSites;

        synchronized (unblockedDomains) {
            for (Map.Entry<InetAddress, List<String>> entry : map.entrySet()) {
                addr = entry.getKey();
                unblockedSites = entry.getValue();

                hostSites = unblockedDomains.get(addr);

                for (String site : unblockedSites) {
                    if (hostSites.contains(site)) {
                        logger.info("Removing unblocked site " + site + " for " + addr);
                        hostSites.remove(site);
                        if (hostSites.isEmpty()) {
                            unblockedDomains.remove(addr);
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
    public void removeAllUnblockedSites()
    {
        unblockedDomains.clear();
    }

    /**
     *
     */
    public WebFilterBase getNode()
    {
        return this.node;
    }

    /**
     * checkUnblockedSites checks the host+uri against the current unblocks for
     * clientIp
     * 
     * @param host
     *            host of the URL
     * @param uri
     *            URI of the URL
     * @param clientIp
     *            IP of the host
     * @return true if the site has been explicitly unblocks for that user,
     *         false otherwise
     */
    private boolean checkUnblockedSites(String host, URI uri, InetAddress clientIp)
    {
        if (isDomainUnblocked(host, clientIp)) {
            logger.debug("LOG: " + host + uri + " in unblock list for " + clientIp);
            return true;
        }

        return false;
    }

    /**
     * Checks the given URL against sites in the block list Returns the given
     * rule if a rule matches, otherwise null
     */
    private GenericRule checkUrlList(String host, String uri, RequestLineToken requestLine)
    {
        logger.debug("checkUrlList( " + host + " , " + uri + " ...)");
        GenericRule rule = UrlMatchingUtil.checkSiteList(host, uri, node.getSettings().getBlockedUrls());

        if (rule == null) return null;

        if (rule.getBlocked()) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_URL, rule.getDescription(), node.getName());
            node.logEvent(hbe);
            return rule;
        } else if (rule.getFlagged()) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.TRUE, Reason.PASS_URL, rule.getDescription(), node.getName());
            node.logEvent(hbe);
            return rule;
        }

        return null;
    }

    /**
     * Check the given URL against the categories (and their settings)
     */
    private GenericRule checkCategory(NodeTCPSession sess, InetAddress clientIp, String host, int port, RequestLineToken requestLine)
    {
        URI reqUri = requestLine.getRequestUri();

        String uri = "";
        if (reqUri.isAbsolute()) {
            host = reqUri.getHost();
            uri = reqUri.normalize().getRawPath();
        } else {
            uri = reqUri.normalize().toString();
        }

        uri = uri.replaceAll("/+", "/");

        logger.debug("checkCategory: " + host + uri);

        List<String> categories = categorizeSite(host, port, uri);

        if (categories == null) {
            logger.warn("NULL categories returned (should be empty list?)");
            categories = new LinkedList<String>();
        }

        boolean isBlocked = false;
        boolean isFlagged = false;
        GenericRule bestCategory = null;

        for (String cat : categories) {
            GenericRule catSettings = node.getSettings().getCategory(cat);
            if (catSettings == null) {
                logger.warn("Missing settings for category: " + cat);
                continue;
            }

            if (bestCategory == null) {
                bestCategory = catSettings;
            }
            /**
             * If this category has more aggressive blocking/flagging than
             * previous category set it to the best category and update flags
             */
            if (!isFlagged && catSettings.getFlagged()) {
                bestCategory = catSettings;
                isFlagged = true;
            }
            /**
             * If this category has more aggressive blocking/flagging than
             * previous category set it to the best category and update flags
             */
            if (!isBlocked && catSettings.getBlocked()) {
                bestCategory = catSettings;
                isBlocked = true;
                isFlagged = true; /* if isBlocked is always Flagged */
            }
        }

        return bestCategory;
    }

    /**
     * Checks whether a given domain has been unblocked for the given address
     */
    private boolean isDomainUnblocked(String domain, InetAddress clientAddr)
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            HashSet<String> unblocks = unblockedDomains.get(clientAddr);
            if (unblocks == null) {
                return false;
            } else {
                for (String d = domain; d != null; d = UrlMatchingUtil.nextHost(d)) {
                    if (unblocks.contains(d)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * If expiraton matches language manager, refresh.
     */
    private void updateI18nMap()
    {
        if ((i18nMapLastUpdated + com.untangle.uvm.LanguageManager.CLEANER_LAST_ACCESS_MAX_TIME - 1000) < System.currentTimeMillis()) {
            i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle");
            i18nMapLastUpdated = System.currentTimeMillis();
        }
    }

    /**
     * Look for any web filter rules that match the session
     */
    private WebFilterRule checkFilterRules(NodeSession sess, String checkCaller)
    {
        if (sess == null) return (null);

        List<WebFilterRule> ruleList = this.node.getSettings().getFilterRules();

        logger.debug("Checking rules against " + checkCaller + " session : " + sess.getProtocol() + " " + sess.getOrigClientAddr().getHostAddress() + ":" + sess.getOrigClientPort() + " -> " + sess.getNewServerAddr().getHostAddress() + ":" + sess.getNewServerPort());

        if (ruleList == null) return null;

        for (WebFilterRule filterRule : ruleList) {
            Boolean result;

            if (!filterRule.getEnabled()) continue;

            result = filterRule.matches(sess);

            if (result == true) {
                logger.debug(checkCaller + " MATCHED WebFilterRule \"" + filterRule.getDescription() + "\"");
                return filterRule;
            }

            else {
                logger.debug(checkCaller + " CHECKED WebFilterRule \"" + filterRule.getDescription() + "\"");
            }
        }

        return null;
    }
}
