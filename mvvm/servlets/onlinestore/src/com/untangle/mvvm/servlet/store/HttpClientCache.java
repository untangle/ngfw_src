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

package com.untangle.mvvm.servlet.store;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.MvvmLocalContext;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;

class HttpClientCache
{
    private static final String HTTP_CLIENT = "httpClient";
    private static final String HTTPCLIENT_ID = "HTTPCLIENTID";

    private final Map<String, Reference<HttpClient>> clients
        = new HashMap<String, Reference<HttpClient>>();
    private final Map<Reference<HttpClient>, String> keys
        = new HashMap<Reference<HttpClient>, String>();
    private final ReferenceQueue queue = new ReferenceQueue<HttpClient>();
    private final Random random = new Random();
    private final String cookieDomain;
    private final Logger logger = Logger.getLogger(getClass());

    HttpClientCache(String cookieDomain)
    {
        this.cookieDomain = cookieDomain;
    }

    HttpClient getClient(HttpServletRequest req, HttpServletResponse resp)
    {
        HttpSession s = req.getSession();

        HttpClient client = (HttpClient)s.getAttribute(HTTP_CLIENT);

        if (null != client) {
            MvvmLocalContext ctx = MvvmContextFactory.context();
            String boxKey = ctx.getActivationKey();
            for (org.apache.commons.httpclient.Cookie c : client.getState().getCookies()) {
                if (c.getName().equals("boxkey") && !c.getValue().equals(boxKey)) {
                    client = null;
                    break;
                }
            }
        }

        if (null == client) {
            synchronized (this) {
                client = (HttpClient)s.getAttribute(HTTP_CLIENT);

                if (null == client) {
                    List<Cookie> cookies = getStoreCookies(req);

                    for (Cookie c : cookies) {
                        if (c.getName().equals(HTTPCLIENT_ID)) {
                            String key = c.getValue();
                            client = getCachedClient(key);
                            if (null != client) {
                                break;
                            }
                        }
                    }

                    if (null == client) {
                        cleanCache();
                        Cookie c;
                        if (0 < cookies.size()) {
                            c = cookies.get(0);
                        } else {
                            c = addStoreCookie(resp);
                        }
                        String key = c.getValue();
                        client = makeNewClient();
                        cacheClient(key, client);
                    }
                }

                s.setAttribute(HTTP_CLIENT, client);
            }
        }

        return client;
    }

    private HttpClient makeNewClient()
    {
        HttpClient client = new HttpClient(new MultiThreadedHttpConnectionManager());
        HttpState state = client.getState();
        MvvmLocalContext ctx = MvvmContextFactory.context();
        String boxKey = ctx.getActivationKey();
        state.addCookie(new org.apache.commons.httpclient.Cookie(cookieDomain, "boxkey", boxKey, "/", -1, false));

        return client;
    }

    private List<Cookie> getStoreCookies(HttpServletRequest req)
    {
        Cookie[] cookies = req.getCookies();
        if (null == cookies) {
            return Collections.emptyList();
        }

        List<Cookie> cookieList = new ArrayList(cookies.length);

        for (Cookie c : cookies) {
            if (c.getName().equals(HTTPCLIENT_ID)) {
                cookieList.add(c);
            }
        }

        return cookieList;
    }

    private Cookie addStoreCookie(HttpServletResponse resp)
    {
        String key = Long.toHexString(random.nextLong());
        Cookie c = new Cookie(HTTPCLIENT_ID, key);
        c.setMaxAge(7200);
        c.setPath("/onlinestore");
        resp.addCookie(c);

        return c;
    }

    private HttpClient getCachedClient(String key)
    {
        Reference<HttpClient> ref = clients.get(key);

        return null == ref ? null : ref.get();
    }

    private void cacheClient(String key, HttpClient client)
    {
        Reference<HttpClient> ref = new WeakReference(client, queue);

        keys.put(ref, key);
        clients.put(key, ref);
    }

    private void cleanCache()
    {
        Reference<HttpClient> ref;
        while (null != (ref = queue.poll())) {
            String key = keys.remove(ref);
            if (null == key) {
                logger.warn("unknown client key: " + key);
            } else {
                clients.remove(key);
            }
        }
    }
}
