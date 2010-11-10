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
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.user.PhoneBookAssistant;
import com.untangle.uvm.util.TransactionWork;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.node.NodeState;
import com.untangle.uvm.node.LocalADConnector;

/**
 * Assistant for the Captive Portal.  This fetches the current username for an ip address from a postgres database.
 * @author rbscott
 *
 */
public class CPDPhoneBookAssistant implements PhoneBookAssistant {

    /* how often to refresh internal cache from database */
    private final long DATABASE_READER_INTERVAL = 10000L;

    private final int ASSISTANT_PRIORITY = 3;

    private final Logger logger = Logger.getLogger(getClass());
    
    private HashMap<InetAddress,MapValue> cache = new HashMap<InetAddress,MapValue>();

    private final Pulse databaseReader;

    private boolean registered = false;

    private CPDImpl cpd;
    
    private List<HostDatabaseEntry> localStatus = new LinkedList<HostDatabaseEntry>();    
    
    /* -------------- Constructors -------------- */
    public CPDPhoneBookAssistant(CPDImpl cpd)
    {
        this.cpd = cpd;
        this.databaseReader = new Pulse("CPDDatabaseReader", true, new CPDDatabaseReader(cpd,this));
        this.startDatabaseReader();
        this.registered = false;
    }


    /**
     * return the username from cache
     */
    public String lookupUsername ( InetAddress addr )
    {
        MapValue cacheEntry = this.cache.get(addr);
        if (cacheEntry != null) {
            logger.debug("CPDPhoneBook lookup IP: " + addr + " User: " + cacheEntry.username);

            return cacheEntry.username;
        } else {
            logger.debug("CPDPhoneBook lookup IP: " + addr + " User: None");

            return null;
        }
    }

    /**
     * return the cache expiration time
     */
    public Date lookupUsernameExpiration ( InetAddress addr )
    {
        MapValue cacheEntry = this.cache.get(addr);
        if (cacheEntry != null) {
            return cacheEntry.expirationDate;
        }
        else
            return null;
    }

    /**
     * CPD knows nothing about hostnames
     */
    public String lookupHostname ( InetAddress addr )
    {
        return null;
    }

    /**
     * CPD knows nothing about hostnames
     */
    public Date lookupHostnameExpiration ( InetAddress addr )
    {
        return null;
    }
    
