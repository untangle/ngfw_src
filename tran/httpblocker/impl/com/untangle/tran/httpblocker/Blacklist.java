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

package com.untangle.tran.httpblocker;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.untangle.mvvm.tran.IPMaddrRule;
import com.untangle.mvvm.tran.MimeType;
import com.untangle.mvvm.tran.MimeTypeRule;
import com.untangle.mvvm.tran.StringRule;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.token.Header;
import com.untangle.tran.util.CharSequenceUtil;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
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

        for (BlacklistCategory cat : settings.getBlacklistCategories()) {
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

        blockedUrls = makeCustomList(settings.getBlockedUrls());
        passedUrls = makeCustomList(settings.getPassedUrls());
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
    String checkRequest(InetAddress clientIp,
                        RequestLineToken requestLine, Header header)
    {
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

        host = host.toLowerCase();
        while (0 < host.length() && '.' == host.charAt(host.length() - 1)) {
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
                StringRule sr = findCategory(passedUrls, dom + path,
                                             settings.getPassedUrls());
                String category = null == sr ? null : sr.getCategory();

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
        String nonce = checkBlacklist(host, requestLine);

        if (null != nonce) {
            return nonce;
        }

        // Check Extensions
        for (StringRule rule : settings.getBlockedExtensions()) {
            String exn = rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking extension " + exn);
                }
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_EXTENSION, exn);
                transform.log(hbe);

                BlockDetails bd = new BlockDetails(settings, host, uri,
                                                   "extension (" + exn + ")");
                return transform.generateNonce(bd);
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

        for (MimeTypeRule rule : settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType);
                transform.log(hbe);
                String host = header.getValue("host");
                URI uri = requestLine.getRequestUri().normalize();

                BlockDetails bd = new BlockDetails(settings, host, uri,
                                                   "Mime-Type (" + contentType + ")");
                return transform.generateNonce(bd);
            }
        }

        HttpBlockerEvent e = new HttpBlockerEvent(requestLine.getRequestLine(),
                                                  null, null, null, true);
        transform.log(e);

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
        for (IPMaddrRule rule : settings.getPassedClients()) {
            if (rule.getIpMaddr().contains(clientIp) && rule.isLive()) {
                return rule.getCategory();
            }
        }

        return null;
    }

    private String checkBlacklist(String host,
                                  RequestLineToken requestLine)
    {
        URI uri = requestLine.getRequestUri().normalize();
        String category = null;
        StringRule stringRule = null;
        Reason reason = null;

        if (settings.getFascistMode()) {
            String c = "All Web Content";
            Reason r = Reason.BLOCK_ALL;
            HttpBlockerEvent hbe = new HttpBlockerEvent
                (requestLine.getRequestLine(), Action.BLOCK, r, c);
            transform.log(hbe);

            BlacklistCache cache = BlacklistCache.cache();
            BlockDetails bd = new BlockDetails(settings, host, uri,
                                               "not allowed");
            return transform.generateNonce(bd);
        }

        String dom = host;
        while (null == category && null != dom) {
            String url = dom + uri.toString();

            stringRule = findCategory(blockedUrls, url,
                                      settings.getBlockedUrls());
            category = null == stringRule ? null : stringRule.getCategory();
            if (null != category) {
                reason = Reason.BLOCK_URL;
            } else {
                String[] categories = findCategories(urls, url);
                category = mostSpecificCategory(categories);

                if (null != category) {
                    reason = Reason.BLOCK_CATEGORY;
                }
            }

            if (null == category) {
                dom = nextHost(dom);
            }
        }

        if (null == category) {
            StringBuilder sb = new StringBuilder(host);
            sb.reverse();
            sb.append(".");
            String revHost = sb.toString();
            String[] categories = findCategories(domains, revHost);
            category = mostSpecificCategory(categories);
            reason = null == category ? null : Reason.BLOCK_CATEGORY;
        }

        if (null != category) {
            if (null == stringRule || stringRule.getLog()) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK, reason, category);
                transform.log(hbe);
            }

            BlacklistCategory bc = settings.getBlacklistCategory(category);
            if (null == bc && null != stringRule && !stringRule.isLive()) {
                return null;
            } else if (null != bc && bc.getLogOnly()) {
                return null;
            } else {
                BlockDetails bd = new BlockDetails(settings, host, uri,
                                                   category);
                return transform.generateNonce(bd);
            }
        }

        return null;
    }

    private String mostSpecificCategory(String[] categories) {
        String category = null;
        if (categories != null)
            for (int i = 0; i < categories.length; i++) {
                if (category == null) {
                    category = categories[i];
                } else {
                    BlacklistCategory bc = settings.getBlacklistCategory(categories[i]);
                    if (bc.getLogOnly())
                        continue;
                    category = categories[i];
                }
            }
        return category;
    }

    private StringRule findCategory(CharSequence[] strs, String val,
                                    List<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private String[] findCategories(Map<String, CharSequence[]> cats, String val)
    {
        List<String> result = new ArrayList<String>();
        for (String cat : cats.keySet()) {
            CharSequence[] strs = cats.get(cat);
            int i = findMatch(strs, val);
            if (0 <= i)
                result.add(cat);
        }
        if (result.size() == 0) {
            return null;
        } else {
            return (String[]) result.toArray(new String[result.size()]);
        }
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

    private StringRule lookupCategory(CharSequence match,
                                      List<StringRule> rules)

    {
        for (StringRule rule : rules) {
            String uri = normalizeDomain(rule.getString());

            if ((rule.isLive() || rule.getLog()) && match.equals(uri)) {
                return rule;
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
                String uri = normalizeDomain(rule.getString());
                strings.add(uri);
            }
        }
        Collections.sort(strings);

        return strings.toArray(new String[strings.size()]);
    }

    private String normalizeDomain(String dom)
    {
        String url = dom.toLowerCase();
        String uri = url.startsWith("http://")
            ? url.substring("http://".length()) : url;

        while (0 < uri.length()
               && ('*' == uri.charAt(0) || '.' == uri.charAt(0))) {
            uri = uri.substring(1);
        }

        if (uri.startsWith("www.")) {
            uri = uri.substring("www.".length());
        }

        return uri;
    }
}
