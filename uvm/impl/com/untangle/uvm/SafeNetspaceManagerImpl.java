/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeNetspaceManagerImpl implements SafeNetspaceManager
{
    private final Supplier<NetspaceManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeNetspaceManagerImpl(Supplier<NetspaceManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * isNetworkAvailable through the reviewed RPC contract.
     * @param ownerName The ownerName argument
     * @param networkText The networkText argument
     * @return The delegated result
     */
    @Override
    public NetspaceManager.NetworkSpace isNetworkAvailable(String ownerName, String networkText)
    {
        return delegate.get().isNetworkAvailable(ownerName, networkText);
    }
}
