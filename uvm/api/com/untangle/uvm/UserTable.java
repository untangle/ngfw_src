/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;

/**
 * The User Table is responsible for storing known information about user.
 * Many different components use the user table to share known information about a given user (for example, its quota).
 */
public interface UserTable
{
    
    /**
     * Gets the UserTableEntry for the specified username
     * Returns null if no entry for the provided username is found.
     */
    UserTableEntry getUserTableEntry( String addr );

    /**
     * Gets the UserTableEntry for the specified username
     * If create is true a new entry will be created if no entry exists
     */
    UserTableEntry getUserTableEntry( String addr, boolean create );

    /**
     * Save the specified entry for the specified addr
     * Will overwrite existing value
     */
    void setUserTableEntry( String addr, UserTableEntry entry );

    /**
     * Returns a duplicated list of all current users
     */
    LinkedList<UserTableEntry> getUsers();

    /**
     * Give an username a quota
     * Utility function to set the appropriate attachment values
     */
    void giveUserQuota( String username, long quotaBytes, int time_sec, String reason );

    /**
     * Remove a quota from the provided username
     * Utility function to set the appropriate attachment values
     */
    void removeQuota( String username );

    /**
     * Refill an existing quota
     * Will do nothing if the username does not have a quota
     * Utility function to set the appropriate attachment values
     */
    void refillQuota( String username );

    /**
     * Decrement the available quota by the provided amount
     * Utility function to set the appropriate attachment values
     */
    boolean decrementQuota( String username, long bytes );
    
    /**
     * Check if the provided username has a quota that is exceeded
     */
    boolean userQuotaExceeded( String username );

    /**
     * Get the quota attainment ratio for an username
     */
    double userQuotaAttainment( String username );
    
    /**
     * Clear the entire table (used by tests)
     */
    void clear();

    /**
     * Get the current size of the table
     */
    int getCurrentSize();
    
    /**
     * save the users to disk
     */
    void saveUsers();

    /**
     * Remove a user table entry
     * returns the entry removed (or null if not found)
     */
    UserTableEntry removeUserTableEntry( String username );
}
