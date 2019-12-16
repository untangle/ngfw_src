/**
 * $Id$
 */
package com.untangle.app.ad_blocker;

import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.app.ad_blocker.cookies.CookieEvent;
import com.untangle.app.ad_blocker.cookies.CookieParser;
import com.untangle.app.http.BlockDetails;
import com.untangle.app.http.HttpEventHandler;
import com.untangle.app.http.HttpRedirect;
import com.untangle.app.http.RequestLineToken;
import com.untangle.app.http.StatusLine;
import com.untangle.uvm.vnet.ChunkToken;
import com.untangle.app.http.HeaderToken;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.app.GenericRule;
import com.untangle.uvm.util.I18nUtil;
import com.untangle.uvm.vnet.AppTCPSession;

/**
 * AD Blocker's HTTP Handler
 * This blocks unwanted (ad) URLS and blocks/remove tracking cookies
 */
public class AdBlockerHandler extends HttpEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private final AdBlockerApp app;

    /**
     * Create an AdBlockerHandler
     * @param app The application containing this handler
     */
    public AdBlockerHandler( AdBlockerApp app )
    {
        super();
        this.app = app;
    }

    /**
     * Handle a new TCP Session
     *
     * @param session The new TCP session
     */
    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        super.handleTCPNewSession( session );
        Map<RequestLineToken, List<String>> killers = new HashMap<>();
        session.attach( killers );
    }
    
    /**
     * Handle a request body token
     * This just passes the chunk, the body is unmodified
     * @param session The TCP session
     * @param token The token
     * @return token The token
     */
    @Override
    protected ChunkToken doRequestBody( AppTCPSession session, ChunkToken token )
    {
        return token;
    }

    /**
     * Handle a request body end
     * This method doesn't take any action
     * @param session The TCP session
     */
    @Override
    protected void doRequestBodyEnd( AppTCPSession session )
    {
    }

    /**
     * Handle a request header
     * This check the URI and sees if the request should be blocked
     * If the request should be blocked it sends a redirect to the block page
     * If the request should not be blocked it is just passed
     * @param session The TCP session
     * @param requestHeader The request header
     * @return the new HeaderToken to pass
     */
    @Override
    protected HeaderToken doRequestHeader( AppTCPSession session, HeaderToken requestHeader )
    {

        app.incrementScanCount();

        HttpRedirect redirect = checkRequest( session, session.getClientAddr(), getRequestLine( session ), requestHeader );
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request redirectDetails: " + redirect);
        }

        if ( redirect == null ) {
            releaseRequest( session );
        } else {
            app.incrementBlockCount();
            blockRequest( session, redirect.getResponse() );
        }

        return requestHeader;
    }

    /**
     * Handle the RequestLineToken
     * This method just passes the token and takes no action
     * @param session The TCP session
     * @param requestLine The RequestLineToken
     * @return the RequestLineToken
     */
    @Override
    protected RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken requestLine )
    {
        return requestLine;
    }

    /**
     * Handle the request body token
     * This method just passes the token and takes no action
     * @param session The TCP session
     * @param token The request body token
     * @return the token
     */
    @Override
    protected ChunkToken doResponseBody( AppTCPSession session, ChunkToken token )
    {
        return token;
    }

    /**
     * Handle the request body end
     * This method takes no action
     * @param session The TCP session
     */
    @Override
    protected void doResponseBodyEnd( AppTCPSession session )
    {
    }

    /**
     * Handle the response header
     * This parses the header and looks for cookies that should be blocked
     * It removes any cookies coming from the server that should be blocked
     * And it also adds "cookie killers" (cookies with 0 timeouts) to remove
     * cookies from the client that were detected in the request header that
     * should not be on the client
     *
     * @param session
     *            The TCP session
     * @param responseHeader
     *            The response header token
     * @return the response header token to send to the client
     */ 
    @Override
    protected HeaderToken doResponseHeader( AppTCPSession session, HeaderToken responseHeader )
    {
        RequestLineToken rl = getResponseRequest( session );
        responseHeader = removeBadCookies( session, rl, responseHeader );
        responseHeader = addCookieKillers( session, rl, responseHeader );

        return responseHeader;
    }

    /**
     * Handle the status line
     * This releases the session from all further processing
     *
     * @param session
     *            The TCP session
     * @param statusLine
     *            The status line
     * @return the status line to send to the client
     */ 
    @Override
    protected StatusLine doStatusLine( AppTCPSession session, StatusLine statusLine )
    {
        releaseResponse( session );
        return statusLine;
    }

    /**
     * Checks if the request should be blocked, giving an appropriate response
     * if it should.
     * 
     * @param session
     *            The TCP session.
     * @param clientIp
     *            The client (source) IP.
     * @param requestLine
     *            The request line token.
     * @param header
     *            The header token
     * @return BlockDetails to force simple page redirect or null it pass.
     */
    private HttpRedirect checkRequest( AppTCPSession session, InetAddress clientIp, RequestLineToken requestLine, HeaderToken header )
    {
        if (!app.getSettings().getScanAds()){
            clientCookie( session, requestLine, header );
            return null;
        }
        
        if (UrlMatchingUtil.checkClientList( clientIp, app.getSettings().getPassedClients()) != null) {
            app.incrementPassCount();
            AdBlockerEvent e = new AdBlockerEvent(Action. PASS, I18nUtil.marktr("client in pass list"), requestLine.getRequestLine() );
            app.logEvent(e);
            return null;
        }

        URI uri = requestLine.getRequestUri().normalize();

        String path = uri.getPath();
        path = null == path ? "" : uri.getPath().toLowerCase();

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }
        host = UrlMatchingUtil.normalizeHostname(host);

        // if any of the general pass rules apply, do no more filtering
        if (checkPassRules(host, requestLine, uri.toString())) {
            return null;
        }

        String request = requestLine.getRequestLine().getUrl().toString();

        GenericRule rule = findMatch(request);
        if (rule != null) {
            if (rule.getBlocked() != null && !rule.getBlocked()) {
                // pass
                app.incrementPassCount();
                AdBlockerEvent e = new AdBlockerEvent(Action.PASS, "", requestLine.getRequestLine());
                app.logEvent(e);
                clientCookie( session, requestLine, header );
                return null;
            } else {
                // block
                AdBlockerEvent event = new AdBlockerEvent(Action.BLOCK, rule.getString(), requestLine.getRequestLine());
                app.logEvent(event);
                return (
                    new HttpRedirect(
                        app.generateResponse( new BlockDetails(host, uri.toString()), session, uri.toString(), header ),
                        HttpRedirect.RedirectType.BLOCK
                ));
            }
        }

        clientCookie( session, requestLine, header );

        return null;
    }

    /**
     * Evaluate the rules against the provided uri
     * It checks the pass rules first, if no pass rule is found
     * it checks the block rules next.
     * @param uri The URI
     * @return the first matching rule or null if no matching rule
     */
    @SuppressWarnings("unused")
    private GenericRule findMatch(String uri)
    {
        if (AdBlockerApp.USE_CACHE && app.getCache().containsKey(uri))
            return app.getCache().get(uri);

        GenericRule result = app.getPassingUrlMatcher().findMatch(uri);
        if (result == null)
            result = app.getBlockingUrlMatcher().findMatch(uri);

        if (AdBlockerApp.USE_CACHE && result != null) {
            if (app.getCache().size() >= AdBlockerApp.MAX_CACHED_ENTRIES) {
                app.getCache().clear();
            }

            app.getCache().put(uri, result);
        }
        return result;
    }

    /**
     * Check the pass rules against the provided host and uri
     * Logs a pass event if the request should be passed explicitly
     * @param host the Host of the request
     * @param requestLine the request line
     * @param uri The URI
     * @return true if passed according to rules, fales otherwise
     */
    private boolean checkPassRules(String host, RequestLineToken requestLine, String uri)
    {
        if (host.contains("untangle")) {
            app.incrementPassCount();
            AdBlockerEvent e = new AdBlockerEvent(Action.PASS, null, requestLine.getRequestLine());
            app.logEvent(e);
            return true;
        }

        GenericRule rule = UrlMatchingUtil.checkSiteList(host, uri, app.getSettings().getPassedUrls());
        String category = (rule != null) ? rule.getDescription() : null;
        if (rule != null) {
            app.incrementPassCount();
            AdBlockerEvent e = new AdBlockerEvent(Action.PASS, category, requestLine.getRequestLine());
            app.logEvent(e);
            return true;
        }
        return false;
    }

    /**
     * This parses the request header and looks for cookies on the clien that should not be present
     * Any cookies provided by the client that should be blocked will be saved in killers hash
     * table attached to the session.
     * Later in the response we will insert a 0-timeout cookie that will force the client to remove
     * these cookies
     *
     * @param session
     *           The TCP session
     * @param requestLine
     *           The Request line token
     * @param headerToken
     *           The request header token
     */
    @SuppressWarnings("unchecked")    
    private void clientCookie( AppTCPSession session, RequestLineToken requestLine, HeaderToken headerToken )
    {
        if (!app.getSettings().getScanCookies())
            return;
        logger.debug("checking client cookie");

        List<String> cookieKillers = new LinkedList<>();

        Map<RequestLineToken, List<String>> killers = (Map<RequestLineToken, List<String>>) session.attachment();
        killers.put(requestLine, cookieKillers);

        String host = headerToken.getValue("host");
        List<String> cookies = headerToken.getValues("cookie"); // XXX cookie2 ???

        if (null == cookies) {
            return;
        }

        for (Iterator<String> i = cookies.iterator(); i.hasNext();) {
            app.incrementScanCount();
            String cookie = i.next();
            Map<String, String> cookieMap = CookieParser.parseCookie(cookie);
            String domain = cookieMap.get("domain");
            if (null == domain) {
                domain = host;
            }

            boolean badDomain = app.isCookieBlocked(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking cookie: " + domain);
                }
                app.incrementBlockCount();
                app.logEvent(new CookieEvent(requestLine.getRequestLine(), domain));
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("making cookieKiller: " + domain);
                }
                cookieKillers.addAll(makeCookieKillers(cookie, host));
            } else {
                app.incrementPassCount();
            }
        }
    }

    /**
     * Make a list of "cookie killers" for killing cookies on the client
     * This adds a cookie killer the invididual cookie, but also subdomains
     * 
     * @param cookie
     *           The cookie to kill
     * @param host
     *           The host that owns the cookie
     * @return a list of cookie killers
     */ 
    private List<String> makeCookieKillers(String cookie, String host)
    {
        logger.debug("making cookie killers");
        List<String> l = new LinkedList<>();

        String cookieKiller = makeCookieKiller(cookie, host);
        l.add(cookieKiller);

        while (true) {
            cookieKiller = makeCookieKiller(cookie, "." + host);
            l.add(cookieKiller);
            if (logger.isDebugEnabled()) {
                logger.debug("added cookieKiller: " + cookieKiller);
            }

            int i = host.indexOf('.');
            if (0 <= i && (i + 1) < host.length()) {
                host = host.substring(i + 1);
                i = host.indexOf('.');
                if (0 > i) {
                    break;
                }
            } else {
                break;
            }
        }

        return l;
    }

    /**
     * Changes the provided cookie specification to a "cookie killer"
     *
     * @param cookie
     *           The original cookie
     * @param host
     *           The host/domain that owns tho cookie
     * @return the new cookie specification that kills the cookie
     */
    private String makeCookieKiller(String cookie, String host)
    {
        cookie = stripMaxAge(cookie);

        int i = cookie.indexOf(';');
        if (0 > i) {
            return cookie + "; path=/; domain=" + host + "; max-age=0";
        } else {
            return cookie.substring(0, i) + "; path=/; domain=" + host + "; max-age=0;" + cookie.substring(i, cookie.length());
        }
    }

    /**
     * Remove the max age specification from the provided cookie
     * @param cookie The original cookie string.
     * @return the new cookie string without the max age
     */
    private String stripMaxAge(String cookie)
    {
        String cl = cookie.toLowerCase();
        int i = cl.indexOf("max-age");
        if (-1 == i) {
            return cookie;
        } else {
            int j = cookie.indexOf(';', i);
            return cookie.substring(0, i) + cookie.substring(j + 1, cookie.length());
        }
    }

    /**
     * Add the cookie killers (attached to session) to the header 
     *
     * @param session
     *           The TCP session
     * @param requestLineToken
     *           The Request line token
     * @param headerToken
     *           The request header token
     * @return the new header token
     */
     @SuppressWarnings("unchecked")    
    private HeaderToken addCookieKillers( AppTCPSession session, RequestLineToken requestLineToken, HeaderToken headerToken )
    {
        Map<RequestLineToken, List<String>> killers = (Map<RequestLineToken, List<String>>) session.attachment();

        List<String> cookieKillers = killers.remove(requestLineToken);
        if (cookieKillers == null) {
            return headerToken;
        }

        for (Iterator<String> i = cookieKillers.iterator(); i.hasNext();) {
            String killer = i.next();
            if (logger.isDebugEnabled()) {
                logger.debug("adding killer to header: " + killer);
            }
            headerToken.addField("Set-Cookie", killer);
        }

        return headerToken;
    }

    /**
     * Remove blocked cookies from the header
     *
     * @param session
     *           The TCP session
     * @param requestLineToken
     *           The Request line token
     * @param headerToken
     *           The request header token
     * @return the new header token
     */
    private HeaderToken removeBadCookies( AppTCPSession session, RequestLineToken requestLineToken, HeaderToken headerToken )
    {
        if (!app.getSettings().getScanCookies())
            return headerToken;
        logger.debug("checking server cookie");
        // XXX if deferred 0ttl cookie, send it and nullify

        String reqDomain = getResponseHost( session );

        if (reqDomain == null) {
            return headerToken;
        }

        List<String> setCookies = headerToken.getValues("set-cookie");

        if (setCookies == null) {
            return headerToken;
        }

        for (Iterator<String> i = setCookies.iterator(); i.hasNext();) {
            app.incrementScanCount();
            String v = i.next();

            if (logger.isDebugEnabled()) {
                logger.debug("handling server cookie: " + v);
            }

            Map<String, String> cookieMap = CookieParser.parseCookie(v);

            String domain = cookieMap.get("domain");
            if (logger.isDebugEnabled()) {
                logger.debug("got domain: " + domain);
            }

            if (null == domain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("NULL domain IN: " + cookieMap);
                    for (String foo : cookieMap.keySet()) {
                        logger.debug("eq " + foo + "? " + foo.equals("domain"));
                    }
                }
                domain = reqDomain;
                if (logger.isDebugEnabled()) {
                    logger.debug("using request domain: " + domain);
                }
            }

            boolean badDomain = app.isCookieBlocked(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie deleted: " + domain);
                }
                app.incrementBlockCount();
                app.logEvent(new CookieEvent(requestLineToken.getRequestLine(), domain));
                i.remove();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie not deleted: " + domain);
                }
                app.incrementPassCount();
            }
        }

        return headerToken;
    }
}
