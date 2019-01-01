/**
 * $Id$
 */

package com.untangle.app.captive_portal;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;

/**
 * Implementation of the user table used to track active captive portal users.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalUserTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private ConcurrentHashMap<String, CaptivePortalUserEntry> activeUserTable = null;
    private CaptivePortalApp ownerApp = null;

    /**
     * Used by the timer task for generating a linked list of users that should
     * be logged out due to session or inactivity timeout.
     */
    public class StaleUser
    {
        CaptivePortalUserEvent.EventType reason;
        String useraddr;

        /**
         * Constructor
         * 
         * @param useraddr
         *        The address of the stale user
         * @param reason
         *        The reason for the logout
         */
        StaleUser(String useraddr, CaptivePortalUserEvent.EventType reason)
        {
            this.useraddr = useraddr;
            this.reason = reason;
        }
    }

    /**
     * Constructs an instance of the user table.
     * 
     * @param ownerApp
     *        The application instance that created the table.
     */
    public CaptivePortalUserTable(CaptivePortalApp ownerApp)
    {
        this.ownerApp = ownerApp;
        activeUserTable = new ConcurrentHashMap<String, CaptivePortalUserEntry>();
    }

    /**
     * Creates an ArrayList of all users in the table.
     * 
     * @return list of all users
     */
    public ArrayList<CaptivePortalUserEntry> buildUserList()
    {
        ArrayList<CaptivePortalUserEntry> userList = new ArrayList<CaptivePortalUserEntry>();
        userList.addAll(activeUserTable.values());
        return (userList);
    }

    /**
     * Insert a user in the table.
     * 
     * @param useraddr
     *        The address (IP or MAC) of the user
     * @param username
     *        The name of the user
     * @param anonymous
     *        Set when users are anonymous with no username
     * @return
     */
    public CaptivePortalUserEntry insertActiveUser(String useraddr, String username, Boolean anonymous)
    {
        CaptivePortalUserEntry userEntry = new CaptivePortalUserEntry(useraddr, username, anonymous);
        activeUserTable.put(useraddr, userEntry);
        logger.debug("INSERT USER: " + userEntry.toString());
        return (userEntry);
    }

    /**
     * Insert a user in the table
     * 
     * @param userEntry
     *        The object to be inserted in the table
     * @return The object that was inserted in the table
     */
    protected CaptivePortalUserEntry insertActiveUser(CaptivePortalUserEntry userEntry)
    {
        logger.debug("INSERT USER: " + userEntry.toString());
        activeUserTable.put(userEntry.getUserAddress(), userEntry);
        return (userEntry);
    }

    /**
     * Remove a user from the table
     * 
     * @param useraddr
     *        The address (IP or MAC) of the user to remove
     * @return true if user is found and removed, otherwise false
     */
    public boolean removeActiveUser(String useraddr)
    {
        // find and remove from the active user table
        CaptivePortalUserEntry user = searchByAddress(useraddr);
        if (user == null) return (false);

        logger.debug("REMOVE USER: " + user.toString());
        activeUserTable.remove(user.getUserAddress());
        return (true);
    }

    /**
     * Search by address for a user in the table
     * 
     * @param netaddr
     *        The address for the search
     * @return The matching user entry or null if not found
     */
    public CaptivePortalUserEntry searchByAddress(String netaddr)
    {
        CaptivePortalUserEntry item = activeUserTable.get(netaddr);
        return (item);
    }

    /**
     * Search by name for a user in the table
     * 
     * @param username
     *        The username for the search
     * @param ignoreCase
     *        Set when case should be ignored when searching
     * @return The matching user entry or null if not found
     */
    public CaptivePortalUserEntry searchByUsername(String username, boolean ignoreCase)
    {
        CaptivePortalUserEntry item;

        for (String address : activeUserTable.keySet()) {
            item = activeUserTable.get(address);

            // if the ignoreCase flag is set we compare both as lowercase
            if (ignoreCase == true) {
                if (username.toLowerCase().equals(item.getUserName().toLowerCase()) == true) return (item);
            }

            // ignoreCase flag is not set so do a direct comparison
            else {
                if (username.equals(item.getUserName()) == true) return (item);
            }
        }

        return (null);
    }

    /**
     * Builds a list of stale users that should be logged out.
     * 
     * @param idleTimeout
     *        The idle timeout to apply or zero if disabled
     * @param userTimeout
     *        The maximum amount of time a user can stay authenticated before
     *        they are forced to log in again
     * @return A list of users to be logged out
     */
    public ArrayList<StaleUser> buildStaleList(long idleTimeout, long userTimeout)
    {
        ArrayList<StaleUser> wipelist = new ArrayList<StaleUser>();
        HostTableEntry entry = null;
        long currentTime = (System.currentTimeMillis() / 1000);
        long idleTrigger = 0;
        long userTrigger = 0;
        int wipecount = 0;
        StaleUser stale;

        for (String address : activeUserTable.keySet()) {
            CaptivePortalUserEntry item = activeUserTable.get(address);
            userTrigger = ((item.getSessionCreation() / 1000) + userTimeout);

            if (address.indexOf(':') >= 0) {
                // any semi-colon in the key indicate a MAC address                
                entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(address);
            } else {
                // not a mac address so do normal lookup by IP address
                entry = UvmContextFactory.context().hostTable().getHostTableEntry(address);
            }

            if (entry != null) {
                idleTrigger = (entry.getLastSessionTime() / 1000) + idleTimeout;
            } else {
                logger.warn("HostTableEntry missing for logged in Captive Portal user: " + item.toString());
                idleTrigger = ((item.getSessionActivity() / 1000) + userTimeout);
            }

            // look for users with no traffic within the configured non-zero idle timeout
            if ((idleTimeout > 0) && (currentTime > idleTrigger)) {
                logger.info("Idle timeout removing user " + item.toString());
                stale = new StaleUser(address, CaptivePortalUserEvent.EventType.INACTIVE);
                wipelist.add(stale);
                wipecount++;
            }

            // look for users who have exceeded the configured maximum session time
            if (currentTime > userTrigger) {
                logger.info("Session timeout removing user " + item.toString());
                stale = new StaleUser(address, CaptivePortalUserEvent.EventType.TIMEOUT);
                wipelist.add(stale);
                wipecount++;
            }
        }

        return (wipelist);
    }

    /**
     * Clear the user table. First we remove the captive portal user name and
     * authenticated flag from the host table for each user, and then we clear
     * our hash table.
     */
    public void purgeAllUsers()
    {
        HostTableEntry entry;

        for (String address : activeUserTable.keySet()) {
            CaptivePortalUserEntry user = activeUserTable.get(address);
            if (address.indexOf(':') >= 0) {
                // any semi-colon in the key indicate a MAC address
                entry = UvmContextFactory.context().hostTable().findHostTableEntryByMacAddress(address);
            } else {
                // not a mac address so do normal lookup by IP address
                entry = UvmContextFactory.context().hostTable().getHostTableEntry(address);
            }

            if (entry == null) continue;

            logger.debug("Purging host table entry for " + user.toString());
            entry.setUsernameCaptivePortal(null);
            entry.setCaptivePortalAuthenticated(false);
        }

        activeUserTable.clear();
    }
}
