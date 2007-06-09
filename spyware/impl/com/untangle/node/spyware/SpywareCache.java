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

package com.untangle.node.spyware;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

class SpywareCache
{
    private static final String URL_LIST
        = "com/untangle/tran/spyware/urlblacklist.txt";

    private static final SpywareCache CACHE = new SpywareCache();

    private final Logger logger = Logger.getLogger(SpywareCache.class);

    private WeakReference<Set<String>> urlsRef = null;

    private SpywareCache() { }

    static SpywareCache cache()
    {
        return CACHE;
    }

    Set<String> getUrls()
    {
        Set<String> s = null;

        synchronized (this) {
            if (null != urlsRef) {
                s = urlsRef.get();
            }

            if (null == s) {
                s = buildUrlList();
                urlsRef = new WeakReference(s);
            }
        }

        return s;
    }

    private Set<String> buildUrlList()
    {
        InputStream is = getClass().getClassLoader().getResourceAsStream(URL_LIST);
        Set<String> s = new HashSet<String>();

        if (null == is) {
            logger.error("Could not find: " + URL_LIST);
            return null;
        }

        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            for (String line = br.readLine(); null != line; line = br.readLine()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("ADDING URL: " + line);
                }
                s.add(line);
            }
        } catch (IOException exn) {
            logger.error("Could not read file: " + URL_LIST, exn);
        } finally {
            try {
                is.close();
            } catch (IOException exn) {
                logger.warn("Could not close file: " + URL_LIST, exn);
            }
        }

        return s;
    }
}
