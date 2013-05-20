/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/DecisionEngine.java $
 */
package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.untangle.node.http.RequestLineToken;
import com.untangle.node.token.Header;
import com.untangle.node.util.GlobUtil;
import com.untangle.node.util.UrlMatchingUtil;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.NodeTCPSession;

/**
 * This is the core functionality of web filter
 * It decides if a site should be blocked, passed, logged, etc based on the settings and categorization.
 *
 * @version 1.0
 */
public abstract class DecisionEngine
{
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
     * Users are able to "unblock" sites if the admin allows it.
     * Unblocked sites are temporary and only stored in memory
     * This map stores a list of unblocked sites by IP address
     */
    private final Map<InetAddress, Set<String>> unblockedDomains = new HashMap<InetAddress, Set<String>>();


    public DecisionEngine( WebFilterBase node )
    {
        this.node = node;
    }
    
    /**
     * This must be overridden by the specific implementation of the Decision Engine
     * It must return a list of categories (strings) for a given URL
     */
    protected abstract List<String> categorizeSite( String dom, int port, String uri );
    
    /**
     * Checks if the request should be blocked, giving an appropriate response if it should.
     *
     * @param clientIp IP That made the request.
     * @param port Port that the request was made to.
     * @param requestLine
     * @param header
     * @param event This is the new sessions request associated with this request, (or null if this is later.)
     * @return an HTML response (null means the site is passed and no response is given).
     */
    public String checkRequest( NodeTCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, Header header)
    {
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("(?<!:)/+", "/"));
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

