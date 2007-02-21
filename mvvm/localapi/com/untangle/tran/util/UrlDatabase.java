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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.sleepycat.je.DatabaseException;
import org.apache.log4j.Logger;

public class UrlDatabase<T>
{
    private final Map<T, UrlList> whitelists = new HashMap<T, UrlList>();
    private final Map<T, UrlList> blacklists = new HashMap<T, UrlList>();

    private final Logger logger = Logger.getLogger(getClass());

    public void addBlacklist(T o, UrlList blacklist)
    {
        blacklists.put(o, blacklist);
    }

    public void addWhitelist(T o, UrlList whitelist)
    {
        whitelists.put(o, whitelist);
    }

    public void updateAll()
    {
        try {
            for (T o : whitelists.keySet()) {
                whitelists.get(o).update();
            }

            for (T o : blacklists.keySet()) {
                blacklists.get(o).update();
            }
        } catch (IOException exn) {
            logger.warn("could not update db", exn);
        } catch (DatabaseException exn) {
            logger.warn("could not update db", exn);
        }
    }

    public UrlDatabaseResult search(String proto, String host, String uri)
    {
        for (T o : whitelists.keySet()) {
            UrlList ul = whitelists.get(o);

            if (ul.contains(proto, host, uri)) {
                return new UrlDatabaseResult(false, o);
            }
        }

        for (T o : blacklists.keySet()) {
            UrlList ul = blacklists.get(o);

            if (ul.contains(proto, host, uri)) {
                return new UrlDatabaseResult(true, o);
            }
        }

        return null;
    }

    public String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (0 > i || i == host.lastIndexOf('.')) {
            return null;  /* skip TLD */
        }

        return host.substring(i + 1);
    }
}
