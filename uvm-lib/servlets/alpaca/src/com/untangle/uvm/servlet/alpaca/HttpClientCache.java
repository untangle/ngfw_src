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

package com.untangle.uvm.servlet.alpaca;

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

import com.untangle.uvm.LocalUvmContext;
import com.untangle.uvm.LocalUvmContextFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.log4j.Logger;

/**
 * Keeps a <code>HttpClient</code> for a Servlet Session.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@SuppressWarnings("unchecked")
class HttpClientCache
{
    private static final String HTTP_CLIENT = "httpClient";
    private static final String HTTPCLIENT_ID = "HTTPCLIENTID";

    private final Map<String, Reference<HttpClient>> clients = new HashMap<String, Reference<HttpClient>>();
    private final Map<Reference<HttpClient>, String> keys = new HashMap<Reference<HttpClient>, String>();
    private final ReferenceQueue queue = new ReferenceQueue<HttpClient>();
    private final Random random = new Random();
    private final Logger logger = Logger.getLogger(getClass());

    HttpClientCache()
    {
    }
  
    HttpClient getClient(HttpServletRequest req, HttpServletResponse resp)
    {
        HttpSession s = req.getSession();

        HttpClient client = (HttpClient)s.getAttribute(HTTP_CLIENT);

        boolean resetClient = false;

        if (null == client) {
            synchronized (this) {
                client = (HttpClient)s.getAttribute(HTTP_CLIENT);

                if (null == client) {
                    List<Cookie> cookies = getStoreCookies(req);

                    for (Cookie c : cookies) {
                        if (c.getName().equals(HTTPCLIENT_ID)) {
                            String key = c.getValue();
                            client = getCachedClient(key);
                            if (resetClient) client = null;
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
        LocalUvmContext ctx = LocalUvmContextFactory.context();

        return client;
    }

    private List<Cookie> getStoreCookies(HttpServletRequest req)
    {
        Cookie[] cookies = req.getCookies();
        if (null == cookies) {
            return Collections.emptyList();
        }

        List<Cookie> cookieList = new ArrayList<Cookie>(cookies.length);

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
        c.setPath("/library");
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
