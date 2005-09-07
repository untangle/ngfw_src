/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import java.net.InetAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.MimeType;
import com.metavize.mvvm.tran.MimeTypeRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.http.RequestLine;
import com.metavize.tran.token.Header;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
class Blacklist
{
    static final Blacklist BLACKLIST = new Blacklist();

    private static final String DB_URL
        = "jdbc:postgresql://localhost/blacklist?charSet=SQL_ASCII";
    private static final String DB_USER = "metavize";
    private static final String DB_PASSWD = "foo";

    private static final String CUSTOM = "custom";

    private final Logger eventLogger = MvvmContextFactory.context()
        .eventLogger();
    private final Logger logger = Logger.getLogger(Blacklist.class);

    private volatile String[] urls = null;
    private volatile String urlClause = null;
    private volatile String[] domains = null;
    private volatile String domClause = null;

    private volatile String[] blockedUrls = null;
    private volatile String[] passedUrls = null;

    private volatile long tid;
    private volatile HttpBlockerSettings settings;

    // XXX support expressions

    // constructors -----------------------------------------------------------

    private Blacklist()
    {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exn) {
            throw new RuntimeException("could not load Postgres driver", exn);
        }
    }

    // blacklist methods ------------------------------------------------------

    void destroy()
    {
        urls = null;
        domains = null;
    }

    void reconfigure()
    {
        tid = settings.getTid().getId();

        Connection c = null;
        try {
            c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWD);

            List<String> domCats = new LinkedList<String>();
            List<String> urlCats = new LinkedList<String>();

            for (BlacklistCategory cat : (List<BlacklistCategory>)settings.getBlacklistCategories()) {
                if (cat.getBlockDomains()) {
                    domCats.add(cat.getName());
                }

                if (cat.getBlockUrls()) {
                    urlCats.add(cat.getName());
                }
            }

            String clause = makeInClause("domains", domCats);

            Statement s = c.createStatement();

            ResultSet rs = s.executeQuery("SELECT count(*) " + clause);
            rs.next();
            int count = rs.getInt(1);

            domains = null;
            String[] l1 = new String[count];

            rs = s.executeQuery("SELECT domain " + clause
                                + " ORDER BY domain ASC");
            int i = 0;
            while(rs.next()) {
                l1[i++] = rs.getString(1);
            }
            domains = l1;
            domClause = clause + " AND domain = ?";

            clause = makeInClause("urls", urlCats);

            rs = s.executeQuery("SELECT count(*) " + clause);
            rs.next();
            count = rs.getInt(1);

            urls = null;
            l1 = new String[count];

            rs = s.executeQuery("SELECT url " + clause + " ORDER BY url ASC");
            i = 0;

            while (rs.next()) {
                l1[i++] = rs.getString(1);
            }

            urls = l1;
            urlClause = clause + " AND url = ?";
        } catch (SQLException exn) {
            logger.warn("could not query uris", exn);
        } finally {
            try {
                if (null != c) {
                    c.close();
                }
            } catch (SQLException exn) {
                logger.warn("could not close connection", exn);
            }
        }

        blockedUrls = makeCustomList((List<StringRule>)settings.getBlockedUrls());
        passedUrls = makeCustomList((List<StringRule>)settings.getPassedUrls());
    }

    synchronized void configure(HttpBlockerSettings settings)
    {
        this.settings = settings;
    }

    /**
     * Checks if the request should be blocked, giving an appropriate
     * response if it should.
     *
     * @param host the requested host.
     * @param path the requested path.
     * @return an HTML response.
     */
    String checkRequest(InetAddress clientIp, RequestLine requestLine,
                        Header header)
    {
        URI uri = requestLine.getRequestUri();

        String path = uri.getPath().toLowerCase();

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }
        host = host.toLowerCase();
        while ('.' == host.charAt(host.length() - 1)) {
            host = host.substring(0, host.length() - 1);
        }

        String passCategory = passClient(clientIp);

        if (null != passCategory) {
            HttpBlockerEvent hbe = new HttpBlockerEvent
                (requestLine, Action.PASS, Reason.PASS_CLIENT, passCategory);
            logger.info(hbe);
            return null;
        } else {
            String dom = host;
            while (null != dom) {
                String category = findCategory(passedUrls, dom + path,
                                               settings.getPassedUrls());

                if (null != category) {
                    HttpBlockerEvent hbe = new HttpBlockerEvent
                        (requestLine, Action.PASS, Reason.PASS_URL, category);
                    eventLogger.info(hbe);

                    return null;
                }
                dom = nextHost(dom);
            }
        }

        // check in HttpBlockerSettings
        String blockMsg = checkBlacklist(host, requestLine);

        if (null != blockMsg) {
            return blockMsg;
        }

        // Check Extensions
        for (StringRule rule : (List<StringRule>)settings.getBlockedExtensions()) {
            String exn = rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                logger.debug("blocking extension " + exn);
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine, Action.BLOCK, Reason.BLOCK_EXTENSION, exn);
                eventLogger.info(hbe);

                return settings.getBlockTemplate()
                    .render(host, uri, "extension (" + exn + ")");
            }
        }

        return null;
    }

    String checkResponse(InetAddress clientIp, RequestLine requestLine,
                         Header header)
    {
        if (null != passClient(clientIp)) { // we only log on the request
            return null;
        }

        String contentType = header.getValue("content-type");

        for (MimeTypeRule rule : (List<MimeTypeRule>)settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine, Action.BLOCK, Reason.BLOCK_MIME, contentType);
                eventLogger.info(hbe);
                String host = header.getValue("host");
                URI uri = requestLine.getRequestUri();

                return settings.getBlockTemplate()
                    .render(host, uri, "Mime-Type (" + contentType + ")");
            }
        }

        return null;
    }

    // private methods --------------------------------------------------------

    /**
     * Check if client is whitelisted.
     *
     * @param clientIp address of the client machine.
     * @return true if the client is whitelisted.
     */
    private String passClient(InetAddress clientIp)
    {
        for (IPMaddrRule rule : (List<IPMaddrRule>)settings.getPassedClients()) {
            if (rule.getIpMaddr().contains(clientIp) && rule.isLive()) {
                return rule.getCategory();
            }
        }

        return null;
    }

    private String checkBlacklist(String host, RequestLine requestLine)
    {
        URI uri = requestLine.getRequestUri();

        StringBuilder sb = new StringBuilder(host);
        sb.reverse();
        sb.append(".");
        String revHost = sb.toString();

        String category = findCategory(domains, revHost, domClause);
        Reason reason = null == category ? null : Reason.BLOCK_CATEGORY;

        String dom = host;
        while (null == category && null != dom) {
            String url = dom + uri.toString();
            category = findCategory(urls, url, urlClause);

            if (null != category) {
                reason = Reason.BLOCK_URL;
            } else {
                category = findCategory(blockedUrls, url,
                                        settings.getBlockedUrls());
                if (null != category) {
                    reason = Reason.PASS_URL;
                }
            }

            if (null == category) {
                dom = nextHost(dom);
            }
        }

        if (null != category) {
            HttpBlockerEvent hbe = new HttpBlockerEvent
                (requestLine, Action.BLOCK, reason, category);
            eventLogger.info(hbe);

            return settings.getBlockTemplate().render(host, uri, category);
        }

        return null;
    }

    private String findCategory(String[] strs, String val,
                                List<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private String findCategory(String[] strs, String val, String clause)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], clause);
    }

    private int findMatch(String[] strs, String val)
    {
        if (null == val || null == strs) {
            return -1;
        }

        int i = Arrays.binarySearch(strs, val);
        if (0 <= i) {
            return i;
        } else {
            int j = -i - 2;
            if (0 <= j && j < strs.length && val.startsWith(strs[j])) {
                return j;
            }
        }

        return -1;
    }

    private String lookupCategory(String match, List<StringRule> rules)
    {
        for (StringRule rule : rules) {
            String url = rule.getString().toLowerCase();
            String uri = url.startsWith("http://")
                ? url.substring("http://".length()) : url;

            if (rule.isLive() && match.equals(uri)) {
                return rule.getCategory();
            }
        }

        return null;
    }

    private String lookupCategory(String match, String clause)
    {
        String category = null;

        Connection c = null;
        try {
            c = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWD);

            PreparedStatement ps = c.prepareStatement
                ("SELECT category " + clause);
            ps.setString(1, match);

            ResultSet rs = ps.executeQuery();
            rs.next();
            category = rs.getString(1);
        } catch (SQLException exn) {
            logger.warn("could not query uris", exn);
        } finally {
            try {
                if (null != c) {
                    c.close();
                }
            } catch (SQLException exn) {
                logger.warn("could not close connection", exn);
            }
        }

        return category;
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

    private String[] makeCustomList(List<StringRule> rules)
    {
        List<String> strings = new ArrayList<String>(rules.size());
        for (StringRule rule : rules) {
            if (rule.isLive()) {
                String url = rule.getString().toLowerCase();
                String uri = url.startsWith("http://")
                    ? url.substring("http://".length()) : url;
                strings.add(uri);
            }
        }
        Collections.sort(strings);

        return strings.toArray(new String[strings.size()]);
    }

    private String makeInClause(String table, List<String> cats)
    {
        StringBuilder clause = new StringBuilder("FROM ");
        clause.append(table);
        if (0 < cats.size()) {
            clause.append(" WHERE category IN (");
            for (Iterator<String> i = cats.iterator(); i.hasNext();) {
                clause.append("'");
                clause.append(i.next());
                clause.append("'");
                clause.append(i.hasNext() ? ", " : ")");
            }
        } else {
            clause.append("WHERE 1 != 1");
        }

        return clause.toString();
    }
}
