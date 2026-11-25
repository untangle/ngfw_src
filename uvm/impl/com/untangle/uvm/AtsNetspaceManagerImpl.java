package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.NetspaceManager.IPVersion;
import com.untangle.uvm.NetspaceManager.NetworkSpace;
import com.untangle.uvm.app.IPMaskedAddress;

/**
 * Composed ATS facade; internal delegates are never exported.
 */
public final class AtsNetspaceManagerImpl implements AtsNetspaceManager
{
    private final SafeNetspaceManager safe;
    private final Supplier<NetspaceManager> delegate;

    /**
     * Create the composed ATS facade.
     * @param safe The normal Safe facade
     * @param delegate The internal delegate
     */
    AtsNetspaceManagerImpl(SafeNetspaceManager safe, Supplier<NetspaceManager> delegate)
    {
        this.safe = Objects.requireNonNull(safe, "safe");
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NetworkSpace isNetworkAvailable(String ownerName, String networkText)
    {
        return safe.isNetworkAvailable(ownerName, networkText);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IPMaskedAddress getAvailableAddressSpace(IPVersion version, int hostId)
    {
        return delegate.get().getAvailableAddressSpace(version, hostId);
    }
}
