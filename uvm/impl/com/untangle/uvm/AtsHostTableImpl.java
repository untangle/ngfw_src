package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsHostTableImpl implements AtsHostTable
{
    private final SafeHostTable safe;
    private final Supplier<HostTable> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsHostTableImpl(SafeHostTable safe, Supplier<HostTable> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentActiveSize()
    {
        return safe.getCurrentActiveSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LinkedList<HostTableEntry> getHosts()
    {
        return safe.getHosts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaxActiveSize()
    {
        return safe.getMaxActiveSize();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refillQuota(InetAddress address)
    {
        safe.refillQuota(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeQuota(InetAddress address)
    {
        safe.removeQuota(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHosts(LinkedList<HostTableEntry> hosts, boolean merge)
    {
        safe.setHosts(hosts, merge);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void cleanup()
    {
        delegate.get().cleanup();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        delegate.get().clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean decrementQuota(InetAddress address, long bytes)
    {
        return delegate.get().decrementQuota(address, bytes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostTableEntry getHostTableEntry(String inetAddress)
    {
        return delegate.get().getHostTableEntry(inetAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void giveHostQuota(InetAddress address, long quotaBytes, int time_sec, String reason)
    {
        delegate.get().giveHostQuota(address, quotaBytes, time_sec, reason);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public HostTableEntry removeHostTableEntry(InetAddress address)
    {
        return delegate.get().removeHostTableEntry(address);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHostTableEntry(InetAddress address, HostTableEntry entry)
    {
        delegate.get().setHostTableEntry(address, entry);
    }
}
