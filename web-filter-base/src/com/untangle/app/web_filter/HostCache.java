/**
 * $Id: HostCache.java,v 1.00 2017/03/03 19:25:38 dmorris Exp $
 * 
 * Copyright (c) 2003-2017 Untangle, Inc.
 *
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Untangle.
 */
package com.untangle.app.web_filter;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.UrlMatchingUtil;
import com.untangle.uvm.util.Pulse;

/**
 * Host Cache Implementation
 */
@SuppressWarnings("serial")
public class HostCache
{
    /**
     * The cache size will be reduced to this size when cleaned Note: this is
     * not actually enforced in real-time
     */
    private final static int DOMAIN_CACHE_MAX_ENTRIES = 20000;

    /**
     * This is how long entries in the cache will be stored This is the default
     * value
     */
    private final static long CACHE_TTL = 1000 * 60 * 60 * 24; // 24 hours

    /**
     * This is how long entries in the cache will be stored This value is used
     * for very busy domains to avoid overcrowding
     */
    private final static long CACHE_TTL_SHORT = 1000 * 60 * 60 * 2; // 2 hours

    /**
     * If a domain has more than this many cached URLs It will be considered
     * "Large" and get the short TTL
     */
    private final static long CACHE_LARGE_SIZE = 1000;

    /**
     * The cache will be cleaned every this many milliseconds during cleaned all
     * expired (according to CACHE_TTL entries are removed)
     */
    private final static long CACHE_CLEAN_FREQUENCY = 30 * 60 * 1000L; // 30 minutes

    /**
     * This is a map of of all domains mapped to their associated URI and
     * categories
     */
    private final static Map<String, DomainCacheRecord> domainCache;

    /**
     * The cache cleaner thread
     */
    private final static Pulse cacheCleaner;

    /**
     * log4j logger
     */
    private final static Logger logger = Logger.getLogger(HostCache.class);

    static {
        domainCache = new ConcurrentHashMap<String, DomainCacheRecord>();
        cacheCleaner = new Pulse("HostCacheCleaner", new HostCacheCleaner(), CACHE_CLEAN_FREQUENCY);
        cacheCleaner.start();

        logger.info("Initializing HostCache");
    }

    /**
     * Constructor
     */
    private HostCache()
    {
    }

    /**
     * Return the cached categories for a host
     * 
     * @param domain
     *        The domain
     * @param uri
     *        The uri
     * @return The category
     */
    public static String getCachedCategories(String domain, String uri)
    {
        uri = uri.toLowerCase(); //match is case insensitive
        logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " )");

        /**
         * Iterate through all subdomains looking for cache hits
         */
        for (String dom = domain; null != dom; dom = UrlMatchingUtil.nextHost(dom)) {

            DomainCacheRecord domainCacheRecord = domainCache.get(dom);
            if (domainCacheRecord == null) continue;

            /**
             * Check non-exact matches first - most likely to live there
             */
            if (domainCacheRecord.nonExactMatches != null) {
                for (CacheRecord r : domainCacheRecord.nonExactMatches) {
                    if (uri.startsWith(r.uri)) {
                        logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " ) = " + r.getCategoryString());
                        return r.getCategoryString();
                    }
                }
            }

