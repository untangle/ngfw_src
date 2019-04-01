/**
 * $Id: UserTableImpl.java,v 1.00 2017/02/25 10:53:06 dmorris Exp $
 */

package com.untangle.uvm;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.Collections;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.UserTable;
import com.untangle.uvm.UserTableEntry;
import com.untangle.uvm.util.Pulse;
import com.untangle.uvm.app.QuotaEvent;

/**
 * The User Table stores a list of recently encountered users and any metadata
 * about them
 * 
 * Other Documentation in UserTable.java
 */
public class UserTableImpl implements UserTable
{
    private static final int HIGH_WATER_SIZE = 12000; // absolute max
    private static final int LOW_WATER_SIZE = 10000; // max size to reduce to when pruning map
    private static final int CLEANER_SLEEP_TIME_MILLI = 60 * 1000; // 60 seconds
    private static final int CLEANER_LAST_ACCESS_MAX_TIME = 60 * 60 * 1000; // 60 minutes
    private static final String USERS_SAVE_FILENAME = System.getProperty("uvm.settings.dir") + "/untangle-vm/users.js";
    private static final int PERIODIC_SAVE_DELAY = 1000 * 60 * 60 * 6; // 6 hours

    private final Logger logger = Logger.getLogger(getClass());

    private ConcurrentHashMap<String, UserTableEntry> userTable;

    private volatile Thread cleanerThread;
    private UserTableCleaner cleaner = new UserTableCleaner();

    private final Pulse saverPulse = new Pulse("user-table-saver", new UserTableSaver(), PERIODIC_SAVE_DELAY);

    private volatile long lastSaveTime = 0;

    /**
     * Constructor
     */
    protected UserTableImpl()
    {
        this.lastSaveTime = System.currentTimeMillis();
        loadSavedUsers();

        saverPulse.start();

        UvmContextFactory.context().newThread(this.cleaner).start();
    }

    /**
     * Set the list of users
     * 
     * @param newUsers
     *        The user list
     */
    public synchronized void setUsers(LinkedList<UserTableEntry> newUsers)
    {
        ConcurrentHashMap<String, UserTableEntry> oldUserTable = this.userTable;
        this.userTable = new ConcurrentHashMap<>();

        /**
         * For each entry, copy the value on top of the exitsing objects so
         * references are maintained If there aren't in the table, create new
         * entries
         */
        for (UserTableEntry entry : newUsers) {
            String username = entry.getUsername();
            if (username == null) continue;

            UserTableEntry existingEntry = oldUserTable.get(username);
            if (existingEntry != null) {
                existingEntry.copy(entry);
                this.userTable.put(existingEntry.getUsername(), existingEntry);
            } else {
                this.userTable.put(username, entry);
            }
        }

        saveUsers();
    }

    /**
     * Get the entry for a username
     * 
     * @param username
     *        The username
     * @return The entry
     */
    public UserTableEntry getUserTableEntry(String username)
    {
        return getUserTableEntry(username, false);
    }

    /**
     * Get the entry for a username, optionally creating a new entry if one is
     * not found in the table.
     * 
     * @param username
     *        The username
     * @param createIfNecessary
     *        True to create when it does not already exist
     * @return The entry
     */
    public UserTableEntry getUserTableEntry(String username, boolean createIfNecessary)
    {
        if (username == null) return null;

        UserTableEntry entry = userTable.get(username);

        if (entry == null && createIfNecessary) {
            entry = new UserTableEntry();
            entry.setUsername(username);
            userTable.put(username, entry);

            UvmContextFactory.context().logEvent(new UserTableEvent(username, "add", null, null));
            UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_ADD, username);
        }

