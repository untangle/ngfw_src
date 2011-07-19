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

package com.untangle.node.spyware;

import java.net.InetAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.node.http.HttpStateMachine;
import com.untangle.node.http.RequestLineToken;
import com.untangle.node.http.StatusLine;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.uvm.node.StringRule;
import com.untangle.uvm.vnet.TCPSession;
import org.apache.log4j.Logger;


public class SpywareHttpHandler extends HttpStateMachine
{
    private static final Pattern OBJECT_PATTERN = Pattern.compile("<object", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLSID_PATTERN  = Pattern.compile("clsid:([0-9\\-]*)", Pattern.CASE_INSENSITIVE);

    private final TCPSession session;

    private final Map<RequestLineToken, List<String>> killers = new HashMap<RequestLineToken, List<String>>();

    private final Logger logger = Logger.getLogger(getClass());

    private final SpywareImpl node;

    private String mimeType = "";

    // constructors -----------------------------------------------------------

    SpywareHttpHandler(TCPSession session, SpywareImpl node)
    {
        super(session);

        this.node = node;
        this.session = session;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        logger.debug("got request line");

        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        logger.debug("got request header");

        // XXX we could check the request-uri for an absolute address too...
        RequestLineToken requestLine = getRequestLine();
        URI uri = requestLine.getRequestUri();

        // XXX this code should be factored out
        String host = uri.getHost();
        if (null == host) {
            host = requestHeader.getValue("host");
            if (null == host) {
                InetAddress clientIp = getSession().clientAddr();
                host = clientIp.getHostAddress();
            }
        }
        host = host.toLowerCase();

        node.incrementHttpScan();
        if (node.isDomainPasslisted(host, session.clientAddr())) {
            node.incrementHttpWhitelisted();
            node.statisticManager.incrPass(); // pass URL
            getSession().release();
            releaseRequest();
            return requestHeader;
        } else if (node.isUrlBlocked(host, uri)) {
            node.incrementHttpBlockedDomain();
            node.statisticManager.incrURL();
            node.log(new SpywareBlacklistEvent(requestLine.getRequestLine()));
            // XXX we could send a page back instead, this isn't really right
            logger.debug("detected spyware, shutting down");

            InetAddress addr = getSession().clientAddr();
            String uriStr = uri.toString();
            SpywareBlockDetails bd = new SpywareBlockDetails(host, uriStr, addr);
            Token[] resp = node.generateResponse(bd, getSession(),
                                                      uriStr, requestHeader,
                                                      isRequestPersistent());
            blockRequest(resp);
            return requestHeader;
        } else {
            node.incrementHttpPassed();
            releaseRequest();
            return clientCookie(requestLine, requestHeader);
        }
    }

    @Override
    protected Chunk doRequestBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doRequestBodyEnd() { }

    @Override
    protected StatusLine doStatusLine(StatusLine statusLine)
    {
        releaseResponse();
        return statusLine;
    }

    @Override
    protected Header doResponseHeader(Header header)
    {
        logger.debug("got response header");
        mimeType = header.getValue("content-type");

        RequestLineToken rl = getResponseRequest();
        header = serverCookie(rl, header);
        header = addCookieKillers(rl, header);

        return header;
    }

    @Override
    protected Chunk doResponseBody(Chunk chunk)
    {
        return chunk;
    }

    @Override
    protected void doResponseBodyEnd()
    {
        logger.debug("got response body end");
    }

    // cookie stuff -----------------------------------------------------------

    private Header clientCookie(RequestLineToken requestLine, Header h)
    {
        logger.debug("checking client cookie");

        List<String> cookieKillers = new LinkedList<String>();;
        killers.put(requestLine, cookieKillers);

        String host = h.getValue("host");
        List<String> cookies = h.getValues("cookie"); // XXX cookie2 ???

        if (null == cookies) {
            return h;
        }

        for (Iterator<String> i = cookies.iterator(); i.hasNext(); ) {
            node.incrementHttpClientCookieScan();
            String cookie = i.next();
            Map<String, String> m = CookieParser.parseCookie(cookie);
            String domain = m.get("domain");
            if (null == domain) {
                domain = host;
            }

            boolean badDomain = node.isCookieBlocked(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking cookie: " + domain);
                }
                node.incrementHttpClientCookieBlock();
                node.statisticManager.incrCookie();
                node.log(new SpywareCookieEvent(requestLine.getRequestLine(), domain, true));
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("making cookieKiller: " + domain);
                }
                cookieKillers.addAll(makeCookieKillers(cookie, host));
            } else {
                node.incrementHttpClientCookiePass();
                node.statisticManager.incrPass(); // pass cookie
            }
        }

        return h;
    }

    private Header serverCookie(RequestLineToken rl, Header h)
    {
        logger.debug("checking server cookie");
        // XXX if deferred 0ttl cookie, send it and nullify

        String reqDomain = getResponseHost();

        if (null == reqDomain) {
            return h;
        }

        List<String> setCookies = h.getValues("set-cookie");

        if (null == setCookies) { return h; }

        for (Iterator<String> i = setCookies.iterator(); i.hasNext(); ) {
            node.incrementHttpServerCookieScan();
            String v = i.next();

            if (logger.isDebugEnabled()) {
                logger.debug("handling server cookie: " + v);
            }

            Map<String, String> m = CookieParser.parseCookie(v);

            String domain = m.get("domain");
            if (logger.isDebugEnabled()) {
                logger.debug("got domain: " + domain);
            }

            if (null == domain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("NULL domain IN: " + m);
                    for (String foo : m.keySet()) {
                        logger.debug("eq " + foo + "? "
                                     + foo.equals("domain"));
                    }
                }
                domain = reqDomain;
                if (logger.isDebugEnabled()) {
                    logger.debug("using request domain: " + domain);
                }
            }

            boolean badDomain = node.isCookieBlocked(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie deleted: " + domain);
                }
                node.incrementHttpServerCookieBlock();
                node.statisticManager.incrCookie();
                node.log(new SpywareCookieEvent(rl.getRequestLine(), domain, false));
                i.remove();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie not deleted: " + domain);
                }
                node.incrementHttpServerCookiePass();
                node.statisticManager.incrPass(); // pass cookie
            }
        }

        return h;
    }

    private Header addCookieKillers(RequestLineToken rl, Header h)
    {
        List<String> cookieKillers = killers.remove(rl);
        if (null == cookieKillers) {
            return h;
        }

        for (Iterator<String> i = cookieKillers.iterator(); i.hasNext(); ) {
            String killer = i.next();
            if (logger.isDebugEnabled()) {
                logger.debug("adding killer to header: " + killer);
            }
            h.addField("Set-Cookie", killer);
        }

        return h;
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
                if (0 > i) { break; }
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
            return c.substring(0, i) + "; path=/; domain=" + h + "; max-age=0;" +
                c.substring(i, c.length());
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

}
