/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.net.InetAddress;

/**
 * The Host Table is responsible for storing known information about hosts.
 * Many different components use the host table to share known information about a given host (for example, its hostname).
 */
public interface HostTable
{

    /**
     * Gets the HostTableEtry for the specified host (specified as an IP string)
     * Returns null if no entry for the provided address is found.
     */
    HostTableEntry getHostTableEntry( String inetAddress );

    /**
     * Gets the HostTableEtry for the specified host
     * Returns null if no entry for the provided address is found.
     */
    HostTableEntry getHostTableEntry( InetAddress address );

    /**
     * Gets the HostTableEtry for the specified host
     * If create is true a new entry will be created if no entry exists
     */
    HostTableEntry getHostTableEntry( InetAddress address, boolean create );

    /**
     * Search for a HostTableEntry with specified MAC address
     */
    HostTableEntry findHostTableEntryByMacAddress( String macaddr );

    /**
     * Search for a HostTableEntry with specified IPsec username
     */
    HostTableEntry findHostTableEntryByIpsecUsername( String username );

    /**
     * return the "license size" (the number of hosts applicable to licensing)
     */
    int getCurrentActiveSize();

    /**
     * return the largest license size the table has ever been
     */
    int getMaxActiveSize();

    /**
     * Save the specified entry for the specified address
     * Will overwrite existing value
     */
    void setHostTableEntry( InetAddress address, HostTableEntry entry );

    /**
     * Returns a duplicated list of all current hosts
     */
    LinkedList<HostTableEntry> getHosts();

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
    boolean decrementQuota(InetAddress address, long bytes);

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
    void clear();

    /**
     * Launch the cleanup thread immediately
     */
    void cleanup();

    /**
     * Get the current size of the table
     */
    int getCurrentSize();

    /**
     * Set the host table to these entries
     */
    void setHosts( LinkedList<HostTableEntry> hosts, boolean merge );

    /**
     * save the hosts to disk
     */
    void saveHosts();

    /**
     * Remove a host table entry
     * returns the entry removed (or null if not found)
     */
    HostTableEntry removeHostTableEntry( InetAddress address );
}
