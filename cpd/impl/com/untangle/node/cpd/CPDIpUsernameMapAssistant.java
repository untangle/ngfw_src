/**
 * $Id: CPDIpUsernameMapAssistant.java,v 1.00 2012/05/09 18:16:08 dmorris Exp $
 */
package com.untangle.node.cpd;

import java.net.InetAddress;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.sql.SQLException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.node.NodeSettings;
import com.untangle.uvm.user.IpUsernameMapAssistant;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.node.DirectoryConnector;

/**
 * Assistant for the Captive Portal. This fetches the current username for an ip
 * address from a postgres database.
 */
public class CPDIpUsernameMapAssistant implements IpUsernameMapAssistant
{

    /* how often to refresh internal cache from database */
    private final long DATABASE_READER_INTERVAL = 10000L;

    private final int ASSISTANT_PRIORITY = 3;

    private final Logger logger = Logger.getLogger(getClass());

    private HashMap<InetAddress, MapValue> cache = new HashMap<InetAddress, MapValue>();

    private final Pulse databaseReader;

    private boolean registered = false;

    private CPDImpl cpd;

    private List<HostDatabaseEntry> localStatus = new LinkedList<HostDatabaseEntry>();

    /* -------------- Constructors -------------- */
    public CPDIpUsernameMapAssistant(CPDImpl cpd)
    {
        this.cpd = cpd;
        this.databaseReader = new Pulse("CPDDatabaseReader", true, new CPDDatabaseReader(cpd, this));
        this.startDatabaseReader();
        this.registered = false;
    }

    /**
     * return the username from cache
     */
    public String lookupUsername(InetAddress addr)
    {
        MapValue cacheEntry = this.cache.get(addr);
        if (cacheEntry != null)
        {
            logger.debug("CPDIpUsernameMap lookup IP: " + addr + " User: " + cacheEntry.username);

            return cacheEntry.username;
        } else
        {
            logger.debug("CPDIpUsernameMap lookup IP: " + addr + " User: None");

            return null;
        }
    }

    /**
     * return the cache expiration time
     */
    public Date lookupUsernameExpiration(InetAddress addr)
    {
        MapValue cacheEntry = this.cache.get(addr);
        if (cacheEntry != null)
        {
            return cacheEntry.expirationDate;
        } else
            return null;
    }

    /**
     * CPD knows nothing about hostnames
     */
    public String lookupHostname(InetAddress addr)
    {
        return null;
    }

    /**
     * CPD knows nothing about hostnames
     */
    public Date lookupHostnameExpiration(InetAddress addr)
    {
        return null;
    }

