package com.untangle.uvm;

import java.net.InetAddress;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsHostTable extends SafeHostTable
{
    /**
     * cleanup through the additional ATS contract.
     */
    void cleanup();

    /**
     * clear through the additional ATS contract.
     */
    void clear();

    /**
     * decrementQuota through the additional ATS contract.
     * @param address The address argument
     * @param bytes The bytes argument
     * @return The delegated result
     */
    boolean decrementQuota(InetAddress address, long bytes);

    /**
     * getHostTableEntry through the additional ATS contract.
     * @param inetAddress The inetAddress argument
     * @return The delegated result
     */
    HostTableEntry getHostTableEntry(String inetAddress);

    /**
     * giveHostQuota through the additional ATS contract.
     * @param address The address argument
     * @param quotaBytes The quotaBytes argument
     * @param time_sec The time_sec argument
     * @param reason The reason argument
     */
    void giveHostQuota(InetAddress address, long quotaBytes, int time_sec, String reason);

    /**
     * removeHostTableEntry through the additional ATS contract.
     * @param address The address argument
     * @return The delegated result
     */
    HostTableEntry removeHostTableEntry(InetAddress address);

    /**
     * setHostTableEntry through the additional ATS contract.
     * @param address The address argument
     * @param entry The entry argument
     */
    void setHostTableEntry(InetAddress address, HostTableEntry entry);
}
