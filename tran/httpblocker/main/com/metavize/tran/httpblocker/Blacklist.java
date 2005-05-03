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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
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

    private final Logger eventLogger = MvvmContextFactory.context()
        .eventLogger();
    private final Logger logger = Logger.getLogger(Blacklist.class);

    private volatile String[] urls = null;
    private volatile String[] urlCats = null;
    private volatile String[] domains = null;
    private volatile String[] domainCats = null;

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
        urlCats = null;
        domains = null;
        domainCats = null;
    }

    void reconfigure()
    {

        tid = settings.getTid().getId();

        Connection c = null;
        try {
            c = DriverManager
                .getConnection("jdbc:postgresql://localhost/blacklist?charSet=SQL_ASCII",
                               "metavize", "foo");

            List domCat = new LinkedList();
            List urlCat = new LinkedList();

            for (Iterator i = settings.getBlacklistCategories().iterator();
                 i.hasNext(); ) {

                BlacklistCategory category = (BlacklistCategory)i.next();

                if (category.getBlockDomains()) {
                    domCat.add(category.getName());
                }

                if (category.getBlockUrls()) {
                    urlCat.add(category.getName());
                }
            }

            StringBuilder domClause = new StringBuilder("FROM domains ");
            if (0 < domCat.size()) {
                domClause.append("WHERE category IN (");
                for (Iterator iter = domCat.iterator(); iter.hasNext(); ) {
                    domClause.append("'");
                    domClause.append(iter.next().toString());
                    domClause.append("'");
                    domClause.append(iter.hasNext() ? ", " : ")");
                }
            } else {
                domClause.append("WHERE 1 != 1");
            }

            Statement s = c.createStatement();

            ResultSet rs = s.executeQuery("SELECT count(*) " + domClause);
            rs.next();
            int count = rs.getInt(1);

            domains = null;
            String[] l1 = new String[count];
            String[] l2 = new String[count];

            rs = s.executeQuery("SELECT domain, category " + domClause
                                + " ORDER BY domain ASC");
            int i = 0;
            while(rs.next()) {
                l1[i] = rs.getString(1);
                l2[i++] = rs.getString(2).intern();;
            }
            domains = l1;
            domainCats = l2;

            StringBuilder urlClause = new StringBuilder("FROM urls ");
            if (0 < urlCat.size()) {
                urlClause.append("WHERE category IN (");
                for (Iterator iter = urlCat.iterator(); iter.hasNext(); ) {
                    urlClause.append("'");
                    urlClause.append(iter.next().toString());
                    urlClause.append("'");
                    urlClause.append(iter.hasNext() ? ", " : ")");
                }
            } else {
                urlClause.append("WHERE 1 != 1");
            }


            rs = s.executeQuery("SELECT count(*) " + urlClause);
            rs.next();
            count = rs.getInt(1);

            urls = null;
            l1 = new String[count];
            l2 = new String[count];

            rs = s.executeQuery("SELECT url, category " + urlClause
                                + " ORDER BY url ASC");
            i = 0;

            while (rs.next()) {
                l1[i] = rs.getString(1);
                l2[i++] = rs.getString(2).intern();
            }
            urls = l1;
            urlCats = l2;

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

        if (passClient(clientIp)) {
            return null;
        } else if (passedUrl(host, path)) {
            return null;
        }

        // check in HttpBlockerSettings
        String blockMsg = checkBlacklist(host, requestLine);

        if (null != blockMsg) {
            return blockMsg;
        }

        // Check Extensions
        for (Iterator i = settings.getBlockedExtensions().iterator();
             i.hasNext(); ) {
            StringRule rule = (StringRule)i.next();
            String exn = rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                logger.debug("blocking extension " + exn);
                eventLogger.info(new HttpBlockerEvent(requestLine,
                                                      Reason.EXTENSION, exn));

                return settings.getBlockTemplate().render(host, uri, "extension");
            }
        }

        return null;
    }

    String checkResponse(InetAddress clientIp, RequestLine requestLine,
                         Header header)
    {
        if (passClient(clientIp)) {
            return null;
        }

        String contentType = header.getValue("content-type");

        for (Iterator i = settings.getBlockedMimeTypes().iterator();
             i.hasNext(); ) {
            MimeTypeRule rule = (MimeTypeRule)i.next();
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches( contentType )) {
                eventLogger.info(new HttpBlockerEvent
                                 (requestLine, Reason.MIME_TYPE, contentType));
                String host = header.getValue("host");
                URI uri = requestLine.getRequestUri();

                return settings.getBlockTemplate().render(host, uri, "mime-type");
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
    private boolean passClient(InetAddress clientIp)
    {
        for (Iterator i = settings.getPassedClients().iterator();
             i.hasNext(); ) {
            IPMaddrRule rule = (IPMaddrRule)i.next();
            if (rule.getIpMaddr().contains(clientIp)) {
                return true;
            }
        }

        return false;
    }

    private String checkBlacklist(String host, RequestLine requestLine)
    {
        URI uri = requestLine.getRequestUri();
        String url = host + uri.toString();

        StringBuilder sb = new StringBuilder(host);
        sb.reverse();
        sb.append(".");
        String revHost = sb.toString();

        String category = findCategory(revHost, domains, domainCats);

        if (null != category) {
            eventLogger.info(new HttpBlockerEvent(requestLine, Reason.DOMAIN,
                                                  category));
            return settings.getBlockTemplate().render(host, uri, category);
        }

        category = findCategory(url, urls, urlCats);

        if (null != category) {
            eventLogger.info(new HttpBlockerEvent(requestLine, Reason.URI,
                                                  category));
            return settings.getBlockTemplate().render(host, uri, category);
        }

        return null;
    }

    private String findCategory(String val, String[] strs, String[] cats)
    {
        if (null == val || null == strs || null == cats) {
            return null;
        }

        int i = Arrays.binarySearch(strs, val);

        if (0 <= i) {
            assert strs[i].equals(val);

            return cats[i];
        } else {
            int j = -i - 2;

            if (0 <= j && j < strs.length && val.startsWith(strs[j])) {
                return cats[j];
            }
        }

        return null;
    }

    private boolean passedUrl(String host, String path)
    {
        return settings.getPassedUrls().contains(host + path);
    }
}