    /**
     * return name
     */
    public String lookupAuthenticationMethod(InetAddress addr)
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
     * Adds a mapping to the CPD Phone Book cache This is public because it is
     * useful when a user logins in and must be immediately added to the phone
     * book and the normal Database Reader will take too long
     */
    public void addCache(InetAddress addr, String username)
    {
        MapValue newCacheEntry = new MapValue();

        newCacheEntry.expirationDate = new Date(System.currentTimeMillis() + (this.cpd.getSettings().getTimeout() * 1000));
        newCacheEntry.username = username;
        synchronized (cache)
        {
            cache.put(addr, newCacheEntry);
        }
        logger.debug("Add    Cache Entry: (IP " + addr + ") (Username " + username + ")");

        DirectoryConnector adconnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null)
        {
            adconnector.getIpUsernameMap().lookupUser(addr);
        }
    }

    /**
     * Removes a mapping to the CPD Phone Book cache This is public because it
     * is useful for immediately expiring cache entries when users logout or are
     * force logged out
     */
    public void removeCache(InetAddress addr)
    {
        logger.debug("Remove Cache Entry: (IP " + addr + ")");
        Object k = null;
        synchronized (cache)
        {
            k = cache.remove(addr);
        }
        if (k == null)
        {
            logger.debug("Failed Remove Cache Entry: (IP " + addr + ") - Missing key");
        }

        DirectoryConnector adconnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null)
        {
            adconnector.getIpUsernameMap().expireUser(addr);
        }
    }

    public void destroy()
    {
        this.stopDatabaseReader();

        DirectoryConnector adconnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
        if (adconnector != null)
        {
            adconnector.getIpUsernameMap().unregisterAssistant(this);
            logger.debug("CPDIpUsernameMapAssistant unregistered");
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

        /*
         * Expiration date of the cache value - not necessarily the same as
         * expiration for authentication
         */
        public Date expirationDate;
    }

    private class CPDDatabaseReader implements Runnable
    {
        private final CPDImpl cpd;
        private final CPDIpUsernameMapAssistant assistant;

        public CPDDatabaseReader(CPDImpl cpd, CPDIpUsernameMapAssistant assistant)
        {
            this.cpd = cpd;
            this.assistant = assistant;
        }

        @SuppressWarnings("unchecked")
        /* for cast of Query result */
        public void run()
        {
            if (this.cpd.getRunState() != NodeSettings.NodeState.RUNNING)
                return;

            if (!registered)
            {
                DirectoryConnector adconnector = (DirectoryConnector) UvmContextFactory.context().nodeManager().node("untangle-node-adconnector");
                if (adconnector != null)
                {
                    adconnector.getIpUsernameMap().registerAssistant(assistant);
                    logger.debug("CPDIpUsernameMapAssistant registered");
                    registered = true;
                } else
                {
                    logger.debug("CPDIpUsernameMapAssistant ignoring (adconnector not running)");
                }
            }

            if (registered)
            {
                List<HostDatabaseEntry> list = readHostDatabase("WHERE expiration_date > NOW() ORDER BY expiration_date DESC");

                Date now = new Date();

                for (HostDatabaseEntry entry : list)
                {
                    InetAddress addr = entry.getIpv4Address();
                    String username = entry.getUsername();
                    Date expirationDate = entry.getExpirationDate();

                    logger.debug("Read from DB: (IP " + addr + ") (Username " + username + ") (Expiration Date " + expirationDate + ")");

                    /* lookup in current cache */
                    MapValue cacheEntry = cache.get(addr);

                    /* if cache miss - add to cache */
                    /* don't count DEFAULT_USERNAME as a valid user */
                    if (cacheEntry == null)
                    {
                        /* cache miss */

                        /*
                         * only add the is it isnt the default username (no
                         * login specified)
                         */
                        if (!CPDImpl.DEFAULT_USERNAME.equals(username))
                        {
                            addCache(addr, username);
                        }
                    }
                    /*
                     * if cache hit - update expire time as its still in the
                     * database
                     */
                    else
                    {
                        cacheEntry.expirationDate = new Date(now.getTime() + (cpd.getSettings().getTimeout() * 1000));
                    }
                }

                LinkedList<InetAddress> toRemove = new LinkedList<InetAddress>();

                synchronized (cache)
                {
                    Set<InetAddress> inets = cache.keySet();

                    /**
                     * Mark expired entries for removal Also remove entries far
                     * in future (this indicateds clock shift)
                     */
                    for (InetAddress inet : inets)
                    {
                        MapValue value = cache.get(inet);

                        /**
                         * expiration time has lapsed
                         */
                        if (value.expirationDate.before(now))
                        {
                            toRemove.add(inet);
                        }
                        /**
                         * too far in future - oldest entries should expire in
                         * DATABASE_CACHE_TIME
                         */
                        if (value.expirationDate.after(new Date(now.getTime() + (cpd.getSettings().getTimeout() * 1000) * 10)))
                        {
                            logger.warn("cache entry expires too far in the future - removing");
                            toRemove.add(inet);
                        }
                    }
                }

                /**
                 * Actually remove the entries We do this here to avoid
                 * concurrent modification issues
                 */
                for (InetAddress inet : toRemove)
                {
                    removeCache(inet);
                }

            }

        }
    }

    public List<HostDatabaseEntry> getCaptiveStatus()
    {
        if (this.cpd.getRunState() != NodeSettings.NodeState.RUNNING)
            return (null);
        List<HostDatabaseEntry> list = readHostDatabase(null);
        return (list);
    }

    private List<HostDatabaseEntry> readHostDatabase(String condition)
    {
        List<HostDatabaseEntry> list = new LinkedList<HostDatabaseEntry>();
        Connection conn = null;
        ResultSet rset = null;
        Statement stmt = null;
        String qstr = "SELECT entry_id, hw_addr, ipv4_addr, username, last_session, session_start, expiration_date FROM events.n_cpd_host_database_entry";

        if (condition != null)
            qstr = (qstr + " " + condition);

        try
        {
            conn = UvmContextFactory.context().getDBConnection();
            if (conn != null)
                stmt = conn.createStatement();
            if (stmt != null)
                rset = stmt.executeQuery(qstr);

            if (rset == null)
            {
                if (conn != null)
                    conn.close();
                return (list);
            }

            while (rset.next())
            {
                HostDatabaseEntry entry = new HostDatabaseEntry();

                entry.setId(rset.getLong(1));
                entry.setHardwareAddress(rset.getString(2));
                entry.setIpv4Address(InetAddress.getByName(rset.getString(3)));
                entry.setUsername(rset.getString(4));
                entry.setLastSession(rset.getDate(5));
                entry.setSessionStart(rset.getDate(6));
                entry.setExpirationDate(rset.getDate(7));
                list.add(entry);
            }

            conn.close();
        }

        catch (Exception e)
        {
            logger.error("Unable to query the database", e);
        }

        return (list);
    }
}
