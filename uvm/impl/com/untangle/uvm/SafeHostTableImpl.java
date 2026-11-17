/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeHostTableImpl implements SafeHostTable
{
    private final Supplier<HostTable> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeHostTableImpl(Supplier<HostTable> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getCurrentActiveSize through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public int getCurrentActiveSize()
    {
        return delegate.get().getCurrentActiveSize();
    }

    /**
     * getHosts through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public LinkedList<HostTableEntry> getHosts()
    {
        return delegate.get().getHosts();
    }

    /**
     * getMaxActiveSize through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public int getMaxActiveSize()
    {
        return delegate.get().getMaxActiveSize();
    }

    /**
     * refillQuota through the reviewed RPC contract.
     * @param address The address argument
     */
    @Override
    public void refillQuota(InetAddress address)
    {
        delegate.get().refillQuota(address);
    }

    /**
     * removeQuota through the reviewed RPC contract.
     * @param address The address argument
     */
    @Override
    public void removeQuota(InetAddress address)
    {
        delegate.get().removeQuota(address);
    }

    /**
     * setHosts through the reviewed RPC contract.
     * @param hosts The hosts argument
     * @param merge The merge argument
     */
    @Override
    public void setHosts(LinkedList<HostTableEntry> hosts, boolean merge)
    {
        delegate.get().setHosts(hosts, merge);
    }
}
