/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.spyware;

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

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.mvvm.tran.StringRule;
import com.untangle.tran.http.HttpStateMachine;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.http.StatusLine;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.EndMarker;
import com.untangle.tran.token.Header;
import com.untangle.tran.token.Token;
import com.untangle.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class SpywareHttpHandler extends HttpStateMachine
{
    private static final String TEXT_HTML = "text/html";
    private static final String IMAGE_GIF = "image/gif";
    private static final String ACCEPT = "accept";

    private static final Pattern OBJECT_PATTERN
        = Pattern.compile("<object", Pattern.CASE_INSENSITIVE);
    private static final Pattern CLSID_PATTERN
        = Pattern.compile("clsid:([0-9\\-]*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern IMAGE_PATTERN
        = Pattern.compile(".*((jpg)|(jpeg)|(gif)|(png)|(ico))",
                          Pattern.CASE_INSENSITIVE);

    private static final byte[] WHITE_GIF = new byte[]
        {
            0x47, 0x49, 0x46, 0x38,
            0x37, 0x61, 0x01, 0x00,
            0x01, 0x00, (byte)0x80, 0x00,
            0x00, (byte)0xff, (byte)0xff, (byte)0xff,
            (byte)0xff, (byte)0xff, (byte)0xff, 0x2c,
            0x00, 0x00, 0x00, 0x00,
            0x01, 0x00, 0x01, 0x00,
            0x00, 0x02, 0x02, 0x44,
            0x01, 0x00, 0x3b
        };

    private static final String REDIRECT_BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<script id='untangleDetect' type='text/javascript'>\n"
        + "var e = document.getElementById(\"untangleDetect\")\n"
        + "if (window == window.top && e.parentNode.tagName == \"BODY\") {\n"
        + "  window.location.href = '%s';"
        + "}\n"
        + "</script>"
        + "</BODY></HTML>";

    // XXX, someone,, make, this, pretty,
    private static final String SIMPLE_BLOCK_TEMPLATE
        = "<HTML><HEAD>"
        + "<TITLE>403 Forbidden</TITLE>"
        + "</HEAD><BODY>"
        + "<script id='metavizeDetect' type='text/javascript'>\n"
        + "var e = document.getElementById(\"metavizeDetect\")\n"
        + "if (window == window.top && e.parentNode.tagName == \"BODY\") {\n"
        + "  document.writeln(\"<center><b>Metavize Spyware Blocker</b></center>\")\n"
        + "  document.writeln(\"<p>This site blocked because it may be a spyware site.</p>\")\n"
        + "  document.writeln(\"<p>Host: %s</p>\")\n"
        + "  document.writeln(\"<p>URI: %s</p>\")\n"
        + "  document.writeln(\"<p>Please contact your network administrator.</p>\")\n"
        + "  document.writeln(\"<HR>\")\n"
        + "  document.writeln(\"<ADDRESS>Untangle</ADDRESS>\")\n"
        + "}\n"
        + "</script>"
        + "</BODY></HTML>";

    private final TCPSession session;

    private final Map<RequestLineToken, List<String>> killers
        = new HashMap<RequestLineToken, List<String>>();

    private final Logger logger = Logger.getLogger(getClass());

    private final SpywareImpl transform;

    private String extension = "";
    private String mimeType = "";

    // constructors -----------------------------------------------------------

    SpywareHttpHandler(TCPSession session, SpywareImpl transform)
    {
        super(session);

        this.transform = transform;
        this.session = session;
    }

    // HttpStateMachine methods -----------------------------------------------

    @Override
    protected RequestLineToken doRequestLine(RequestLineToken requestLine)
    {
        logger.debug("got request line");

        String path = requestLine.getRequestUri().getPath();
        if (null == path) {
            extension = "";
        } else {
            int i = path.lastIndexOf('.');
            extension = (0 <= i && path.length() - 1 > i)
                ? path.substring(i + 1) : null;
        }

        return requestLine;
    }

    @Override
    protected Header doRequestHeader(Header requestHeader)
    {
        logger.debug("got request header");

        // XXX we could check the request-uri for an absolute address too...
        RequestLineToken requestLine = getRequestLine();
        String host = requestHeader.getValue("host");
        URI uri = requestLine.getRequestUri();

        transform.incrementCount(Spyware.SCAN);
        if (transform.isWhitelistedDomain(host, session.clientAddr())) {
            transform.incrementCount(Spyware.PASS);
            transform.statisticManager.incrPass(); // pass URL
            getSession().release();
            releaseRequest();
            return requestHeader;
        } else if (transform.isBlacklistDomain(host, uri)) {
            transform.incrementCount(Spyware.BLOCK);
            transform.statisticManager.incrURL();
            transform.log(new SpywareBlacklistEvent(requestLine.getRequestLine()));
            // XXX we could send a page back instead, this isn't really right
            logger.debug("detected spyware, shutting down");

            blockRequest(generateResponse(requestHeader, host, uri.toString(),
                                          isRequestPersistent()));
            return requestHeader;
        } else {
            transform.incrementCount(Spyware.PASS);
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
        logger.debug("got response body");
        if (null != mimeType && mimeType.equalsIgnoreCase("text/html")) {
            chunk = activeXChunk(getResponseRequest(), chunk);
        }

        return chunk;
    }

    @Override
    protected void doResponseBodyEnd()
    {
        logger.debug("got response body end");
    }

    // private methods --------------------------------------------------------

    private Token[] generateResponse(Header header, String host, String uri,
                                     boolean persistent)
    {
        Token response[] = new Token[4];

        String contentType;
        ByteBuffer buf;

        Matcher m = IMAGE_PATTERN.matcher(uri);

        if (m.matches() || imagePreferred(header)) {
            buf = generateGif();
            contentType = IMAGE_GIF;
        } else {
            buf = generateHtml(host, uri);
            contentType = TEXT_HTML;
        }

        StatusLine sl = new StatusLine("HTTP/1.1", 403, "Forbidden");
        response[0] = sl;

        Header h = new Header();
        h.addField("Content-Length", Integer.toString(buf.remaining()));
        h.addField("Content-Type", contentType);
        h.addField("Connection", persistent ? "Keep-Alive" : "Close");
        response[1] = h;

        Chunk c = new Chunk(buf);
        response[2] = c;

        response[3] = EndMarker.MARKER;

        return response;
    }

    private boolean imagePreferred(Header header)
    {
        String accept = header.getValue(ACCEPT);

        // firefox uses "image/png, */*;q=0.5" when expecting an image
        // ie uses "*/*" no matter what it expects
        return null != accept && accept.startsWith("image/png");
    }

    private ByteBuffer generateGif()
    {
        byte[] buf = new byte[WHITE_GIF.length];
        System.arraycopy(WHITE_GIF, 0, buf, 0, buf.length);
        ByteBuffer bb = ByteBuffer.wrap(buf);

        return bb;
    }


    private ByteBuffer generateHtml(String host, String uri)
    {
        InetAddress addr = MvvmContextFactory.context().networkManager()
            .getInternalHttpAddress(getSession());
        if (null == addr) {
            return generateSimplePage(host, uri);
        } else {
            String redirectHost = addr.getHostAddress();
            return generateRedirect(redirectHost, host, uri);
        }
    }

    private ByteBuffer generateSimplePage(String host, String uri)
    {
        String replacement = String.format(SIMPLE_BLOCK_TEMPLATE, host, uri);

        ByteBuffer buf = ByteBuffer.allocate(replacement.length());
        buf.put(replacement.getBytes()).flip();

        return buf;
    }

    private ByteBuffer generateRedirect(String redirectHost, String host, String uri)
    {
        InetAddress addr = session.clientAddr();
        String nonce = NonceFactory.factory().generateNonce(host, uri, addr);

        String tidStr = transform.getTid().toString();

        String url = "http://" + redirectHost + "/spyware/blockpage.jsp?nonce="
            + nonce + "&tid=" + tidStr;

        String replacement = String.format(REDIRECT_BLOCK_TEMPLATE, url);

        ByteBuffer buf = ByteBuffer.allocate(replacement.length());
        buf.put(replacement.getBytes()).flip();

        return buf;
    }

    // cookie stuff -----------------------------------------------------------

    private Header clientCookie(RequestLineToken requestLine, Header h)
    {
        logger.debug("checking client cookie");

        List<String> cookieKillers = new LinkedList<String>();;
        killers.put(requestLine, cookieKillers);

        String host = h.getValue("host");
        List cookies = h.getValues("cookie"); // XXX cookie2 ???

        if (null == cookies) {
            return h;
        }

        for (Iterator i = cookies.iterator(); i.hasNext(); ) {
            transform.incrementCount(Spyware.SCAN);
            String cookie = (String)i.next();
            Map m = CookieParser.parseCookie(cookie);
            String domain = (String)m.get("domain");
            if (null == domain) {
                domain = host;
            }

            boolean badDomain = transform.isBlockedCookie(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking cookie: " + domain);
                }
                transform.incrementCount(Spyware.BLOCK);
                transform.statisticManager.incrCookie();
                transform.log(new SpywareCookieEvent(requestLine.getRequestLine(), domain, true));
                i.remove();
                if (logger.isDebugEnabled()) {
                    logger.debug("making cookieKiller: " + domain);
                }
                cookieKillers.addAll(makeCookieKillers(cookie, host));
            } else {
                transform.incrementCount(Spyware.PASS);
                transform.statisticManager.incrPass(); // pass cookie
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

        List setCookies = h.getValues("set-cookie");

        if (null == setCookies) { return h; }

        for (Iterator i = setCookies.iterator(); i.hasNext(); ) {
            transform.incrementCount(Spyware.SCAN);
            String v = (String)i.next();

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

            boolean badDomain = transform.isBlockedCookie(domain);

            if (badDomain) {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie deleted: " + domain);
                }
                transform.incrementCount(Spyware.BLOCK);
                transform.statisticManager.incrCookie();
                transform.log(new SpywareCookieEvent(rl.getRequestLine(), domain, false));
                i.remove();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("cookie not deleted: " + domain);
                }
                transform.incrementCount(Spyware.PASS);
                transform.statisticManager.incrPass(); // pass cookie
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

        for (Iterator i = cookieKillers.iterator(); i.hasNext(); ) {
            String killer = (String)i.next();
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

    // ActiveX stuff ----------------------------------------------------------

    private Chunk activeXChunk(RequestLineToken rl, Chunk c)
    {
        logger.debug("scanning activeX chunk");

        ByteBuffer b = c.getData();
        AsciiCharBuffer cb = AsciiCharBuffer.wrap(b);
        Matcher m = OBJECT_PATTERN.matcher(cb);
        if (m.find()) {
            logger.debug("found activex tag");
            int os = m.start();
            m = CLSID_PATTERN.matcher(cb);

            if (!m.find(os)) {
                return c; // not a match
            }

            int cs = m.start();
            int ce = m.end();

            boolean block = transform.getSpywareSettings().getBlockAllActiveX();
            String ident = null;
            if (!block) {
                String clsid = m.group(1);
                long t0 = System.currentTimeMillis();
                StringRule rule = transform.getBlockedActiveX(clsid);
                long t1 = System.currentTimeMillis();
                if (logger.isDebugEnabled()) {
                    logger.debug("looked up activeX in: " + (t1 - t0) + " ms");
                }

                if (null != rule) {
                    transform.incrementCount(Spyware.SCAN);
                    block = rule.isLive();
                    ident = rule.getString();
                }

                if (logger.isDebugEnabled()) {
                    if (block) {
                        logger.debug("blacklisted classid: " + clsid);
                    } else {
                        logger.debug("not blacklisted classid: " + clsid);
                    }
                }
            } else {
                ident = "All ActiveX Blocked";
            }

            if (block) {
                logger.debug("blocking activeX");
                transform.incrementCount(Spyware.BLOCK);
                transform.statisticManager.incrActiveX();
                transform.log(new SpywareActiveXEvent(rl.getRequestLine(), ident));
                int len = findEnd(cb, os);
                if (-1 == len) {
                    logger.warn("chunk does not contain entire tag");
                    // XXX cut & buffer from start
                } else {
                    for (int i = 0; i < len; i++) {
                        cb.put(os + i, ' ');
                    }
                }
            } else {
                transform.incrementCount(Spyware.PASS);
                transform.statisticManager.incrPass(); // pass activeX
            }

            return c;
        } else {
            // no activex
            return c;
        }
    }

    private int findEnd(AsciiCharBuffer cb, int start)
    {
        AsciiCharBuffer dup = cb.duplicate();
        dup.position(dup.position() + start);
        int level = 0;
        while (dup.hasRemaining()) {
            assert 0 <= level;
            char c = dup.get();
            switch (c) {
            case '<':
                if (!dup.hasRemaining()) {
                    return -1;
                } else if ('/' == dup.get(dup.position())) {
                    dup.get();
                    level--;
                    if (0 == level) {
                        while (dup.hasRemaining()) {
                            c = dup.get();
                            if (c == '>') {
                                return dup.position() - (cb.position() + start);
                            }
                        }
                        return -1;
                    }
                } else {
                    level++;
                }
                break;
            case '/':
                if (!dup.hasRemaining()) {
                    return -1;
                } else if ('>' == dup.get(dup.position())) {
                    dup.get();
                    level--;
                }
                break;
            }

            if (0 == level) {
                return dup.position() - (cb.position() + start);
            }
        }

        return -1;
    }
}
