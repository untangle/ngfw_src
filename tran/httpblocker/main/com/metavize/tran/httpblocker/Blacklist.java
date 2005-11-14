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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.metavize.mvvm.tran.IPMaddrRule;
import com.metavize.mvvm.tran.MimeType;
import com.metavize.mvvm.tran.MimeTypeRule;
import com.metavize.mvvm.tran.StringRule;
import com.metavize.tran.http.RequestLineToken;
import com.metavize.tran.token.Header;
import com.metavize.tran.util.CharSequenceUtil;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
class Blacklist
{
    private final Logger logger = Logger.getLogger(Blacklist.class);

    private final HttpBlockerImpl transform;

    private volatile Map<String, CharSequence[]> urls = Collections.emptyMap();
    private volatile Map<String, CharSequence[]> domains = Collections.emptyMap();

    private volatile String[] blockedUrls = new String[0];
    private volatile String[] passedUrls = new String[0];

    private volatile HttpBlockerSettings settings;

    // XXX support expressions

    // constructors -----------------------------------------------------------

    Blacklist(HttpBlockerImpl transform)
    {
        this.transform = transform;

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exn) {
            throw new RuntimeException("could not load Postgres driver", exn);
        }
    }

    // blacklist methods ------------------------------------------------------

    void configure(HttpBlockerSettings settings)
    {
        this.settings = settings;
    }

    synchronized void reconfigure()
    {
        Map<String, CharSequence[]> u = new HashMap<String, CharSequence[]>();
        Map<String, CharSequence[]> d = new HashMap<String, CharSequence[]>();

        BlacklistCache cache = BlacklistCache.cache();

        for (BlacklistCategory cat : (List<BlacklistCategory>)settings.getBlacklistCategories()) {
            String name = cat.getName();
            if (cat.getBlockUrls()) {
                u.put(name, cache.getUrlBlacklist(name));
            }

            if (cat.getBlockDomains()) {
                d.put(name, cache.getDomainBlacklist(name));
            }
        }

        urls = u;
        domains = d;

        blockedUrls = makeCustomList((List<StringRule>)settings.getBlockedUrls());
        passedUrls = makeCustomList((List<StringRule>)settings.getPassedUrls());
    }

    void destroy()
    {
        urls = Collections.emptyMap();
        domains = Collections.emptyMap();
        blockedUrls = new String[0];
        passedUrls = new String[0];
    }

    /**
     * Checks if the request should be blocked, giving an appropriate
     * response if it should.
     *
     * @param host the requested host.
     * @param path the requested path.
     * @return an HTML response.
     */
    String checkRequest(InetAddress clientIp, RequestLineToken requestLine,
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
                (requestLine.getRequestLine(), Action.PASS, Reason.PASS_CLIENT,
                 passCategory);
            logger.info(hbe);
            return null;
        } else {
            String dom = host;
            while (null != dom) {
                String category = findCategory(passedUrls, dom + path,
                                               settings.getPassedUrls());

                if (null != category) {
                    HttpBlockerEvent hbe = new HttpBlockerEvent
                        (requestLine.getRequestLine(), Action.PASS,
                         Reason.PASS_URL, category);
                    transform.log(hbe);

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
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_EXTENSION, exn);
                transform.log(hbe);

                return settings.getBlockTemplate()
                    .render(host, uri, "extension (" + exn + ")");
            }
        }

        return null;
    }

    String checkResponse(InetAddress clientIp, RequestLineToken requestLine,
                         Header header)
    {
        if (null == requestLine) {
            return null;
        } else if (null != passClient(clientIp)) {
            return null;
        }

        String contentType = header.getValue("content-type");

        for (MimeTypeRule rule : (List<MimeTypeRule>)settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType);
                transform.log(hbe);
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

    private String checkBlacklist(String host, RequestLineToken requestLine)
    {
        URI uri = requestLine.getRequestUri();

        StringBuilder sb = new StringBuilder(host);
        sb.reverse();
        sb.append(".");
        String revHost = sb.toString();

        String category = findCategory(domains, revHost);
        Reason reason = null == category ? null : Reason.BLOCK_CATEGORY;

        String dom = host;
        while (null == category && null != dom) {
            String url = dom + uri.toString();
            category = findCategory(urls, url);

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
                (requestLine.getRequestLine(), Action.BLOCK, reason, category);
            transform.log(hbe);

            return settings.getBlockTemplate().render(host, uri, category);
        }

        return null;
    }

    private String findCategory(CharSequence[] strs, String val,
                                List<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private String findCategory(Map<String, CharSequence[]> cats, String val)
    {
        for (String cat : cats.keySet()) {
            CharSequence[] strs = cats.get(cat);
            int i = findMatch(strs, val);
            if (0 <= i) {
                return cat;
            }
        }

        return null;
    }

    private int findMatch(CharSequence[] strs, String val)
    {
        if (null == val || null == strs) {
            return -1;
        }

        int i = Arrays.binarySearch(strs, val, CharSequenceUtil.COMPARATOR);
        if (0 <= i) {
            return i;
        } else {
            int j = -i - 2;
            if (0 <= j && j < strs.length
                && CharSequenceUtil.startsWith(val, strs[j])) {
                return j;
            }
        }

        return -1;
    }

    private String lookupCategory(CharSequence match, List<StringRule> rules)
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
}
