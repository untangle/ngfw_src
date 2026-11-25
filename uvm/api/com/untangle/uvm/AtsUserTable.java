package com.untangle.uvm;

/**
 * Combined Safe and additional ATS operations.
 */
public interface AtsUserTable extends SafeUserTable
{
    /**
     * decrementQuota through the additional ATS contract.
     * @param username The username argument
     * @param bytes The bytes argument
     * @return The delegated result
     */
    boolean decrementQuota(String username, long bytes);

    /**
     * getUserTableEntry through the additional ATS contract.
     * @param addr The addr argument
     * @param create The create argument
     * @return The delegated result
     */
    UserTableEntry getUserTableEntry(String addr, boolean create);

    /**
     * giveUserQuota through the additional ATS contract.
     * @param username The username argument
     * @param quotaBytes The quotaBytes argument
     * @param time_sec The time_sec argument
     * @param reason The reason argument
     */
    void giveUserQuota(String username, long quotaBytes, int time_sec, String reason);

    /**
     * setUserTableEntry through the additional ATS contract.
     * @param addr The addr argument
     * @param entry The entry argument
     */
    void setUserTableEntry(String addr, UserTableEntry entry);
}
