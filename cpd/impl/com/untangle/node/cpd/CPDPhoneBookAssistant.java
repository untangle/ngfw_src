/*
 * Copyright (c) 2003-2009 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: CPDPhoneBookAssistant.java 25604 2010-01-26 03:55:59Z rbscott $
 */
package com.untangle.node.cpd;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.user.PhoneBookAssistant;
import com.untangle.uvm.user.UserInfo;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.node.NodeState;

/**
 * Assistant for the Captive Portal.  This fetches the current username for an ip address from a postgres database.
 * @author rbscott
 *
 */
public class CPDPhoneBookAssistant implements PhoneBookAssistant {

    /* how often to refresh internal cache from database */
    private final long DATABASE_READER_INTERVAL = 10000L;

    /* how long cache entries last - this should be greater than reader interval in case it has delays cache will still be valid */
    private final long DATABASE_CACHE_TIME = DATABASE_READER_INTERVAL * 2;

    private final int ASSISTANT_PRIORITY = 3;

    private final Logger logger = Logger.getLogger(getClass());
    
    private HashMap<InetAddress,MapValue> cache = new HashMap<InetAddress,MapValue>();

    private final Pulse databaseReader;

    /* -------------- Constructors -------------- */
    public CPDPhoneBookAssistant(CPDImpl cpd)
    {
        this.databaseReader = new Pulse("CPDDatabaseReader", true, new CPDDatabaseReader(cpd));
        this.startDatabaseReader();
    }

    /* ----------------- Public ----------------- */

    /**
     * Lookup user info for an IP
     */
    public void lookup(final UserInfo info)
    {
        final InetAddress address = info.getAddress();

        MapValue cacheEntry = this.cache.get(address);
        if (cacheEntry != null) {
            if ( logger.isDebugEnabled()) {
                logger.debug("CPDPhoneBook lookup IP: " + info.getAddress() + " User: " + cacheEntry.username);
            }
            
            info.setUsername(cacheEntry.username);
            info.setExpirationDate(cacheEntry.expirationDate.getTime());
        } else {
            if ( logger.isDebugEnabled()) {
                logger.debug("CPDPhoneBook lookup IP: " + info.getAddress() + " User: None");
            }
        }

        return;
    }

    /**
     * Check to see if the user information has changed, if it has return a new
     * UserInfo object
     */
    public UserInfo update(UserInfo info) {
        throw new IllegalStateException("unimplemented");
    }

    /**
     * Assistant priority
     */
    public int priority()
    {
        return ASSISTANT_PRIORITY;
    }

    /**
     * Adds a mapping to the CPD Phone Book cache
     * This is public because it is useful when a user logins in and must be immediately added to the
     * phone book and the normal Database Reader will take too long
     */
    public void addCache(InetAddress ipAddr, String username)
    {
        MapValue newCacheEntry = new MapValue();
        newCacheEntry.expirationDate = new Date(System.currentTimeMillis() + DATABASE_CACHE_TIME);
        newCacheEntry.username = username;
        cache.put(ipAddr,newCacheEntry);
        logger.debug( "Add    Cache Entry: (IP " + ipAddr + ") (Username " + username + ")");
    }

    /**
     * Removes a mapping to the CPD Phone Book cache
     * This is public because it is useful for immediately expiring cache entries when users logout
     * or are force logged out
     */
    public void removeCache(InetAddress addr)
    {
        logger.debug( "Remove Cache Entry: (IP " + addr + ")");
        Object k = cache.remove(addr);
        if (k == null) {
            logger.debug( "Failed Remove Cache Entry: (IP " + addr + ") - Missing key");
        }
    }

    void startDatabaseReader()
    {
        this.databaseReader.start(DATABASE_READER_INTERVAL);
    }

    void stopDatabaseReader()
    {
        this.databaseReader.stop();
    }
    
    private class MapValue
    {
        public String username;

        /* Expiration date of the cache value - not necessarily the same as expiration for authentication */
        public Date expirationDate;
    }

    private class CPDDatabaseReader implements Runnable
    {
        private final CPDImpl cpd;

        public CPDDatabaseReader(CPDImpl cpd)
        {
            this.cpd = cpd;
        }
        
        public void run()
        {
            if (this.cpd.getRunState() !=  NodeState.RUNNING)
                return;

            TransactionWork<Void> tw = new TransactionWork<Void>()
                {
                    public boolean doWork(Session s)
                    {
                        Query q = s.createQuery("from HostDatabaseEntry hde where hde.expirationDate > :now ORDER BY hde.expirationDate DESC");
                        q.setDate("now", new Date());

                        List<HostDatabaseEntry> list = (List<HostDatabaseEntry>)q.list();

                        Date now = new Date();

                        for ( HostDatabaseEntry entry : list ) {
                            InetAddress ipAddr = entry.getIpv4Address();
                            String username = entry.getUsername();
                            Date expirationDate = entry.getExpirationDate();

                            logger.debug( "Read from DB: (IP " + ipAddr + ") (Username " + username + ") (Expiration Date " + expirationDate + ")");

                            /* lookup in current cache */
                            MapValue cacheEntry = cache.get(ipAddr);

                            /* if cache miss - add to cache */
                            if ( cacheEntry == null) { /* cache miss */
                                addCache(ipAddr,username);
                            }
                            /* if cache hit - update expire time as its still in the database */
                            else {
                                cacheEntry.expirationDate = new Date(now.getTime() + DATABASE_CACHE_TIME);
                            }
                        }

                        Set<InetAddress> inets = cache.keySet();

                        /**
                         * Remove expired entries
                         * Also remove entries far in future (this indicateds clock shift)
                         */
                        for ( InetAddress inet : inets ) {
                            MapValue value = cache.get(inet);
                            /**
                             * expiration time has lapsed
                             */
                            if (value.expirationDate.before(now)) {
                                removeCache(inet);
                            }
                            /**
                             * too far in future - oldest entries should expire in DATABASE_CACHE_TIME
                             */
                            if (value.expirationDate.after(new Date(now.getTime() + DATABASE_CACHE_TIME*10))) {
                                logger.warn("cache entry expires too far in the future - removing");
                                removeCache(inet);
                            }

                        }

                        return true;
                    }
                };
        
            this.cpd.getNodeContext().runTransaction(tw);

        }
    }

}