    /**
     * return name
     */
    public String lookupAuthenticationMethod ( InetAddress addr )
    {
        return "Captive Portal";
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
    public void addCache(InetAddress addr, String username)
    {
        MapValue newCacheEntry = new MapValue();

        newCacheEntry.expirationDate = new Date(System.currentTimeMillis() + (this.cpd.getCPDSettings().getBaseSettings().getTimeout()*1000));
        newCacheEntry.username = username;
        synchronized(cache) {
            cache.put(addr,newCacheEntry);
        }
        logger.debug( "Add    Cache Entry: (IP " + addr + ") (Username " + username + ")");

        LocalADConnector adconnector = (LocalADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getPhoneBook().lookupUser( addr );
        }
    }

    /**
     * Removes a mapping to the CPD Phone Book cache
     * This is public because it is useful for immediately expiring cache entries when users logout
     * or are force logged out
     */
    public void removeCache(InetAddress addr)
    {
        logger.debug( "Remove Cache Entry: (IP " + addr + ")");
        Object k = null;
        synchronized(cache) {
            k = cache.remove(addr);
        }
        if (k == null) {
            logger.debug( "Failed Remove Cache Entry: (IP " + addr + ") - Missing key");
        }

        LocalADConnector adconnector = (LocalADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getPhoneBook().expireUser( addr );
        }
    }

    public void destroy()
    {
        this.stopDatabaseReader();

        LocalADConnector adconnector = (LocalADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null) {
            adconnector.getPhoneBook().unregisterAssistant( this );
            logger.debug("CPDPhoneBookAssistant unregistered");
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
        private final CPDPhoneBookAssistant assistant;

        public CPDDatabaseReader(CPDImpl cpd, CPDPhoneBookAssistant assistant)
        {
            this.cpd = cpd;
            this.assistant = assistant;
        }
        
        @SuppressWarnings("unchecked") /* for cast of Query result */
        public void run()
        {
            if (this.cpd.getRunState() !=  NodeState.RUNNING)
                return;

            TransactionWork<Void> tw = new TransactionWork<Void>()
                {
                    public boolean doWork(Session s)
                    {
                        if (!registered) {
                            LocalADConnector adconnector = (LocalADConnector)LocalUvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                            if (adconnector != null) {
                                adconnector.getPhoneBook().registerAssistant( assistant );
                                logger.debug("CPDPhoneBookAssistant registered");
                                registered = true;
                            }
                            else {
                                logger.debug("CPDPhoneBookAssistant ignoring (adconnector not running)");
                            }
                        }

                        if (registered) {
                            Query q = s.createQuery("from HostDatabaseEntry hde where hde.expirationDate > :now ORDER BY hde.expirationDate DESC");
                            q.setDate("now", new Date());

                            List<HostDatabaseEntry> list = q.list();

                            Date now = new Date();

                            for ( HostDatabaseEntry entry : list ) {
                                InetAddress addr = entry.getIpv4Address();
                                String username = entry.getUsername();
                                Date expirationDate = entry.getExpirationDate();

                                logger.debug( "Read from DB: (IP " + addr + ") (Username " + username + ") (Expiration Date " + expirationDate + ")");

                                /* lookup in current cache */
                                MapValue cacheEntry = cache.get(addr);

                                /* if cache miss - add to cache */
                                /* don't count DEFAULT_USERNAME as a valid user */
                                if ( cacheEntry == null && ! CPDImpl.DEFAULT_USERNAME.equals(username)) { /* cache miss */
                                    addCache(addr,username);
                                }
                                /* if cache hit - update expire time as its still in the database */
                                else {
                                    cacheEntry.expirationDate = new Date(now.getTime() + (cpd.getCPDSettings().getBaseSettings().getTimeout()*1000));
                                }
                            }
                            
                            LinkedList<InetAddress> toRemove = new LinkedList<InetAddress>();
                            
                            synchronized(cache) {
                                Set<InetAddress> inets = cache.keySet();

                                /**
                                 * Mark expired entries for removal
                                 * Also remove entries far in future (this indicateds clock shift)
                                 */
                                for ( InetAddress inet : inets ) {
                                    MapValue value = cache.get(inet);
                                    
                                    /**
                                     * expiration time has lapsed
                                     */
                                    if (value.expirationDate.before(now)) {
                                        toRemove.add(inet);
                                    }
                                    /**
                                     * too far in future - oldest entries should expire in DATABASE_CACHE_TIME
                                     */
                                    if (value.expirationDate.after(new Date(now.getTime() + (cpd.getCPDSettings().getBaseSettings().getTimeout()*1000)*10))) {
                                        logger.warn("cache entry expires too far in the future - removing");
                                        toRemove.add(inet);
                                    }
                                    
                                }
                            }

                            /**
                             * Actually remove the entries
                             * We do this here to avoid concurrent modification issues
                             */
                            for ( InetAddress inet : toRemove ) {
                                removeCache(inet);
                            }

                        }

                        return true;
                    }
                };
        
            this.cpd.getNodeContext().runTransaction(tw);
        }
    }
   
    @SuppressWarnings("unchecked") /* for cast of Query result */
    public List<HostDatabaseEntry> getCaptiveStatus()
    {
        if (this.cpd.getRunState() !=  NodeState.RUNNING) return(null);

            TransactionWork<Void> tw = new TransactionWork<Void>()
            {
                public boolean doWork(Session s)
                {
                    Query q = s.createQuery("from HostDatabaseEntry");
                    localStatus = q.list();
                    return true;
                }
            };
    
        this.cpd.getNodeContext().runTransaction(tw);
        return(localStatus);
    }
}