        return entry;
    }

    /**
     * Save the entry for a username in the table
     * 
     * @param username
     *        The username
     * @param entry
     *        The entry
     */
    public void setUserTableEntry(String username, UserTableEntry entry)
    {
        userTable.put(username, entry);
    }

    /**
     * Get the list of users
     * 
     * @return The list of users
     */
    public synchronized LinkedList<UserTableEntry> getUsers()
    {
        return new LinkedList<>(userTable.values());
    }

    /**
     * Set a quota for a user
     * 
     * @param username
     *        The username
     * @param quotaBytes
     *        Quota size
     * @param time_sec
     *        Quota time
     * @param reason
     *        Reason for the quota
     */
    public void giveUserQuota(String username, long quotaBytes, int time_sec, String reason)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return;
        }
        UserTableEntry entry = getUserTableEntry(username, true);
        long now = System.currentTimeMillis();

        /* If there already is a quota and it will be reset */
        entry.setQuotaSize(quotaBytes);
        entry.setQuotaRemaining(quotaBytes);
        entry.setQuotaIssueTime(now);
        entry.setQuotaExpirationTime(now + (((long) time_sec) * 1000L));

        /* Call hook listeners */
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_QUOTA_GIVEN, username);

        UvmContextFactory.context().logEvent(new QuotaEvent(QuotaEvent.ACTION_GIVEN, username, reason, quotaBytes));

        return;
    }

    /**
     * Remove the quota for a username
     * 
     * @param username
     *        The username
     */
    public void removeQuota(String username)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return;
        }
        UserTableEntry entry = getUserTableEntry(username);
        if (entry == null) return;

        entry.setQuotaSize(0);
        entry.setQuotaRemaining(0);
        entry.setQuotaIssueTime(0);
        entry.setQuotaExpirationTime(0);

        /* Call hook listeners */
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_QUOTA_REMOVED, username);
    }

    /**
     * See if a user has exceeded their quota
     * 
     * @param username
     *        The username
     * @return True if exceeded, otherwise false
     */
    public boolean userQuotaExceeded(String username)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return false;
        }
        UserTableEntry entry = getUserTableEntry(username);
        if (entry == null) return false;
        if (entry.getQuotaSize() <= 0) return false;

        /**
         * Check if its expired, if it is - remove the quota
         */
        long now = System.currentTimeMillis();
        if (now > entry.getQuotaExpirationTime()) {
            removeQuota(username);
            return false;
        }

        if (entry.getQuotaRemaining() <= 0) return true;
        return false;
    }

    /**
     * Check quota attainment for a user
     * 
     * @param username
     *        The username
     * @return The attainment value
     */
    public double userQuotaAttainment(String username)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return 0.0;
        }
        UserTableEntry entry = getUserTableEntry(username);
        if (entry == null) return 0.0;
        if (entry.getQuotaSize() <= 0) return 0.0;

        /**
         * Check if its expired, if it is - remove the quota
         */
        long now = System.currentTimeMillis();
        if (now > entry.getQuotaExpirationTime()) {
            removeQuota(username);
            return 0.0;
        }

        long quotaRemaining = entry.getQuotaRemaining();
        long quotaSize = entry.getQuotaSize();
        long quotaUsed = quotaSize - quotaRemaining;

        long quotaUsedK = quotaUsed / 1000;
        long quotaSizeK = quotaSize / 1000;

        return ((double) quotaUsedK) / ((double) quotaSizeK);
    }

    /**
     * Refil the quota for a user
     * 
     * @param username
     *        The username
     */
    public void refillQuota(String username)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return;
        }
        UserTableEntry entry = getUserTableEntry(username);
        if (entry == null) return;
        if (entry.getQuotaSize() <= 0) return;

        entry.setQuotaRemaining(entry.getQuotaSize());

        /* Call hook listeners */
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_QUOTA_GIVEN, username);
    }

    /**
     * Decrease the quota for a user by the provided amount
     * 
     * @param username
     *        The username
     * @param bytes
     *        The amount to decrease
     * @return True if successful, otherwise false
     */
    public boolean decrementQuota(String username, long bytes)
    {
        if (username == null) {
            logger.warn("Invalid argument: username is null");
            return false;
        }
        UserTableEntry entry = getUserTableEntry(username);
        if (entry == null) return false;
        if (entry.getQuotaSize() <= 0) return false;

        /**
         * Decrement
         */
        long remaining = entry.getQuotaRemaining();
        long newRemaning = remaining - bytes;
        entry.setQuotaRemaining(newRemaning);

        if (remaining > 0 && newRemaning <= 0) {
            logger.info("User " + username + " exceeded quota.");

            /* Call hook listeners */
            UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_QUOTA_EXCEEDED, username);

            UvmContextFactory.context().logEvent(new QuotaEvent(QuotaEvent.ACTION_EXCEEDED, username, null, entry.getQuotaSize()));
            return true;
        }

        return false;
    }

    /**
     * Get the size of the user table
     * 
     * @return The size
     */
    public int getCurrentSize()
    {
        return this.userTable.size();
    }

    /**
     * Clear the user table
     */
    public void clear()
    {
        this.userTable.clear();
    }

    /**
     * Remove the specified user from the table
     * 
     * @param username
     *        The username
     * @return The entry that was removed, or null if not found
     */
    public UserTableEntry removeUserTableEntry(String username)
    {
        if (username == null) {
            logger.warn("Invalid argument: " + username);
            return null;
        }
        logger.info("Removing user table entry: " + username);

        UserTableEvent event = new UserTableEvent(username, "remove", null, null);
        UvmContextFactory.context().logEvent(event);

        UserTableEntry removed = userTable.remove(username);
        UvmContextFactory.context().hookManager().callCallbacks(HookManager.USER_TABLE_REMOVE, username);
        return removed;
    }

    /**
     * Save the user table to a file
     */
    @SuppressWarnings("unchecked")
    public void saveUsers()
    {
        lastSaveTime = System.currentTimeMillis();

        try {
            Collection<UserTableEntry> entries = userTable.values();
            logger.info("Saving users to file... (" + entries.size() + " entries)");

            LinkedList<UserTableEntry> list = new LinkedList<>();
            for (UserTableEntry entry : entries) {
                list.add(entry);
            }

            if (list.size() > HIGH_WATER_SIZE) {
                logger.info("User table over max size, pruning oldest entries"); // remove entries with oldest (lowest) lastSeenTime
                Collections.sort(list, new Comparator<UserTableEntry>()
                {
                    /**
                     * Compare function for locating old entries
                     * 
                     * @param o1
                     *        Entry one
                     * @param o2
                     *        Entry two
                     * @return Comparison result
                     */
                    public int compare(UserTableEntry o1, UserTableEntry o2)
                    {
                        if (o1.getLastAccessTime() < o2.getLastAccessTime()) return 1;
                        if (o1.getLastAccessTime() == o2.getLastAccessTime()) return 0;
                        return -1;
                    }
                });
                while (list.size() > LOW_WATER_SIZE) {
                    logger.info("User table too large. Removing oldest entry: " + list.get(list.size() - 1));
                    list.removeLast();
                }
            }

            UvmContextFactory.context().settingsManager().save(USERS_SAVE_FILENAME, list, false, true);
            logger.info("Saving users to file... done");
        } catch (Exception e) {
            logger.warn("Exception", e);
        }
    }

    /**
     * Load the user table from a file
     */
    @SuppressWarnings("unchecked")
    private void loadSavedUsers()
    {
        try {
            this.userTable = new ConcurrentHashMap<>();

            logger.info("Loading users from file...");
            LinkedList<UserTableEntry> savedEntries = UvmContextFactory.context().settingsManager().load(LinkedList.class, USERS_SAVE_FILENAME);
            if (savedEntries == null) {
                logger.info("Loaded  users from file.   (no users saved)");
            } else {
                for (UserTableEntry entry : savedEntries) {
                    try {
                        // if its invalid just ignore it
                        if (entry.getUsername() == null) {
                            logger.warn("Invalid entry: " + entry.toJSONString());
                            continue;
                        }

                        userTable.put(entry.getUsername(), entry);
                    } catch (Exception e) {
                        logger.warn("Error loading user entry: " + entry.toJSONString(), e);
                    }
                }
                logger.info("Loaded  users from file.   (" + savedEntries.size() + " entries)");
            }
        } catch (Exception e) {
            logger.warn("Failed to load users", e);
        }
    }

    /**
     * This thread periodically walks through the entries and removes expired
     * entries It also cleans up some metadata
     */
    private class UserTableCleaner implements Runnable
    {
        /**
         * Thread run function
         */
        public void run()
        {
            cleanerThread = Thread.currentThread();

            while (cleanerThread != null) {
                try {
                    Thread.sleep(CLEANER_SLEEP_TIME_MILLI);
                } catch (Exception e) {
                }
                logger.debug("UserTableCleaner: Running... ");

                try {
                    Long now = System.currentTimeMillis();
                    /**
                     * Remove old entries
                     */
                    LinkedList<UserTableEntry> entries = new LinkedList<>(userTable.values());
                    for (UserTableEntry entry : entries) {
                        String username = entry.getUsername();
                        if (username == null) continue;

                        /**
                         * Check quota expiration Remove from quota if expired
                         */
                        if (entry.getQuotaSize() > 0) {
                            long expireTime = entry.getQuotaExpirationTime();
                            if (now > expireTime) {
                                removeQuota(username);
                            }
                        }

                        /**
                         * If this user hasnt been touched recently, delete it
                         */
                        if (now > (entry.getLastAccessTime() + CLEANER_LAST_ACCESS_MAX_TIME)) {

                            /**
                             * If this user table entry is storing vital
                             * information, don't delete it
                             */
                            if (entry.getQuotaSize() > 0) {
                                continue;
                            }
                            if (entry.getTags() != null && entry.getTags().size() > 0) {
                                continue;
                            }

                            /**
                             * Otherwise just delete the entire entry
                             */
                            else {
                                logger.debug("UserTableCleaner: Removing " + username);

                                removeUserTableEntry(username);
                                continue;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Exception while cleaning user table", e);
                }
            }
        }
    }

    /**
     * User table saver
     */
    private class UserTableSaver implements Runnable
    {
        /**
         * Thread run function
         */
        public void run()
        {
            saveUsers();
        }
    }
}
