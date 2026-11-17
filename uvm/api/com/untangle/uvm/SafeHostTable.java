/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;

/**
 * Reviewed callable operations for SafeHostTable.
 */
public interface SafeHostTable
{
    /**
     * getCurrentActiveSize through the reviewed RPC contract.
     * @return The delegated result
     */
    int getCurrentActiveSize();

    /**
     * getHosts through the reviewed RPC contract.
     * @return The delegated result
     */
    LinkedList<HostTableEntry> getHosts();

    /**
     * getMaxActiveSize through the reviewed RPC contract.
     * @return The delegated result
     */
    int getMaxActiveSize();

    /**
     * refillQuota through the reviewed RPC contract.
     * @param address The address argument
     */
    void refillQuota(InetAddress address);

    /**
     * removeQuota through the reviewed RPC contract.
     * @param address The address argument
     */
    void removeQuota(InetAddress address);

    /**
     * setHosts through the reviewed RPC contract.
     * @param hosts The hosts argument
     * @param merge The merge argument
     */
    void setHosts(LinkedList<HostTableEntry> hosts, boolean merge);
}
