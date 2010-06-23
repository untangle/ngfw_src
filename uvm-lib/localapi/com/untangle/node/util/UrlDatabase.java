/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.untangle.node.token.Header;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Holds blacklists and whitelists comprised of a series of
 * <code>UrlList<code>s that may be queried for an URL.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UrlDatabase<T>
{
    private static final int DEFAULT_UPDATE_PERIOD = 21600000; // 6 hours

    private final Map<T, UrlList> whitelists = new HashMap<T, UrlList>();
    private final Map<T, UrlList> blacklists = new HashMap<T, UrlList>();

    private int updatePeriod = DEFAULT_UPDATE_PERIOD;

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

    public void setUpdatePeriod(int updatePeriod)
    {
        this.updatePeriod = updatePeriod;
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
                return new UrlDatabaseResult<T>(false);
            }
        }

        for (T o : blacklists.keySet()) {
            UrlList ul = blacklists.get(o);
            if (null == ul) {
                continue;
            }

            if (ul.contains(proto, host, uri)) {
                return new UrlDatabaseResult<T>(true);
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
        startUpdateTimer(updatePeriod);
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
