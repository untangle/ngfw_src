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
 * Blocks unwanted HTTP traffic - unwanted Ad URLs
 */
public class AdBlockerHandler extends HttpEventHandler
{
    private final Logger logger = Logger.getLogger(getClass());
    private final AdBlockerApp app;

    public AdBlockerHandler( AdBlockerApp app )
    {
        super();
        this.app = app;
    }

    @Override
    public void handleTCPNewSession( AppTCPSession session )
    {
        super.handleTCPNewSession( session );
        Map<RequestLineToken, List<String>> killers = new HashMap<RequestLineToken, List<String>>();
        session.attach( killers );
    }
    
    @Override
    protected ChunkToken doRequestBody( AppTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doRequestBodyEnd( AppTCPSession session )
    {
    }

    @Override
    protected HeaderToken doRequestHeader( AppTCPSession sess, HeaderToken requestHeader )
    {

        app.incrementScanCount();

        String nonce = checkRequest( sess, sess.getClientAddr(), 80, getRequestLine( sess ), requestHeader );
        if (logger.isDebugEnabled()) {
            logger.debug("in doRequestHeader(): " + requestHeader + "check request returns: " + nonce);
        }

        if ( nonce == null ) {
            releaseRequest( sess );
        } else {
            app.incrementBlockCount();
            String uri = getRequestLine( sess ).getRequestUri().toString();
            Token[] response = app.generateResponse( nonce, sess, uri, requestHeader );

            blockRequest( sess, response );
        }

        return requestHeader;
    }

    @Override
    protected RequestLineToken doRequestLine( AppTCPSession session, RequestLineToken requestLine )
    {
        return requestLine;
    }

    @Override
    protected ChunkToken doResponseBody( AppTCPSession session, ChunkToken c )
    {
        return c;
    }

    @Override
    protected void doResponseBodyEnd( AppTCPSession session )
    {
    }

    @Override
    protected HeaderToken doResponseHeader( AppTCPSession session, HeaderToken responseHeader )
    {
        // releaseResponse();

        logger.debug("got response header");

        RequestLineToken rl = getResponseRequest( session );
        responseHeader = serverCookie( session, rl, responseHeader );
        responseHeader = addCookieKillers( session, rl, responseHeader );

        return responseHeader;
    }

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
     * @param host
     *            the requested host.
     * @param path
     *            the requested path.
     * @return an HTML response.
     */
    private String checkRequest( AppTCPSession session, InetAddress clientIp, int port, RequestLineToken requestLine, HeaderToken header )
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
                return app.generateNonce(new BlockDetails(host, uri.toString()));
            }
        }

        clientCookie( session, requestLine, header );

        return null;
    }

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

    // cookie stuff -----------------------------------------------------------

    @SuppressWarnings("unchecked")    
    private void clientCookie( AppTCPSession session, RequestLineToken requestLine, HeaderToken h )
    {
        if (!app.getSettings().getScanCookies())
            return;
        logger.debug("checking client cookie");

        List<String> cookieKillers = new LinkedList<String>();

        Map<RequestLineToken, List<String>> killers = (Map<RequestLineToken, List<String>>) session.attachment();
        killers.put(requestLine, cookieKillers);

        String host = h.getValue("host");
        List<String> cookies = h.getValues("cookie"); // XXX cookie2 ???

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

    private List<String> makeCookieKillers(String c, String h)
    {
        logger.debug("making cookie killers");
        List<String> l = new LinkedList<String>();

        String cookieKiller = makeCookieKiller(c, h);
        l.add(cookieKiller);

        while (true) {
            cookieKiller = makeCookieKiller(c, "." + h);
            l.add(cookieKiller);
            if (logger.isDebugEnabled()) {
                logger.debug("added cookieKiller: " + cookieKiller);
            }

            int i = h.indexOf('.');
            if (0 <= i && (i + 1) < h.length()) {
                h = h.substring(i + 1);
                i = h.indexOf('.');
                if (0 > i) {
                    break;
                }
            } else {
                break;
            }
        }

        return l;
    }

    private String makeCookieKiller(String c, String h)
    {
        c = stripMaxAge(c);

        int i = c.indexOf(';');
        if (0 > i) {
            return c + "; path=/; domain=" + h + "; max-age=0";
        } else {
            return c.substring(0, i) + "; path=/; domain=" + h + "; max-age=0;" + c.substring(i, c.length());
        }
    }

    private String stripMaxAge(String c)
    {
        String cl = c.toLowerCase();
        int i = cl.indexOf("max-age");
        if (-1 == i) {
            return c;
        } else {
            int j = c.indexOf(';', i);
            return c.substring(0, i) + c.substring(j + 1, c.length());
        }
    }

    @SuppressWarnings("unchecked")    
    private HeaderToken addCookieKillers( AppTCPSession session, RequestLineToken rl, HeaderToken h )
    {
        Map<RequestLineToken, List<String>> killers = (Map<RequestLineToken, List<String>>) session.attachment();

        List<String> cookieKillers = killers.remove(rl);
        if (cookieKillers == null) {
            return h;
        }

        for (Iterator<String> i = cookieKillers.iterator(); i.hasNext();) {
            String killer = i.next();
            if (logger.isDebugEnabled()) {
                logger.debug("adding killer to header: " + killer);
            }
            h.addField("Set-Cookie", killer);
        }

        return h;
    }

    private HeaderToken serverCookie( AppTCPSession session, RequestLineToken rl, HeaderToken h )
    {
        if (!app.getSettings().getScanCookies())
            return h;
        logger.debug("checking server cookie");
        // XXX if deferred 0ttl cookie, send it and nullify

        String reqDomain = getResponseHost( session );

        if (null == reqDomain) {
            return h;
        }

        List<String> setCookies = h.getValues("set-cookie");

        if (null == setCookies) {
            return h;
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
                app.logEvent(new CookieEvent(rl.getRequestLine(), domain));
                i.remove();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie not deleted: " + domain);
                }
                app.incrementPassCount();
            }
        }

        return h;
    }
}