        // check client IP address pass list
        // If a client is on the pass list is is passed regardless of any other settings
        GenericRule rule =  UrlMatchingUtil.checkClientList( clientIp, node.getSettings().getPassedClients());
        String description = (rule != null)? rule.getDescription():null;
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_CLIENT, description, node.getName());
            node.logEvent(hbe);
            return null;
        }

        // check passlisted rules
        // If a site/URL is on the pass list is is passed regardless of any other settings
        rule = UrlMatchingUtil.checkSiteList( host, uri.toString(), node.getSettings().getPassedUrls() );
        description = (rule != null)? rule.getDescription():null;		
        if ( description != null ) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_URL, description, node.getName());
            logger.debug("LOG: in pass list: " + requestLine.getRequestLine());
            node.logEvent(hbe);
            return null;
        }

        // check unblocks
        // if a site/URL is unblocked already for this specific IP it is passed regardless of any other settings
        if ( checkUnblockedSites( host, uri, clientIp ) ) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.FALSE, Reason.PASS_UNBLOCK, "unblocked", node.getName());
            logger.debug("LOG: in unblock list: " + requestLine.getRequestLine());
            node.logEvent(hbe);
            return null;
        }

        // if this is HTTP traffic and the request is IP-based and block IP-based browsing is enabled, block this traffic
        if ( port == 80 && node.getSettings().getBlockAllIpHosts() ) {
            if ( host == null || IP_PATTERN.matcher(host).matches() ) {
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_IP_HOST, host, node.getName());
                logger.debug("LOG: block all IPs: " + requestLine.getRequestLine());
                node.logEvent(hbe);

                Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-webfilter");
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), I18nUtil.tr("host name is an IP address ({0})", host, i18nMap), clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            }
        }

        Boolean isFlagged = false; /* this stores whether this visit should be flagged for any reason */
        Reason reason = Reason.DEFAULT; /* this stores the corresponding reason for the flag/block */
            
        // Check Block lists
        GenericRule urlRule = checkUrlList( host, uri.toString(), port, requestLine );
        if ( urlRule != null ) {
            if ( urlRule.getBlocked() ) {
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), urlRule.getDescription(), clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            }
            else if (!isFlagged && urlRule.getFlagged()) {
                isFlagged = true;
                reason = Reason.BLOCK_URL;
            }
            
        } 
        
        // Check Extensions
        // If this extension is blocked, block the request
        GenericRule extRule = checkExtensionList(host, uri, port, requestLine );
        if (extRule != null) {
            if ( extRule.getBlocked() ) {
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), extRule.getDescription(), clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            } else if ( !isFlagged && extRule.getFlagged() ) {
                isFlagged = true;
                reason = Reason.BLOCK_EXTENSION;
            }
        }

        // Check Categories
        GenericRule bestCategory = checkCategory(sess, clientIp, host, port, requestLine);
        if (bestCategory != null) {
            if (!isFlagged && bestCategory.getFlagged()) {
                isFlagged = true;
                reason = Reason.BLOCK_CATEGORY;
            }
            if (bestCategory.getBlocked())
                reason = Reason.BLOCK_CATEGORY;
            
            if (sess != null) {
                /**
                 * Tag the session with metadata
                 */
                sess.globalAttach(node.getName()+"-best-category-id",bestCategory.getId());
                sess.globalAttach(node.getName()+"-best-category-name",bestCategory.getName());
                sess.globalAttach(node.getName()+"-best-category-description",bestCategory.getDescription());
                sess.globalAttach(node.getName()+"-best-category-flagged",bestCategory.getFlagged());
                sess.globalAttach(node.getName()+"-best-category-blocked",bestCategory.getBlocked());
                sess.globalAttach(node.getName()+"-flagged",isFlagged);
            }
        
            /**
             * Always log an event if the site was categorized
             */
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), bestCategory.getBlocked(), isFlagged, reason, bestCategory.getName(), node.getName());
            node.logEvent(hbe);

            /**
             * If the site was blocked return the nonce
             */
            if (bestCategory.getBlocked()) {
                Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-webfilter");
                String blockReason = I18nUtil.tr(bestCategory.getName(), i18nMap) + " - " + I18nUtil.tr(bestCategory.getDescription(), i18nMap);
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), blockReason, clientIp, node.getNodeTitle());
                return node.generateNonce(bd);
            } else {
                return null;
            }
        } 

        // No category was found (this should happen rarely as most will return an "Uncategorized" category)
        // Since nothing matched, just log it and return null to allow the visit

        WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, isFlagged, reason, "None", node.getName());
        node.logEvent(hbe);
        return null;
    }

    /**
     * This checks a given response (some items such as mime-type are only known when the response comes)
     * If for any reason the visit is block a nonce is returned.
     * Otherwise null is return and the response is passed
     */
    public String checkResponse( NodeTCPSession sess, InetAddress clientIp, RequestLineToken requestLine, Header header )
    {
        if (null == requestLine) {
            return null;
        }

        String contentType = header.getValue("content-type");
        
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("/+", "/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'",e);
        }
        String host = UrlMatchingUtil.normalizeHostname(requestLine.getRequestLine().getUrl().getHost());

        if (UrlMatchingUtil.checkClientList(clientIp, node.getSettings().getPassedClients()) != null)
            return null;
        if (UrlMatchingUtil.checkSiteList(host,uri.toString(), node.getSettings().getPassedUrls()) != null)
            return null;
        if (checkUnblockedSites(host,uri,clientIp))
            return null;

        logger.debug("checkResponse: " + host + uri + " content: " + contentType);

        // check mime-type list

        GenericRule rule = checkMimetypeList(contentType);
        if (rule != null && rule.getBlocked()) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE, Boolean.TRUE, Reason.BLOCK_MIME, contentType, node.getName());
            logger.debug("LOG: in mimetype list: " + requestLine.getRequestLine());
            node.logEvent(hbe);

            Map<String,String> i18nMap = UvmContextFactory.context().languageManager().getTranslations("untangle-node-webfilter");
            WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(),
                                                                 I18nUtil.tr("Mime-Type ({0})", contentType, i18nMap),
                                                                 clientIp, node.getNodeTitle());
            return node.generateNonce(bd);
        } else if (rule != null && rule.getFlagged()) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.TRUE, Reason.BLOCK_MIME, contentType, node.getName());
            logger.debug("LOG: in mimetype list: " + requestLine.getRequestLine());
            node.logEvent(hbe);
        }

        return null;
    }

    /**
     * Add a specify site to the unblocked list for the specified IP
     */
    public void addUnblockedSite( InetAddress addr, String site )
    {
        Set<String> wl;
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
     * For each InetAddress in the map, remove the associated
     * host-unblocked sites.
     *
     * @param map a Map<InetAddress, List<String>>
     */
    public void removeUnblockedSites( Map<InetAddress, List<String>> map )
    {
        logger.info("about to remove host-unblocked sites for "  + map.size() + " host(s)");

        InetAddress addr;
        List<String> unblockedSites;
        Set<String> hostSites;

        synchronized(unblockedDomains) {
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
     * checkUnblockedSites checks the host+uri against the current unblocks for clientIp
     *
     * @param host host of the URL
     * @param uri URI of the URL
     * @param clientIp IP of the host
     * @return true if the site has been explicitly unblocks for that user, false otherwise
     */
    private boolean     checkUnblockedSites( String host, URI uri, InetAddress clientIp )
    {
        String dom;
        for ( dom = host ; null != dom ; dom = UrlMatchingUtil.nextHost(dom) ) {
            if (isDomainUnblocked(dom, clientIp)) {
                logger.debug("LOG: "+host+uri+" in unblock list for "+ clientIp);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks the given URL against sites in the block list
     * Returns the given rule if a rule matches, otherwise null
     */
    private GenericRule checkUrlList( String host, String uri, int port, RequestLineToken requestLine )
    {
        logger.debug("checkUrlList( " + host + " , " + uri + " ...)");
        GenericRule rule = UrlMatchingUtil.checkSiteList(host, uri, node.getSettings().getBlockedUrls());

        if (rule == null)
            return null;

        if (rule.getBlocked()) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE,  Boolean.TRUE, Reason.BLOCK_URL, rule.getDescription(), node.getName());
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
     * Checks the given content type against mime type rules
     * Returns the given rule if a rule matches (that is either blocking or flagging), otherwise null
     */
    private GenericRule checkMimetypeList( String contentType )
    {
        // check mime-type list
        for (GenericRule rule : node.getSettings().getBlockedMimeTypes()) {
            MimeType mt = new MimeType(rule.getString());
            if ((rule.getBlocked() || rule.getFlagged()) && mt.matches(contentType)) {
                return rule;
            }
        }

        return null;
    }
    
    /**
     * Checks the given URL against the file extension rules
     * Returns the given rule if a rule matches, otherwise null
     */
    private GenericRule checkExtensionList( String host, URI fullUri, int port, RequestLineToken requestLine )
    {
        String uri = fullUri.toString();
        try { uri = (new URI(fullUri.getPath())).toString(); /*ignore everything after ?*/ } catch (URISyntaxException e) {}
                
        for ( GenericRule rule : node.getSettings().getBlockedExtensions()) {
            String exn = "." + rule.getString().toLowerCase();
            
            if ((rule.getBlocked() || rule.getFlagged()) && uri.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking/flagging extension " + exn);
                }

                if (rule.getBlocked()) {
                    WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.TRUE,  Boolean.TRUE, Reason.BLOCK_EXTENSION, rule.getDescription(), node.getName());
                    node.logEvent(hbe);
                    return rule;
                } else if (rule.getFlagged()) {
                    WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Boolean.FALSE, Boolean.TRUE, Reason.BLOCK_EXTENSION, rule.getDescription(), node.getName());
                    node.logEvent(hbe);
                    return rule;
                } 
            }
        }

        return null;
    }
    
    /**
     * Check the given URL against the categories (and their settings)
     */
    private GenericRule checkCategory( NodeTCPSession sess, InetAddress clientIp, String host, int port, RequestLineToken requestLine )
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

        for(String cat : categories) {
            GenericRule catSettings = node.getSettings().getCategory(cat);
            if (catSettings == null) {
                logger.warn("Missing settings for category: " + cat);
                continue;
            }

            if ( bestCategory ==  null ) {
                bestCategory = catSettings;
            }
            /**
             * If this category has more aggressive blocking/flagging than previous category
             * set it to the best category and update flags
             */
            if ( !isFlagged && catSettings.getFlagged() ) {
                bestCategory = catSettings;
                isFlagged = true;
            }
            /**
             * If this category has more aggressive blocking/flagging than previous category
             * set it to the best category and update flags
             */
            if ( !isBlocked && catSettings.getBlocked() ) {
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
    private boolean     isDomainUnblocked( String domain, InetAddress clientAddr )
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            Set<String> unblocks = unblockedDomains.get(clientAddr);
            if (unblocks == null) {
                return false;
            } else {
                for ( String d = domain ; d != null ; d = UrlMatchingUtil.nextHost(d) ) {
                    if (unblocks.contains(d)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
