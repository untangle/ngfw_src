/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java,v 1.15 2005/03/15 02:11:52 amread Exp $
 */

package com.metavize.tran.spyware;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.TransformContextFactory;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.mvvm.tran.Transform;
import com.metavize.tran.http.HttpStateMachine;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.http.StatusLine;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Header;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenResult;
import com.metavize.tran.util.AsciiCharBuffer;
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;

public class SpywareHttpHandler extends HttpStateMachine
{
    private static final Pattern OBJECT_PATTERN
        = Pattern.compile("<object");
    private static final Pattern CLSID_PATTERN
        = Pattern.compile("clsid:([0-9\\-]*)");

    private static final int SCAN = Transform.GENERIC_0_COUNTER;
    private static final int DETECT = Transform.GENERIC_1_COUNTER;
    private static final int BLOCK = Transform.GENERIC_2_COUNTER;

    private static final Logger logger = Logger
        .getLogger(SpywareHttpHandler.class);

    private final List cookieQueue = new LinkedList();
    private final Queue reqQueue = new LinkedList();
    private final List reqHostQueue = new LinkedList();

    private final Logger eventLogger = MvvmContextFactory.context()
        .eventLogger();

    private SpywareImpl transform;

    private String extension = "";
    private String mimeType = "";
    private RequestLine responseRequest;
    private StatusLine statusLine;

    SpywareHttpHandler(TCPSession session, SpywareImpl transform)
    {
        super(session);

        this.transform = transform;
    }

    protected TokenResult doRequestLine(RequestLine requestLine)
    {
        logger.debug("got request line");
        transform.incrementCount(SCAN);

        reqQueue.offer(requestLine);

        String path = requestLine.getRequestUri().getPath();
        int i = path.lastIndexOf('.');
        extension = (0 <= i && path.length() - 1 > i)
            ? path.substring(i + 1) : null;

        return new TokenResult(null, new Token[] { requestLine });
    }

    protected TokenResult doRequestHeader(Header requestHeader)
    {
        logger.debug("got request header");
        Header h = clientCookie(requestHeader);
        return new TokenResult(null, new Token[] { h });
    }

    protected TokenResult doRequestBody(Chunk chunk)
    {
        logger.debug("got request body");
        return new TokenResult(null, new Token[] { chunk });
    }

    protected TokenResult doRequestBodyEnd(EndMarker endMarker)
    {
        logger.debug("got request body end");
        return new TokenResult(null, new Token[] { endMarker });
    }

    protected TokenResult doStatusLine(StatusLine statusLine)
    {
        this.statusLine = statusLine;

        logger.debug("got status line");

        if (100 != statusLine.getStatusCode()) {
            responseRequest = (RequestLine)reqQueue.remove();
        }

        transform.incrementCount(SCAN);
        return new TokenResult(new Token[] { statusLine }, null);
    }

    protected TokenResult doResponseHeader(Header header)
    {
        logger.debug("got response header");
        mimeType = header.getValue("content-type");

        Header h;
        if (100 != statusLine.getStatusCode()) {
            h = serverCookie(header);
            h  = addCookieKillers( h );
        } else {
            h = header;
        }

        return new TokenResult(new Token[] { h }, null);
    }

    protected TokenResult doResponseBody(Chunk chunk)
    {
        logger.debug("got response body");
        if (null != mimeType && mimeType.equalsIgnoreCase("text/html")) {
            chunk = activeXChunk(chunk);
        }
        return new TokenResult(new Token[] { chunk }, null);
    }

    protected TokenResult doResponseBodyEnd(EndMarker endMarker)
    {
        logger.debug("got response body end");
        return new TokenResult(new Token[] { endMarker }, null);
    }

    // cookie stuff -----------------------------------------------------------

    private Header clientCookie(Header h)
    {
        logger.debug("checking client cookie");

        List cookieKillers = new LinkedList();;
        cookieQueue.add(cookieKillers);

        String host = h.getValue("host");
        List cookies = h.getValues("cookie"); // XXX cookie2 ???

        reqHostQueue.add(host);

        if (null == cookies) {
            return h;
        }

        for (Iterator i = cookies.iterator(); i.hasNext(); ) {
            transform.incrementCount(DETECT);
            String cookie = (String)i.next();
            Map m = CookieParser.parseCookie(cookie);
            String domain = (String)m.get("domain");
            if (null == domain) {
                domain = host;
            }

            long t0 = System.currentTimeMillis();
            boolean badDomain = inDomainsSet(domain);
            long t1 = System.currentTimeMillis();
            if (logger.isDebugEnabled())
                logger.debug("looked up domain in: " + (t1 - t0) + " ms");

            if (badDomain) {
                logger.debug("blocking cookie: " + domain);
                transform.incrementCount(BLOCK);

                eventLogger.info(new SpywareCookieEvent(getSession().id(), responseRequest, domain, true));
                i.remove();
                logger.debug("making cookieKiller: " + domain);
                cookieKillers.addAll(makeCookieKillers(cookie, host));
            }
        }

        return h;
    }

