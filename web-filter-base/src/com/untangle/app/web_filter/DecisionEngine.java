/**
 * $Id$
 */

package com.untangle.app.web_filter;

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

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.AppSession;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.app.GenericRule;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.HeaderToken;

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
     * Match  slashes (except for leading protocol) with single slashes
     */
    public static Pattern CONSECUTIVE_SLASHES_URI_PATTERN = Pattern.compile("(?<!:)/+");
    public static Pattern CONSECUTIVE_SLASHES_PATH_PATTERN = Pattern.compile("/+");


    /**
     * This is the base app that owns this decision engine
     */
    private final WebFilterBase app;

    /**
     * Users are able to "unblock" sites if the admin allows it. Unblocked sites
     * are temporary and only stored in memory This map stores a list of
     * unblocked sites by IP address
     */
    private final Map<InetAddress, HashSet<String>> unblockedDomains = new HashMap<InetAddress, HashSet<String>>();

    /**
     * Constructor
     * 
     * @param app
     *        The owner application
     */
    public DecisionEngine(WebFilterBase app)
    {
        this.app = app;
    }

    /**
     * This must be overridden by the specific implementation of the Decision
     * Engine It must return a list of categories (strings) for a given URL
     * 
     * @param dom
     *        The domain (host header)
     * @param uri
     *        The uri of the request
     * @return The list of categories
     */
    protected abstract List<Integer> categorizeSite(String dom, String uri);

    /**
     * Checks if the request should be blocked, giving an appropriate response
     * if it should.
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        IP That made the request.
     * @param port
     *        Port that the request was made to.
     * @param requestLine
     *        The request line token
     * @param header
     *        The header token
     * @return an HTML response (null means the site is passed and no response
     *         is given).
     */
    public String checkRequest(AppTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header)
    {
        /*
         * this stores whether this visit should be flagged for any reason
         */
        Boolean isFlagged = false;
        /*
         * this stores the corresponding reason for the flag/block
         */
        Reason reason = Reason.DEFAULT;
        GenericRule bestCategory = null;
        String bestCategoryStr = null;
        String requestMethod = null;
        String catStr = null;
        URI uri = null;

        try {
            uri = new URI(CONSECUTIVE_SLASHES_URI_PATTERN.matcher(requestLine.getRequestUri().normalize().toString()).replaceAll("/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
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
        bestCategory = checkCategory(sess, clientIp, host, requestLine);
        if (bestCategory != null) bestCategoryStr = bestCategory.getName();

        // tag the session with the metadata
        if (sess != null) {
            sess.globalAttach(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_ID, bestCategory.getId());
            sess.globalAttach(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME, bestCategory.getName());
            sess.globalAttach(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_DESCRIPTION, bestCategory.getDescription());
            sess.globalAttach(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_FLAGGED, bestCategory.getFlagged());
            sess.globalAttach(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_BLOCKED, bestCategory.getBlocked());
        }

        // restrict google applications
        if (app.getSettings().getRestrictGoogleApps()) {
            String allowedDomains = app.getSettings().getRestrictGoogleAppsDomain();
            if (allowedDomains != null &&
                !"".equals(allowedDomains.trim()) && 
                port == 443 &&
                (host.contains("google") || host.contains("youtube"))) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding X-GoogApps-Allowed-Domains header: " + allowedDomains + " to " + host + " port " + port);
                }
                header.addField("X-GoogApps-Allowed-Domains", allowedDomains);
            }
        }

        // check client IP pass list
        // If a client is on the pass list is is passed regardless of any other settings
        GenericRule rule = UrlMatchingUtil.checkClientList(clientIp, app.getSettings().getPassedClients());
        if (rule != null) {
            catStr = (bestCategoryStr == null ? rule.getDescription() : bestCategoryStr);
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_CLIENT, catStr, app.getName());
            logger.debug("LOG: in client pass list: " + requestLine.getRequestLine());
            app.logEvent(hbe);
            return null;
        }

        // check server IP pass list
        // If a site/URL is on the pass list is is passed regardless of any other settings
        rule = UrlMatchingUtil.checkSiteList(host, uri.toString(), app.getSettings().getPassedUrls());
        if (rule != null) {
            catStr = (bestCategoryStr == null ? rule.getDescription() : bestCategoryStr);
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_URL, catStr, app.getName());
            logger.debug("LOG: in site pass list: " + requestLine.getRequestLine());
            app.logEvent(hbe);
            return null;
        }

        String refererHeader = header.getValue("referer");
        if (app.getSettings().getPassReferers() && (refererHeader != null)) {
            try {
                URI refererUri = new URI(CONSECUTIVE_SLASHES_URI_PATTERN.matcher(refererHeader).replaceAll("/"));
                String refererHost = refererUri.getHost();
                if (refererHost == null) {
                    refererHost = host;
                }
                refererHost = UrlMatchingUtil.normalizeHostname(refererHost);

                rule = UrlMatchingUtil.checkSiteList(refererHost, refererUri.getPath().toString(), app.getSettings().getPassedUrls());
                if (rule != null) {
                    catStr = (bestCategoryStr == null ? rule.getDescription() : bestCategoryStr);
                    WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_REFERER_URL, catStr, app.getName());
                    logger.debug("LOG: Referer in pass list: " + requestLine.getRequestLine());
                    app.logEvent(hbe);
                    return null;
                }
            } catch (URISyntaxException e) {
                logger.warn("Could not parse referer URI '" + refererHeader + "' " + e.getClass());
            }
        }

        // check unblocks
        // if a site/URL is unblocked already for this specific IP it is passed regardless of any other settings
        if (checkUnblockedSites(host, uri, clientIp)) {
            catStr = bestCategoryStr;
            if (catStr == null) {
                updateI18nMap();
                catStr = I18nUtil.tr("Unblocked", i18nMap);
            }
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_UNBLOCK, catStr, app.getName());
            logger.debug("LOG: in unblock list: " + requestLine.getRequestLine());
            app.logEvent(hbe);
            return null;
        }

        // if this is HTTP traffic and the request is IP-based and block IP-based browsing is enabled, block this traffic
        if (port == 80 && app.getSettings().getBlockAllIpHosts()) {
            if (host == null || IP_PATTERN.matcher(host).matches()) {
                updateI18nMap();
                catStr = (bestCategoryStr == null ? host : bestCategoryStr);
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_IP_HOST, catStr, app.getName());
                logger.debug("LOG: block all IPs: " + requestLine.getRequestLine());
                app.logEvent(hbe);
                app.incrementFlagCount();

                WebFilterBlockDetails bd = new WebFilterBlockDetails(app.getSettings(), host, uri.toString(), I18nUtil.tr("Host name is an IP address ({0})", host, i18nMap), clientIp, app.getAppTitle());
                return app.generateNonce(bd);
            }
        }

        // Check Block lists
        GenericRule urlRule = checkUrlList(sess, host, uri.toString(), requestLine);
        if (urlRule != null) {
            if (urlRule.getBlocked()) {
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_URL, catStr, app.getName());
                logger.debug("LOG: matched block rule: " + requestLine.getRequestLine());
                app.logEvent(hbe);
                app.incrementFlagCount();

                WebFilterBlockDetails bd = new WebFilterBlockDetails(app.getSettings(), host, uri.toString(), urlRule.getDescription(), clientIp, app.getAppTitle());
                return app.generateNonce(bd);
            } else {
                if (urlRule.getFlagged()) isFlagged = true;

                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, isFlagged, Reason.BLOCK_URL, catStr, app.getName());
                logger.debug("LOG: matched pass rule: " + requestLine.getRequestLine());
                app.logEvent(hbe);
                if (isFlagged) app.incrementFlagCount();
                return null;
            }
        }

        // check the filter rules
        WebFilterRule filterRule = checkFilterRules(sess, "REQUEST");

        /**
         * The filter rules take priority over category so if we find a block or
         * flag rule we log the event and pass or block right here
         */
        if ((filterRule != null) && (filterRule.getBlocked())) {
            catStr = (bestCategoryStr == null ? filterRule.getDescription() : bestCategoryStr);
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.TRUE, Boolean.TRUE, Reason.FILTER_RULE, catStr, app.getName());
            app.logEvent(hbe);
            app.incrementFlagCount();
            WebFilterBlockDetails bd = new WebFilterBlockDetails(app.getSettings(), host, uri.toString(), filterRule.getDescription(), clientIp, app.getAppTitle());
            return app.generateNonce(bd);
        } else if ((filterRule != null) && (filterRule.getFlagged())) {
            catStr = (bestCategoryStr == null ? rule.getDescription() : bestCategoryStr);
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.TRUE, Reason.FILTER_RULE, catStr, app.getName());
            app.logEvent(hbe);
            app.incrementFlagCount();
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

            if (sess != null) sess.globalAttach(AppSession.KEY_WEB_FILTER_FLAGGED, isFlagged);

            /**
             * Always log an event if the site was categorized
             */
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), (bestCategory.getBlocked() ? Boolean.TRUE : Boolean.FALSE), (isFlagged ? Boolean.TRUE : Boolean.FALSE), reason, bestCategory.getName(), app.getName());
            app.logEvent(hbe);
            if (isFlagged) app.incrementFlagCount();

            /**
             * If the site was blocked return the nonce
             */
            if (bestCategory.getBlocked()) {
                updateI18nMap();
                String blockReason = I18nUtil.tr(bestCategory.getName(), i18nMap) + " - " + I18nUtil.tr(bestCategory.getDescription(), i18nMap);
                WebFilterBlockDetails bd = new WebFilterBlockDetails(app.getSettings(), host, uri.toString(), blockReason, clientIp, app.getAppTitle());
                return app.generateNonce(bd);
            } else {
                return null;
            }
        }

        // No category was found (this should happen rarely as most will return an "Uncategorized" category)
        // Since nothing matched, just log it and return null to allow the visit
        WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, (isFlagged ? Boolean.TRUE : Boolean.FALSE), reason, I18nUtil.tr("None", i18nMap), app.getName());
        app.logEvent(hbe);
        if (isFlagged) app.incrementFlagCount();
        return null;
    }

    /**
     * 
     * This checks a given response (some items such as mime-type are only known
     * when the response comes) If for any reason the visit is block a nonce is
     * returned. Otherwise null is return and the response is passed
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        IP that made the request
     * @param requestLine
     *        Request line token
     * @param header
     *        Header token
     * @return Returns a nonce if the visit is blocked, otherwise null
     */
    public String checkResponse(AppTCPSession sess, InetAddress clientIp, RequestLineToken requestLine, HeaderToken header)
    {
        String catStr = null;

        if (requestLine == null) {
            return null;
        }

        URI uri = null;
        try {
            uri = new URI(CONSECUTIVE_SLASHES_PATH_PATTERN.matcher(requestLine.getRequestUri().normalize().toString()).replaceAll("/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
        }

        String host = UrlMatchingUtil.normalizeHostname(requestLine.getRequestLine().getUrl().getHost());

        // check all of our the block and pass lists 
        if (UrlMatchingUtil.checkClientList(clientIp, app.getSettings().getPassedClients()) != null) return null;
        if (UrlMatchingUtil.checkSiteList(host, uri.toString(), app.getSettings().getPassedUrls()) != null) return null;
        if (checkUnblockedSites(host, uri, clientIp)) return null;

        logger.debug("checkResponse: " + host + uri);

        // not in any of the block or pass lists so check the filter rules
        WebFilterRule filterRule = checkFilterRules(sess, "RESPONSE");

        if (sess != null) {
            catStr = (String) sess.globalAttachment(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME);
        }

        if ((filterRule != null) && (filterRule.getBlocked())) {
            if (catStr == null) catStr = filterRule.getDescription();
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.TRUE, Boolean.TRUE, Reason.FILTER_RULE, catStr, app.getName());
            app.logEvent(hbe);
            app.incrementFlagCount();
            WebFilterBlockDetails bd = new WebFilterBlockDetails(app.getSettings(), host, uri.toString(), filterRule.getDescription(), clientIp, app.getAppTitle());
            return app.generateNonce(bd);
        } else if (filterRule != null && (filterRule.getFlagged())) {
            if (catStr == null) catStr = filterRule.getDescription();
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.TRUE, Reason.FILTER_RULE, catStr, app.getName());
            app.logEvent(hbe);
            app.incrementFlagCount();
        }

        return null;
    }

    /**
     * Add a specify site to the unblocked list for the specified IP
     * 
     * @param addr
     *        The site address
     * @param site
     *        The site name
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
     *        a Map<InetAddress, List<String>>
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
     * Get the owner application
     * 
     * @return The owner application
     */
    public WebFilterBase getApp()
    {
        return this.app;
    }

    /**
     * Start the engine.
     */
    public void start(){}

    /**
     * Stop the engine.
     */
    public void stop(){}

    /**
     * Reconfigure the decision engine.
     * @param settings WebFilter settings.
     */
    public void reconfigure(WebFilterSettings settings){}

    /**
     * checkUnblockedSites checks the host+uri against the current unblocks for
     * clientIp
     * 
     * @param host
     *        host of the URL
     * @param uri
     *        URI of the URL
     * @param clientIp
     *        IP of the host
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
     * 
     * @param sess
     *        The session
     * @param host
     *        The host
     * @param uri
     *        The uri
     * @param requestLine
     *        The request line token
     * @return The first matching rule or null
     */
    private GenericRule checkUrlList(AppTCPSession sess, String host, String uri, RequestLineToken requestLine)
    {
        String catStr = null;

        logger.debug("checkUrlList( " + host + " , " + uri + " ...)");
        GenericRule rule = UrlMatchingUtil.checkSiteList(host, uri, app.getSettings().getBlockedUrls());

        if (rule == null) return null;

        if (sess != null) {
            catStr = (String) sess.globalAttachment(AppSession.KEY_WEB_FILTER_BEST_CATEGORY_NAME);
        }

        if (catStr == null) catStr = rule.getDescription();

        if(sess.sessionEvent() == null){
            logger.info("Unable to get sessionEvent() for " + requestLine.getRequestLine());
        }else{
            if (rule.getBlocked()) {
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_URL, catStr, app.getName());
                app.logEvent(hbe);
                app.incrementFlagCount();
                return rule;
            } else if (rule.getFlagged()) {
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), sess.sessionEvent(), Boolean.FALSE, Boolean.TRUE, Reason.PASS_URL, catStr, app.getName());
                app.logEvent(hbe);
                app.incrementFlagCount();
                return rule;
            }
        }

        return null;
    }

    /**
     * Check the given URL against the categories (and their settings)
     * 
     * @param sess
     *        The session
     * @param clientIp
     *        The client address
     * @param host
     *        The destination host
     * @param requestLine
     *        The request line token
     * @return The first matching rule or null
     */
    private GenericRule checkCategory(AppTCPSession sess, InetAddress clientIp, String host, RequestLineToken requestLine)
    {
        URI reqUri = requestLine.getRequestUri();

        String uri = "";
        if (reqUri.isAbsolute()) {
            host = reqUri.getHost();
            uri = reqUri.normalize().getRawPath();
        } else {
            uri = reqUri.normalize().toString();
        }

        uri = CONSECUTIVE_SLASHES_PATH_PATTERN.matcher(uri).replaceAll("/");

        logger.debug("checkCategory: " + host + uri);

        List<Integer> categories = categorizeSite(host, uri);

        if (categories == null) {
            logger.warn("NULL categories returned (should be empty list?)");
            categories = new LinkedList<Integer>();
        }

        boolean isBlocked = false;
        boolean isFlagged = false;
        GenericRule bestCategory = null;

        for (Integer catid : categories) {
            GenericRule catSettings = app.getSettings().getCategory(catid);
            if (catSettings == null) {
                logger.warn("Missing settings for category: " + catid);
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
     * 
     * @param domain
     *        The domain
     * @param clientAddr
     *        The client address
     * @return True if unblocked, otherwise false
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
     * 
     * @param sess
     *        The session
     * @param checkCaller
     *        The caller
     * @return The first matching rule or null
     */
    private WebFilterRule checkFilterRules(AppSession sess, String checkCaller)
    {
        if (sess == null) return (null);

        List<WebFilterRule> ruleList = this.app.getSettings().getFilterRules();

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
