/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeUserTableImpl implements SafeUserTable
{
    private final Supplier<UserTable> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeUserTableImpl(Supplier<UserTable> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getUsers through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LinkedList<UserTableEntry> getUsers()
    {
        return delegate.get().getUsers();
    }

    /**
     * refillQuota through the reviewed RPC contract.
     * @param username The username argument
     */
    @Override
    public void refillQuota(String username)
    {
        delegate.get().refillQuota(username);
    }

    /**
     * removeQuota through the reviewed RPC contract.
     * @param username The username argument
     */
    @Override
    public void removeQuota(String username)
    {
        delegate.get().removeQuota(username);
    }

    /**
     * setUsers through the reviewed RPC contract.
     * @param users The users argument
     */
    @Override
    public void setUsers(LinkedList<UserTableEntry> users)
    {
        delegate.get().setUsers(users);
    }
}
