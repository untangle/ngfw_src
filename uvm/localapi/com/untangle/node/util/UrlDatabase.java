/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareHttpHandler.java 8668 2007-01-29 19:17:09Z amread $
 */

package com.untangle.tran.util;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Header;
import org.apache.log4j.Logger;

public class UrlDatabase<T>
{
    private static final int UPDATE_PERIOD = 21600000; // 6 hours

    private final Map<T, UrlList> whitelists = new HashMap<T, UrlList>();
    private final Map<T, UrlList> blacklists = new HashMap<T, UrlList>();

    private final Logger logger = Logger.getLogger(getClass());

    private Timer timer;

    public void clear()
    {
        whitelists.clear();
        blacklists.clear();
    }

    public void addBlacklist(T o, UrlList blacklist)
    {
        blacklists.put(o, blacklist);
    }

    public void addWhitelist(T o, UrlList whitelist)
    {
        whitelists.put(o, whitelist);
    }

    public void updateAll(boolean async)
    {
        for (T o : whitelists.keySet()) {
            UrlList ul = whitelists.get(o);
            if (null != ul) {
                ul.update(async);
            }
        }

        for (T o : blacklists.keySet()) {
            UrlList ul = blacklists.get(o);
            if (null != ul) {
                ul.update(async);
            }
        }
    }

    public UrlDatabaseResult search(TCPSession session, URI uri,
                                    Header requestHeader)
    {
        String host = uri.getHost();
        if (null == host) {
            host = requestHeader.getValue("host");
            if (null == host) {
                InetAddress clientIp = session.clientAddr();
                host = clientIp.getHostAddress();
            }
        }
        host = host.toLowerCase();

        // XXX dont do nextHost() for ipaddresses
        UrlDatabaseResult result = null;
        do {
            result = search("http", host, uri.toString());
        } while (null == result && null != (host = nextHost(host)));

        return result;
    }

    public UrlDatabaseResult search(String proto, String host, String uri)
    {
        for (T o : whitelists.keySet()) {
            UrlList ul = whitelists.get(o);
            if (null == ul) {
                continue;
            }

            if (ul.contains(proto, host, uri)) {
                return new UrlDatabaseResult(false, o);
            }
        }

        for (T o : blacklists.keySet()) {
            UrlList ul = blacklists.get(o);
            if (null == ul) {
                continue;
            }

            if (ul.contains(proto, host, uri)) {
                return new UrlDatabaseResult(true, o);
            }
        }

        return null;
    }

    public List<T> findAllBlacklisted(String proto, String host, String uri)
    {
        List<T> l = null;

        for (T o : blacklists.keySet()) {
            UrlList ul = blacklists.get(o);
            if (null == ul) {
                continue;
            }

            if (ul.contains(proto, host, uri)) {
                if (null == l) {
                    l = new ArrayList<T>();
                }
                l.add(o);
            }
        }

        return l;
    }

    public String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }

    public void startUpdateTimer()
    {
        startUpdateTimer(UPDATE_PERIOD);
    }

    public void startUpdateTimer(long updatePeriod)
    {
        synchronized (this) {
            if (null != timer) {
                timer.cancel();
            }

            timer = new Timer(true);
            TimerTask t = new TimerTask() {
                    public void run()
                    {
                        updateAll(false);
                    }
                };
            timer.scheduleAtFixedRate(t, updatePeriod, updatePeriod);
        }
    }

    public void stopUpdateTimer()
    {
        synchronized (this) {
            if (null != timer) {
                timer.cancel();
            }
            timer = null;
        }
    }
}
