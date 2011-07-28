/*
 * $HeadURL: svn://chef/work/src/webfilter-base/impl/com/untangle/node/webfilter/DecisionEngine.java $
 */
package com.untangle.node.webfilter;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.apache.log4j.Logger;

import com.untangle.node.http.RequestLineToken;
import com.untangle.node.token.Header;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.node.IPMaskedAddressRule;
import com.untangle.uvm.node.IPMaskedAddress;
import com.untangle.uvm.node.MimeType;
import com.untangle.uvm.node.GenericRule;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.Session;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.event.TCPNewSessionRequestEvent;

/**
 * This is the core functionality of web filter
 * It decides if a site should be blocked, passed, logged, etc based on the settings and categorization.
 *
 * @version 1.0
 */
public abstract class DecisionEngine
{
    private final Logger logger = Logger.getLogger(DecisionEngine.class);

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


    //---------------------------------------------
    //----------------- public  -------------------
    //---------------------------------------------
    
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
     * This must be overridden by a specific implementation
     * Return true if subdomains should be done as seperate lookups.
     * For example if the user visits "foo.example.com"
     * If false, only one lookup takes place of "foo.example.com"
     * If true, two lookups take place of "foo.example.com" and "example.com"
     */
    protected abstract boolean getLookupSubdomains();

