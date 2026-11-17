/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;

/**
 * Reviewed callable operations for SafeUserTable.
 */
public interface SafeUserTable
{
    /**
     * getUsers through the reviewed RPC contract.
     * @return The delegated result
     */
    LinkedList<UserTableEntry> getUsers();

    /**
     * refillQuota through the reviewed RPC contract.
     * @param username The username argument
     */
    void refillQuota(String username);

    /**
     * removeQuota through the reviewed RPC contract.
     * @param username The username argument
     */
    void removeQuota(String username);

    /**
     * setUsers through the reviewed RPC contract.
     * @param users The users argument
     */
    void setUsers(LinkedList<UserTableEntry> users);
}
