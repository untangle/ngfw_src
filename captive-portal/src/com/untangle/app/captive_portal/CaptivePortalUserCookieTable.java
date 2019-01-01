/**
 * $Id: CaptivePortalUserCookieTable.java 39560 2015-01-22 20:46:37Z dmorris $
 */

package com.untangle.app.captive_portal;

import java.util.concurrent.ConcurrentHashMap;
import org.apache.log4j.Logger;

/**
 * When a user is forced to log out, their cookie must be expired. There's no
 * way to push this, so the list of logged out users is maintained and the next
 * time the user hits the handler script, the script checks and issues the
 * "expire cookie".
 * 
 * The table is not saved.
 * 
 * @author mahotz
 * 
 */

public class CaptivePortalUserCookieTable
{
    private final Logger logger = Logger.getLogger(getClass());
    private ConcurrentHashMap<String, CaptivePortalUserEntry> userTable;

    /**
     * Constructor
     */
    public CaptivePortalUserCookieTable()
    {
        userTable = new ConcurrentHashMap<>();
    }

    /**
     * Inserts a user in the cookie table.
     * 
     * @param user
     *        The user object to be inserted
     * @return The user object that was inserted
     */
    public CaptivePortalUserEntry insertInactiveUser(CaptivePortalUserEntry user)
    {
        userTable.put(user.getUserAddress(), user);
        logger.debug("INSERT cookie user: " + user.toString());
        return (user);
    }

    /**
     * Remove a user from the cookie table.
     * 
     * @param address
     *        The address of the user to be removed
     * @return True if found and removed, otherwise false
     */
    public boolean removeActiveUser(String address)
    {
        CaptivePortalUserEntry user = userTable.get(address);
        if (user == null) return (false);
        userTable.remove(address);
        logger.debug("REMOVE cookie user: " + user.toString());
        return (true);
    }

    /**
     * Search for a user in the cookie table.
     * 
     * @param address
     *        The address of the user to be located.
     * @param username
     *        The username of the user to be located.
     * @return The user if both args match, otherwise null
     */
    public CaptivePortalUserEntry searchByAddressUsername(String address, String username)
    {
        CaptivePortalUserEntry user = userTable.get(address);
        if ((user != null) && (user.getUserName().equals(username))) {
            return (user);
        }
        return null;
    }

    /**
     * Clear the cookie table
     */
    public void purgeAllUsers()
    {
        userTable.clear();
    }
}
