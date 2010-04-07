/*
 * $HeadURL: svn://chef/branch/prod/mawk/work/src/webfilter/impl/com/untangle/node/webfilter/Blacklist.java $
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

package com.untangle.node.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.util.Pulse;

public class HostCache
{
    /**
     * This is a hard-limit on the #domains in the cache
     * If a new entry is added when at this limit the eldest is deleted
     * as per documented in LinkedHashMap
     */
    private final static int CACHE_HARDMAX_ENTRIES = 100000;

    /**
     * The cache size will be reduced to this size when cleaned
     * Note: this is not actually enforced in real-time
     */
    private final static int CACHE_MAX_ENTRIES = 50000;

    /**
     * This is how long entries in the cache will be stored
     */
    private final static long CACHE_TTL = 1000*60*60*24; // 24 hours

    /**
     * The cache will be cleaned every this many milliseconds
     * during cleaned all expired (according to CACHE_TTL_ entries are removed
     */
    private final static long CACHE_CLEAN_FREQUENCY = 60*60*1000L;

    /**
     * This is a map of of all domains mapped to their associated URI and categories
     */
    private final static Map cache;

    /**
     * The cache cleaner thread
     */
    private final static Pulse cacheCleaner;

    /**
     * log4j logger
     */
    private final static Logger logger;

    static
    {
        logger = Logger.getLogger(HostCache.class);
        cache = new LimitedSizeLinkedHashMap();
        cacheCleaner = new Pulse("HostCacheCleaner", true, new HostCacheCleaner());
        cacheCleaner.start(CACHE_CLEAN_FREQUENCY);
    }

    
    public HostCache()
    {
        logger.info("Creating HostCache.");
    }

    public void open()
    {
        /* no op */
        logger.debug("open()");
    }

    public void close()
    {
        /* no op */
        logger.debug("close()");
    }

    public String getCachedCategories(String domain, String uri)
    {
        logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " )");
        
        for ( String dom = domain; null != dom ; dom = nextHost(dom) ) {
            List<CacheRecord> cacheRecords;
            synchronized(cache) {
                cacheRecords = (List<CacheRecord>)cache.get(domain);
            }
            
            if (null != cacheRecords) {
                for (CacheRecord r : cacheRecords) {
                    if (r.isExact()) {
                        if (domain.equals(dom) && uri.toLowerCase().equals(r.uri.toLowerCase())) {
                            logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " ) = " + r.getCategoryString());
                            return r.getCategoryString();
                        }
                    } else {
                        if (uri.toLowerCase().startsWith(r.uri.toLowerCase())) {
                            logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " ) = " + r.getCategoryString());
                            return r.getCategoryString();
                        }
                    }
                }
            }
        }
        return null;
    }

    public void cacheCategories(String url, String categories, String origDomain, String origUri)
    {
        logger.debug("cacheCategories( url=" + url + ", categories=" + categories + ", origDomain=" + origDomain + ", origUri=" + origUri + " )");

        String[] s = url.split("/", 2);

        if (s.length >= 1) {
            String domain;
            String uri;

            boolean exact = false;
            domain = s[0];
            if (domain.startsWith(".")) {
                exact = true;
                domain = domain.substring(1);

                if (origDomain.endsWith(domain)) {
                    addRecord(origDomain, categories, true, origUri);
                }
            }

            if (s.length >= 2) {
                uri = "/" + s[1];
            } else {
                uri = "/";
            }

            addRecord(domain, categories, exact, uri);
        }
    }

    private void addRecord(String domain, String categories, boolean exact, String uri)
    {
        logger.debug("addRecord( domain=" + domain + ", categories=" + categories + ", exact=" + exact + ", uri=" + uri + " )");

        synchronized(cache) {
            List<CacheRecord> cacheRecords = (List<CacheRecord>)cache.get(domain);

            if (cacheRecords == null) {
                cacheRecords = new LinkedList();
            } else {
                for (Iterator<CacheRecord> i = cacheRecords.iterator(); i.hasNext(); ) {
                    CacheRecord cr = i.next();
                    if (uri.equals(cr.getUri())) {
                        i.remove();
                    }
                }
            } 

            cacheRecords.add(new CacheRecord(domain, categories, exact, uri));

            logger.debug("cache.put( domain=" + domain + ", records=" + cacheRecords + " )");
            cache.put(domain, cacheRecords);
        }
    }

    public void cleanCache()
    {
        clean(false);
    }
    
    public void cleanCache(boolean expireAll)
    {
        clean(expireAll);
    }

    
    private static void clean(boolean expireAll)
    {
        long cutoff = System.currentTimeMillis() - CACHE_TTL;
        int removed = 0;
        int kept = 0;

        /* When expire all is true, just delete all of the records. */
        if ( expireAll ) {
            cutoff = Long.MAX_VALUE;
        }

        synchronized(cache) {
            logger.info("Cleaning HostCache... (Current Size: " + cache.size() + ")");

            for (Iterator itr = cache.keySet().iterator() ; itr.hasNext(); ) {
                String key = (String)itr.next();
                List<CacheRecord> cacheRecords = (List<CacheRecord>)cache.get(key);

                logger.debug("Checking: " + key);

                if (cache.size() >= CACHE_MAX_ENTRIES) {
                    itr.remove();
                    removed++;
                    logger.debug("Removing: " + key + " (over max size)");
                }
                else if (null != cacheRecords) {
                    for (Iterator<CacheRecord> i = cacheRecords.iterator(); i.hasNext(); ) {
                        CacheRecord cr = i.next();
                        if (cr.getTime() < cutoff) {
                            i.remove();
                        }
                    }

                    if (0 < cacheRecords.size()) {
                        cache.put(key,cacheRecords);
                        kept++;
                        logger.debug("Keeping : " + key);
                    } else {
                        itr.remove();
                        removed++; 
                        logger.debug("Removing: " + key + " (expired)");
                    }
                }
            }
        }

        logger.info("Cleaned  HostCache. (Kept: " + kept + " Removed: " + removed + ")");

        printStats();
    }

    private static String nextHost(String host)
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

    private static void printStats()
    {
        synchronized(cache) {
            logger.info("HostCache Stats: #Domains:" + cache.size());
        }
    }
    
    private static class HostCacheCleaner implements Runnable
    {
        public void run()
        {
            clean(false);
        }
    }

    private static class LimitedSizeLinkedHashMap extends LinkedHashMap
    {
        protected boolean removeEldestEntry(Map.Entry eldest)
        {
            boolean toobig = (size() > CACHE_HARDMAX_ENTRIES);

            if (toobig) 
                logger.warn("Max size reached, force pruning. (size: " + size() + ")");

            return toobig;
        }
    }
    
    private static class CacheRecord
    {
        private String time;
        private String categoryStr;
        private String exact;
        private String uri;

        CacheRecord(String domain, String categoryStr, boolean exact, String uri)
        {
            this.time = Long.toString(System.currentTimeMillis());
            this.categoryStr = categoryStr;
            this.exact = exact ? "1" : "0";
            this.uri = uri;
        }

        private CacheRecord(String time, String categoryStr, String exact, String uri)
        {
            this.time = time;
            this.categoryStr = categoryStr;
            this.exact = exact;
            this.uri = uri;
        }

        static CacheRecord readRecord(String s)
        {
            String[] split = s.split(" ", 4);
            if (split.length != 4) {
                Logger.getLogger(CacheRecord.class).warn("bad record: " + s);
                return null;
            } else {
                return new CacheRecord(split[0], split[1], split[2], split[3]);
            }
        }

        private void writeRecord(StringBuilder sb)
        {
            sb.append(time).append(" ").append(categoryStr).append(" ").append(exact).append(" ").append(uri);
        }

        public long getTime()
        {
            try {
                return Long.parseLong(this.time);
            } catch (NumberFormatException exn) {
                return 0L;
            }
        }

        public String getUri()
        {
            return uri;
        }

        public String getCategoryString()
        {
            return categoryStr;
        }

        public boolean isExact()
        {
            return "1".equals(exact);
        }

        @Override
        public String toString()
        {
            return "[record" + " category: " + categoryStr + " exact: " + exact + " uri: " + uri + " expires: " + time;
        }
    }
}