    /**
     * Checks if the request should be blocked, giving an appropriate response if it should.
     */
    public String checkRequest( TCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, Header header)
    {
        return checkRequest(sess, clientIp, port, requestLine, header, null);
    }

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
    public String checkRequest( TCPSession sess, InetAddress clientIp, int port, RequestLineToken requestLine, Header header, TCPNewSessionRequestEvent event )
    {
        URI uri = null;
        try {
            uri = new URI(requestLine.getRequestUri().normalize().toString().replaceAll("(?<!:)/+", "/"));
        } catch (URISyntaxException e) {
            logger.error("Could not parse URI '" + uri + "'", e);
        }

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

        String username = (String) sess.globalAttachment(Session.KEY_PLATFORM_ADCONNECTOR_USERNAME);

        // depending on the context, the uri can be either a full
        // hierarchical one, or just the path relative to the host; if
        // we fail here, no biggie, we can totally move on.
        try {
            uri = new URI(uri.getPath());
        } catch(Exception e) {
            logger.debug("Could not parse URI for " + uri.getPath(), e);
        }

        logger.debug("checkRequest: " + host + uri);

        // check client IP address pass list
        // If a client is on the pass list is is passed regardless of any other settings
        String description = checkClientPassList(clientIp);
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Action.PASS, Reason.PASS_CLIENT,description, node.getVendor());
            logger.info(hbe);
            return null;
        }

        // check passlisted rules
        // If a site/URL is on the pass list is is passed regardless of any other settings
        description = checkSitePassList(host,uri);
        if (null != description) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Action.PASS, Reason.PASS_URL, description, node.getVendor());
            logger.debug("LOG: in pass list: " + requestLine.getRequestLine());
            node.log(hbe, host, port, event);
            return null;
        }

        // check unblocks
        // if a site/URL is unblocked already for this specific IP it is passed regardless of any other settings
        if (checkUnblockedSites(host, uri, clientIp)) {
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Action.PASS, Reason.PASS_UNBLOCK, "bypass", node.getVendor());
            logger.debug("LOG: in unblock list: " + requestLine.getRequestLine());
            node.log(hbe, host, port, event);
            return null;
        }

        // if this is HTTP traffic and the request is IP-based and block IP-based browsing is enabled, block this traffic
        if (80 == port && node.getSettings().getBlockAllIpHosts()) {
            if (null == host || IP_PATTERN.matcher(host).matches()) {
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Action.BLOCK, Reason.BLOCK_IP_HOST, host, node.getVendor());
                logger.debug("LOG: block all IPs: " + requestLine.getRequestLine());
                node.log(hbe, host, port, event);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().languageManager().getTranslations("untangle-node-webfilter");
                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(),
                                                                     I18nUtil.tr("host name is an IP address ({0})", host, i18nMap),
                                                                     clientIp, node.getNodeTitle(),
                                                                     (String) sess.globalAttachment(Session.KEY_PLATFORM_ADCONNECTOR_USERNAME));
                return node.generateNonce(bd);
            }
        }

        // Check Block lists
        GenericRule urlRule = checkUrlList(host, uri.toString(), port, requestLine, event);
        if (urlRule != null) {
            WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(), urlRule.getDescription(), clientIp, node.getNodeTitle(), username);
            return node.generateNonce(bd);
        }
        
        // Check Extensions
        // If this extension is blocked, block the request
        for ( GenericRule rule : node.getSettings().getBlockedExtensions()) {
            String exn = "."+rule.getString().toLowerCase();
            if (rule.getEnabled() && path.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking extension " + exn);
                }
                WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), Action.BLOCK,Reason.BLOCK_EXTENSION, exn, node.getVendor());
                logger.debug("LOG: in extensions list: " + requestLine.getRequestLine());
                node.log(hbe, host, port, event);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().languageManager().getTranslations("untangle-node-webfilter");

                WebFilterBlockDetails bd = new WebFilterBlockDetails (node.getSettings(), host, uri.toString(),
                                                                      I18nUtil.tr("extension ({0})", exn, i18nMap), clientIp,
                                                                      node.getNodeTitle(),
                                                                      (String) sess.globalAttachment(Session.KEY_PLATFORM_ADCONNECTOR_USERNAME));
                return node.generateNonce(bd);
            }
        }

        // Check categories
        String nonce = checkCategory(sess, clientIp, host, port, requestLine, event, username);
        if (nonce != null) {
            return nonce;
        }
        
        /* XXX */
        // need to log all categories visits
        // need to log flagged categories
        // need to log flagged sites
        /* XXX */

        WebFilterEvent hbe = new WebFilterEvent( requestLine.getRequestLine(), null, null, null, node.getVendor(), true );
        node.log( hbe, host, port, event );

        return null;
    }

    /**
     * This checks a given response (some items such as mime-type are only known when the response comes)
     * If for any reason the visit is block a nonce is returned.
     * Otherwise null is return and the response is passed
     */
    public String checkResponse( InetAddress clientIp, RequestLineToken requestLine, Header header )
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
        String host = normalizeHostname(requestLine.getRequestLine().getUrl().getHost());

        if (checkClientPassList(clientIp) != null)
            return null;
        if (checkSitePassList(host,uri) != null)
            return null;
        if (checkUnblockedSites(host,uri,clientIp))
            return null;

        logger.debug("checkResponse: " + host + uri + " content: " + contentType);

        // check mime-type list
        for (GenericRule rule : node.getSettings().getBlockedMimeTypes()) {
            MimeType mt = new MimeType(rule.getString());
            if (rule.getEnabled() && mt.matches(contentType)) {
                WebFilterEvent hbe = new WebFilterEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType, node.getVendor());
                logger.debug("LOG: in mimetype list: " + requestLine.getRequestLine());
                node.log(hbe);

                Map<String,String> i18nMap = LocalUvmContextFactory.context().
                    languageManager().getTranslations("untangle-node-webfilter");

                WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri.toString(),
                                                                     I18nUtil.tr("Mime-Type ({0})", contentType, i18nMap),
                                                                     clientIp, node.getNodeTitle(), null);
                return node.generateNonce(bd);
            }
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
                logger.info(".. for address '" + addr + "'");
                unblockedSites = entry.getValue();

                hostSites = unblockedDomains.get(addr);

                for (String site : unblockedSites) {
                    if (hostSites.contains(site)) {
                        logger.info(".... removing unblocked site " + site);
                        hostSites.remove(site);
                        if (hostSites.isEmpty()) {
                            logger.info(".... '" + addr + "' has no more unblocked sites");
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


    //---------------------------------------------
    //----------------- private -------------------
    //---------------------------------------------
    
    /**
     * checkSitePassList checks the host+uri against the pass list
     *
     * @param host host of the URL
     * @param uri URI of the URL
     * @return description of the rule that passlist rule, null if DNE
     */
    private String     checkSitePassList( String host, URI uri )
    {
        String dom;
        for ( dom = host ; null != dom ; dom = nextHost(dom) ) {
            GenericRule sr = findMatchingRule(node.getSettings().getPassedUrls(), dom + uri);
            String category = null == sr ? null : sr.getDescription();

            if (null != category) {
                logger.debug("LOG: "+host+uri+" in pass list");
                return category;
            }
        }

        return null;
    }

    /**
     * checkClientPassList checks the clientIp against the client pass list
     *
     * @param clientIp IP of the host
     * @return description of the rule that passlist rule, null if DNE
     */
    private String     checkClientPassList( InetAddress clientIp )
    {
        for (GenericRule rule : node.getSettings().getPassedClients()) {
            IPMaskedAddress addr = new IPMaskedAddress(rule.getString());
            if (addr.contains(clientIp) && rule.getEnabled()) {
                logger.debug("LOG: "+clientIp+" in client pass list");
                return rule.getDescription();
            }
        }

        return null;
    }

    /**
     * checkUnblockedSites checks the host+uri against the current unblocks for clientIp
     *
     * @param host host of the URL
     * @param uri URI of the URL
     * @param clientIp IP of the host
     * @return true if the site has been explicitly unblocks for that user, false otherwise
     */
    private boolean    checkUnblockedSites( String host, URI uri, InetAddress clientIp )
    {
        String dom;
        for ( dom = host ; null != dom ; dom = nextHost(dom) ) {
            if (isDomainUnblocked(dom, clientIp)) {
                logger.debug("LOG: "+host+uri+" in unblock list for "+ clientIp);
                return true;
            }
        }

        return false;
    }

    /**
     * Checks the given URL against sites in the block list
     */
    private GenericRule checkUrlList( String host, String uri, int port, RequestLineToken requestLine, TCPNewSessionRequestEvent event )
    {
        String dom;
        GenericRule rule = null;
        
        //iterate through domains & subdomains
        for ( dom = host ; dom != null ; dom = nextHost(dom) ) {
            String url = dom + uri;
            rule = findMatchingRule(node.getSettings().getBlockedUrls(), url);
            if (rule != null)
                break;
        }

        if (rule == null)
            return null;
        
        if (rule.getBlocked()) {
            Action a = Action.BLOCK;
            Reason reason = Reason.BLOCK_URL;
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), a, reason, rule.getDescription(), node.getVendor());
            node.log(hbe, host, port, event);
        } else if (rule.getFlagged()) {
            /* FIXME log an event XXX bug #8876 */
        } 

        return null;
    }

    /**
     * Check the given URL against the categories (and their settings)
     */
    private String     checkCategory( TCPSession sess, InetAddress clientIp, String host, int port, RequestLineToken requestLine, TCPNewSessionRequestEvent event, String username )
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

        logger.info("checkCategory: " + host + uri);

        List<String> categories = categorizeSite(host, port, uri);

        if (categories == null) {
            logger.warn("NULL categories returned (should be empty list?)");
            categories = new LinkedList<String>();
        }

        boolean isBlocked = false;
        boolean isFlagged = false;
        GenericRule bestCategory = null;
        String blockedName = null;

        for(String cat : categories) {
            GenericRule catSettings = node.getSettings().getCategory(cat);
            if (catSettings == null) {
                logger.warn("Missing settings for category: " + cat);
                continue;
            }

            if ( bestCategory ==  null ) {
                bestCategory = catSettings;
                logger.info("checkCategory: " + host + uri + " bestCategory: " + bestCategory.getName());
            }
            if ( catSettings.getBlocked() ) {
                isBlocked = true;
                blockedName = catSettings.getName();
            }
            if ( catSettings.getFlagged() ) 
                isFlagged = true;
        }
        
        if (bestCategory != null && sess != null) {
            /**
             * Tag the session with metadata
             */
            sess.globalAttach(node.getVendor()+"-best-category-id",bestCategory.getId());
            sess.globalAttach(node.getVendor()+"-best-category-name",bestCategory.getName());
            sess.globalAttach(node.getVendor()+"-best-category-description",bestCategory.getDescription());
            sess.globalAttach(node.getVendor()+"-best-category-flagged",bestCategory.getFlagged());
            sess.globalAttach(node.getVendor()+"-best-category-blocked",bestCategory.getBlocked());
            sess.globalAttach(node.getVendor()+"-category-flagged",isFlagged);
            sess.globalAttach(node.getVendor()+"-category-blocked",isBlocked);
        }
        
        if (isBlocked) {
            Action a = Action.BLOCK;
            Reason reason = Reason.BLOCK_CATEGORY;
            WebFilterEvent hbe = new WebFilterEvent(requestLine.getRequestLine(), a, reason, blockedName, node.getVendor());
            node.log(hbe, host, port, event);

            WebFilterBlockDetails bd = new WebFilterBlockDetails(node.getSettings(), host, uri, bestCategory.getDescription(), clientIp, node.getNodeTitle(), username);
            return node.generateNonce(bd);
        } else if (isFlagged) {
            /* FIXME log an event XXX bug #8876 */
        }

        return null;
    }

    
    /**
     * Finds a matching active rule from the ruleset that matches the given value
     */
    private GenericRule findMatchingRule( List<GenericRule> rules, String value )
    {
        String oldVal = value;
        value = normalizeDomain(value);
        
        logger.debug("findMatchRule: rules = '" + rules +"', value = '" + value + "' (normalized from '" + oldVal + ";");

        for (GenericRule rule : rules) {
            if (!rule.getEnabled()) continue;
            
            String re = rule.getString();
            // remove potential '\*\.?' at the beginning
            re = re.replaceAll("^"+Pattern.quote("*."), "");
            // next 3: transform globbing operators into regex ones
            re = re.replaceAll(Pattern.quote("."), "\\.");
            re = re.replaceAll(Pattern.quote("*"), ".*");
            re = re.replaceAll(Pattern.quote("?"), ".");
            // possibly some path after a domain name... People
            // specifying 'google.com' certainly want to block
            // '"google.com/whatever"
            re = re + "(/.*)?";

            // match
            try {
                if (Pattern.matches(re, value)) {
                    logger.debug("findMatchRule: ** matches pattern '" + re + "'");
                    return rule; // done, we do not care if others match too
                } else {
                    logger.debug("findMatchRule: ** does not match '" + re + "'");		
                }
            } catch (PatternSyntaxException e) {
                logger.error("findMatchRule: ** invalid pattern '" + re + "'");		
            }
            
        }

        return null;
    }

    /**
     * normalize the hostname
     *
     * @param host host of the URL
     * @return the normalized string for that hostname, or null if param is null
     */
    private String     normalizeHostname( String oldhost )
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
     * Gets the next domain stripping off the lowest level domain from
     * host. Does not return the top level domain. Returns null when
     * no more domains are left.
     *
     * <b>This method assumes trailing dots are stripped from host.</b>
     *
     * @param host a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String     nextHost( String host )
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
     * normalizes a domain name
     * removes extra "http://" or "www." or "." at the beginning
     */
    private String     normalizeDomain( String dom )
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
     * Checks whether a given domain has been unblocked for the given address
     */
    private boolean    isDomainUnblocked( String domain, InetAddress clientAddr )
    {
        if (null == domain) {
            return false;
        } else {
            domain = domain.toLowerCase();

            Set<String> unblocks = unblockedDomains.get(clientAddr);
            if (unblocks == null) {
                return false;
            } else {
                for ( String d = domain ; d != null ; d = nextHost(d) ) {
                    if (unblocks.contains(d)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