    private Header serverCookie(Header h)
    {
        logger.debug("checking server cookie");
        // XXX if deferred 0ttl cookie, send it and nullify

        String reqDomain = (String)reqHostQueue.remove(0);

        List setCookies = h.getValues("set-cookie");

        if (null == setCookies) { return h; }

        for (Iterator i = setCookies.iterator(); i.hasNext(); ) {
            transform.incrementCount(DETECT);
            String v = (String)i.next();

            logger.debug("handling server cookie: " + v);

            Map m = CookieParser.parseCookie(v);
            String domain = (String)m.get("domain");
            logger.debug("got domain: " + domain);
            if (null == domain) {
                domain = reqDomain;
                logger.debug("using request domain: " + domain);
            }


            boolean badDomain = inDomainsSet(domain);

            if (badDomain) {
                logger.debug("cookie deleted: " + domain);
                transform.incrementCount(BLOCK);
                eventLogger.info(new SpywareCookieEvent(getSession().id(), responseRequest, domain, false));
                i.remove();
            } else {
                logger.debug("cookie not deleted: " + domain);
            }
        }

        return h;
    }

    private Header addCookieKillers( Header h )
    {
        List cookieKillers = (List)cookieQueue.remove(0);
        for (Iterator i = cookieKillers.iterator(); i.hasNext(); ) {
            String killer = (String)i.next();
            logger.debug("adding killer to header: " + killer);
            h.addField("Set-Cookie", killer);
        }

        return h;
    }

    private List makeCookieKillers(String c, String h)
    {
        logger.debug("making cookie killers");
        List l = new LinkedList();

        String cookieKiller = makeCookieKiller(c, h);
        l.add(cookieKiller);

        while (true) {
            cookieKiller = makeCookieKiller(c, "." + h);
            l.add(cookieKiller);
            logger.debug("added cookieKiller: " + cookieKiller);

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

    private Chunk activeXChunk(Chunk c)
    {
        logger.debug("scanning activeX chunk");

        ByteBuffer b = c.getData();
        AsciiCharBuffer cb = AsciiCharBuffer.wrap(b);
        Matcher m = OBJECT_PATTERN.matcher(cb);
        if (m.find()) {
            logger.debug("found activex tag");
            transform.incrementCount(DETECT);
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
                StringRule rule = matchActiveX(clsid);
                long t1 = System.currentTimeMillis();
                logger.debug("looked up activeX in: " + (t1 - t0) + " ms");

                if (null != rule) {
                    block = rule.isLive();
                    ident = rule.getString();
                }

                if (block) {
                    logger.debug("blacklisted classid: " + clsid);
                } else {
                    logger.debug("not blacklisted classid: " + clsid);
                }
            } else {
                ident = "All ActiveX Blocked";
            }

            if (block) {
                logger.debug("blocking activeX");
                transform.incrementCount(BLOCK);
                eventLogger.info(new SpywareActiveXEvent(getSession().id(), responseRequest, ident));
                int len = findEnd(cb, os);
                if (-1 == len) {
                    logger.warn("chunk does not contain entire tag");
                    // XXX cut & buffer from start
                } else {
                    for (int i = 0; i < len; i++) {
                        cb.put(os + i, ' ');
                    }
                }
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

    private boolean inDomainsSet(String domain)
    {
        boolean found = false;

        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();

            for (String h = domain; null != h; h = nextHost(h)) {
                Query q = s.createQuery
                    ("from SpywareSettings ss join ss.cookieRules cookieRules "
                     + "where ss.tid = :tid and cookieRules.string = :domain and cookieRules.live = true");
                q.setParameter("tid", transform.getTid());
                q.setParameter("domain", h);
                Iterator i = q.list().iterator();
                if (i.hasNext()) {
                    found = true;
                    break;
                }
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return found;
    }

    private StringRule matchActiveX(String clsId)
    {
        StringRule rule = null;

        Session s = TransformContextFactory.context().openSession();
        try {
            Transaction tx = s.beginTransaction();
            Query q = s.createQuery
                ("from SpywareSettings ss join ss.activeXRules rules "
                 + "where ss.tid = :tid and rules.string = :clsid and rules.live = true");
            q.setParameter("tid", transform.getTid());
            q.setParameter("clsid", clsId);
            Iterator i = q.list().iterator();
            if (i.hasNext()) {
                Object[] o = (Object[])i.next();
                rule = (StringRule)o[1];
            }

            tx.commit();
        } catch (HibernateException exn) {
            logger.warn("Could not get SpywareSettings", exn);
        } finally {
            try {
                s.close();
            } catch (HibernateException exn) {
                logger.warn("could not close Hibernate session", exn);
            }
        }

        return rule;
    }

    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }
}
