/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeNetcapManagerImpl implements SafeNetcapManager
{
    private final Supplier<NetcapManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeNetcapManagerImpl(Supplier<NetcapManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * setJNetcapDebugLevel through the reviewed RPC contract.
     * @param level The level argument
     */
    @Override
    public void setJNetcapDebugLevel(int level)
    {
        ((NetcapManagerImpl) delegate.get()).setJNetcapDebugLevel(level);
    }

    /**
     * setNetcapDebugLevel through the reviewed RPC contract.
     * @param level The level argument
     */
    @Override
    public void setNetcapDebugLevel(int level)
    {
        ((NetcapManagerImpl) delegate.get()).setNetcapDebugLevel(level);
    }
}