            /**
             * Check exact matches next
             */
            if (domainCacheRecord.exactMatches != null) {
                for (CacheRecord r : domainCacheRecord.exactMatches) {
                    if (domain.equals(dom) && uri.equals(r.uri)) {
                        logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " ) = " + r.getCategoryString());
                        return r.getCategoryString();
                    }
                }
            }
        }

        logger.debug("getCachedCategories( domain=" + domain + ", uri=" + uri + " ) = null ");
        return null;
    }

    /**
     * Holds a list of categories
     * 
     * @param url
     *        The url
     * @param categories
     *        The categories
     * @param origDomain
     *        The original domain
     * @param origUri
     *        The original URI
     */
    public static void cacheCategories(String url, String categories, String origDomain, String origUri)
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

    /**
     * Called to clean expired cache entries
     */
    public static void cleanCache()
    {
        clean(false);
    }

    /**
     * Called to clean cache entries
     * 
     * @param expireAll
     *        True to clean all, false to clean expired
     */
    public static void cleanCache(boolean expireAll)
    {
        clean(expireAll);
    }

    /**
     * Add a record to the cache
     * 
     * @param domain
     *        The domain
     * @param categories
     *        The categories
     * @param exact
     *        Exact match flag
     * @param uri
     *        The uri
     */
    private static void addRecord(String domain, String categories, boolean exact, String uri)
    {
        int domainCacheSize = 0;

        logger.debug("addRecord( domain=" + domain + ", categories=" + categories + ", exact=" + exact + ", uri=" + uri + " )");

        DomainCacheRecord domainCacheRecord = domainCache.get(domain);
        ConcurrentLinkedQueue<CacheRecord> cacheRecords;

        if (domainCacheRecord == null) {
            domainCacheRecord = new DomainCacheRecord();
            domainCache.put(domain, domainCacheRecord);
        }

        if (exact) {
            if (domainCacheRecord.exactMatches == null) domainCacheRecord.exactMatches = new ConcurrentLinkedQueue<CacheRecord>();
            cacheRecords = domainCacheRecord.exactMatches;
        } else {
            if (domainCacheRecord.nonExactMatches == null) domainCacheRecord.nonExactMatches = new ConcurrentLinkedQueue<CacheRecord>();
            cacheRecords = domainCacheRecord.nonExactMatches;
        }

        /**
         * If one already exists for the same URI - remove it
         */
        for (Iterator<CacheRecord> i = cacheRecords.iterator(); i.hasNext();) {
            CacheRecord cr = i.next();
            if (uri.equals(cr.getUri())) {
                i.remove();
            }
            domainCacheSize++;
        }

        cacheRecords.add(new CacheRecord(domain, categories, exact, uri));

        /**
         * If this is a large domain - use the short TTL
         */
        if (domainCacheSize > CACHE_LARGE_SIZE) {
            domainCacheRecord.cacheTTL = CACHE_TTL_SHORT;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("domainCache.put( domain=" + domain + ", records=" + cacheRecords + " )");
        }
    }

    /**
     * Private cache cleaner function
     * 
     * @param expireAll
     *        True to clean all, false to clean expired
     */
    private static void clean(boolean expireAll)
    {
        int domainsRemoved = 0;
        int domainsKept = 0;
        int uriRemoved = 0;
        int uriKept = 0;

        logger.info("Cleaning HostCache... (Current Size: " + domainCache.size() + ")");

        for (Iterator<String> itr = domainCache.keySet().iterator(); itr.hasNext();) {
            String key = itr.next();
            logger.debug("Checking: " + key);

            if (domainCache.size() >= DOMAIN_CACHE_MAX_ENTRIES) {
                itr.remove();
                domainsRemoved++;
                logger.debug("Removing: " + key + " (over max size)");
                continue;
            }

            DomainCacheRecord domainCacheRecord = domainCache.get(key);
            if (domainCacheRecord == null) continue;

            ConcurrentLinkedQueue<CacheRecord> nonExactCacheRecords = domainCacheRecord.nonExactMatches;
            ConcurrentLinkedQueue<CacheRecord> exactCacheRecords = domainCacheRecord.exactMatches;
            long cutoff = System.currentTimeMillis() - domainCacheRecord.cacheTTL;
            if (expireAll) {
                cutoff = Long.MAX_VALUE;
            }

            int domainUrisKept = 0;

            if (nonExactCacheRecords != null) {
                for (Iterator<CacheRecord> i = nonExactCacheRecords.iterator(); i.hasNext();) {
                    CacheRecord cr = i.next();
                    if (cr.getCreationTime() < cutoff) {
                        i.remove();
                        uriRemoved++;
                    } else {
                        uriKept++;
                        domainUrisKept++;
                    }
                }
            }
            if (exactCacheRecords != null) {
                for (Iterator<CacheRecord> i = exactCacheRecords.iterator(); i.hasNext();) {
                    CacheRecord cr = i.next();
                    if (cr.getCreationTime() < cutoff) {
                        i.remove();
                        uriRemoved++;
                    } else {
                        uriKept++;
                        domainUrisKept++;
                    }
                }
            }

            if (domainUrisKept > 0) {
                domainsKept++;
                logger.debug("Keeping : " + key);
            } else {
                itr.remove();
                domainsRemoved++;
                logger.debug("Removing: " + key + " (expired)");
            }
        }

        logger.info("Cleaned  HostCache. (Domains Kept: " + domainsKept + " Removed: " + domainsRemoved + ") (URI Kept: " + uriKept + " Removed: " + uriRemoved + ")");

        printStats();
    }

    /**
     * Print cache statistics
     */
    private static void printStats()
    {
        logger.info("HostCache Stats: #Domains:" + domainCache.size());

        if (logger.isDebugEnabled()) {
            for (Iterator<String> itr = domainCache.keySet().iterator(); itr.hasNext();) {
                String key = itr.next();
                ConcurrentLinkedQueue<CacheRecord> cacheRecords;
                cacheRecords = domainCache.get(key).nonExactMatches;

                logger.debug("HostCache Stats: Domain:" + key);
                if (null != cacheRecords) {
                    for (CacheRecord cr : cacheRecords) {
                        logger.debug("HostCache Stats: Domain:" + key + " Non-Exact URI:" + cr.getUri() + " Category: " + cr.getCategoryString());
                    }
                }

                cacheRecords = domainCache.get(key).exactMatches;
                if (null != cacheRecords) {
                    for (CacheRecord cr : cacheRecords) {
                        logger.debug("HostCache Stats: Domain:" + key + "     Exact URI:" + cr.getUri() + " Category: " + cr.getCategoryString());
                    }
                }

            }
        }
    }

    /**
     * Host Cache Cleaner
     */
    private static class HostCacheCleaner implements Runnable
    {
        /**
         * Main thread run function
         */
        public void run()
        {
            clean(false);
        }
    }

    /**
     * Domain Cache Record
     */
    private static class DomainCacheRecord
    {
        ConcurrentLinkedQueue<CacheRecord> nonExactMatches;
        ConcurrentLinkedQueue<CacheRecord> exactMatches;

        long cacheTTL;

        /**
         * Constructor
         */
        DomainCacheRecord()
        {
            nonExactMatches = null;
            exactMatches = null;
            this.cacheTTL = CACHE_TTL;
        }
    }

    /**
     * Cache Record
     */
    private static class CacheRecord
    {
        private long creationTime;
        private String categoryStr;
        private boolean exact;
        private String uri;

        /**
         * Constructor
         * 
         * @param domain
         *        The domain
         * @param categoryStr
         *        The category
         * @param exact
         *        Exact flag
         * @param uri
         *        The URI
         */
        CacheRecord(String domain, String categoryStr, boolean exact, String uri)
        {
            this.creationTime = System.currentTimeMillis();
            this.categoryStr = categoryStr;
            this.exact = exact;
            //call toLower on creation time to avoid doing this later
            this.uri = uri.toLowerCase();
        }

        /**
         * Get the creation time
         * 
         * @return The creation time
         */
        public long getCreationTime()
        {
            return this.creationTime;
        }

        /**
         * Get the URI
         * 
         * @return The URI
         */
        public String getUri()
        {
            return uri;
        }

        /**
         * Get the category
         * 
         * @return The category
         */
        public String getCategoryString()
        {
            return categoryStr;
        }

        /**
         * Get the exact match flag
         * 
         * @return The exact match flag
         */
        public boolean isExact()
        {
            return exact;
        }

        /**
         * Get the string representation of cache record
         * 
         * @return The string representation
         */
        @Override
        public String toString()
        {
            return "[record" + " category: " + categoryStr + " exact: " + exact + " uri: " + uri + " expires: " + creationTime;
        }
    }
}
