package com.untangle.uvm;

import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsUserTableImpl implements AtsUserTable
{
    private final SafeUserTable safe;
    private final Supplier<UserTable> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsUserTableImpl(SafeUserTable safe, Supplier<UserTable> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedList<UserTableEntry> getUsers()
    {
        return safe.getUsers();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refillQuota(String username)
    {
        safe.refillQuota(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeQuota(String username)
    {
        safe.removeQuota(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUsers(LinkedList<UserTableEntry> users)
    {
        safe.setUsers(users);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean decrementQuota(String username, long bytes)
    {
        return delegate.get().decrementQuota(username, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserTableEntry getUserTableEntry(String addr, boolean create)
    {
        return delegate.get().getUserTableEntry(addr, create);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveUserQuota(String username, long quotaBytes, int time_sec, String reason)
    {
        delegate.get().giveUserQuota(username, quotaBytes, time_sec, reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUserTableEntry(String addr, UserTableEntry entry)
    {
        delegate.get().setUserTableEntry(addr, entry);
    }
}
