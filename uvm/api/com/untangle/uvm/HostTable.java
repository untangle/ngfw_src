/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Hashtable;
import java.net.InetAddress;

/**
 * The Host Table is responsible for storing known information about hosts.
 * Many different components use the host table to share known information about a given host (for example, its hostname).
 * Each host has a table of information known about it and these "attachments" can be read/written using keys
 *
 * The host table also contains penalty box methods which maintain all the host table attachments related to penalty box logic
 */
public interface HostTable
{
    
    /**
     * Gets the HostTableEtry for the specified host
     * Returns null if no entry for the provided address is found.
     */
    HostTableEntry getHostTableEntry( InetAddress addr );

    /**
     * Gets the HostTableEtry for the specified host
     * If create is true a new entry will be created if no entry exists
     */
    HostTableEntry getHostTableEntry( InetAddress addr, boolean create );

    /**
     * return the "license size" (the number of hosts applicable to licensing)
     */
    int getCurrentActiveSize();

    /**
     * return the largest license size the table has ever been
     */
    int getMaxActiveSize();
    
    /**
     * Save the specified entry for the specified addr
     * Will overwrite existing value
     */
    void setHostTableEntry( InetAddress addr, HostTableEntry entry );

    /**
     * Returns a duplicated list of all current hosts
     */
    LinkedList<HostTableEntry> getHosts();

    /**
     * Add a host to the penalty box for the specified amount of time at the specified priority
     * This sets all the appropriate attachments and calls the listeners
     */
    void addHostToPenaltyBox( InetAddress address, int time_sec, String reason );

    /**
     * Release a host from the penalty box
     * This sets all the appropriate attachments and calls the listeners
     */
    void releaseHostFromPenaltyBox( InetAddress address );
    
    /**
     * Checks if a host is in the penalty box
     */
    boolean hostInPenaltyBox( InetAddress address );

    /**
     * Returns a current list of all hosts in the penalty box
     * This is used by the UI to display the list
     */
    LinkedList<HostTableEntry> getPenaltyBoxedHosts();

    /**
     * Give an address a quota
     * Utility function to set the appropriate attachment values
     */
    void giveHostQuota( InetAddress address, long quotaBytes, int time_sec, String reason );

    /**
     * Remove a quota from the provided address
     * Utility function to set the appropriate attachment values
     */
    void removeQuota( InetAddress address );

    /**
     * Refill an existing quota
     * Will do nothing if the address does not have a quota
     * Utility function to set the appropriate attachment values
     */
    void refillQuota( InetAddress address );

    /**
     * Decrement the available quota by the provided amount
     * Utility function to set the appropriate attachment values
     */
    boolean decrementQuota(InetAddress addr, long bytes);
    
    /**
     * Check if the provided address has a quota that is exceeded
     */
    boolean hostQuotaExceeded( InetAddress address );

    /**
     * Get the quota attainment ratio for an address
     */
    double hostQuotaAttainment( InetAddress address );
    
    /**
     * Return a list of all the table entries for hosts with quotas
     * This is used for display in the UI
     */
    LinkedList<HostTableEntry> getQuotaHosts();

    /**
     * Clear the entire table (used by tests)
     */
    void clearTable();

    /**
     * Get the current size of the table
     */
    int getCurrentSize();
    
    /**
     * Set the host table to these entries
     */
    void setHosts( LinkedList<HostTableEntry> devices );

    /**
     * save the hosts to disk
     */
    void saveHosts();

    /**
     * Remove a host table entry
     * returns the entry removed (or null if not found)
     */
    HostTableEntry removeHostTableEntry( InetAddress addr );
}

