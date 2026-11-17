/**
 * $Id$
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Exposes daemon status without exposing lifecycle or monitoring operations.
 * Callable references are registered by concrete class, so this facade must
 * remain separate from the full DaemonManager implementation.
 */
public final class SafeDaemonManagerImpl implements SafeDaemonManager
{
    private final Supplier<DaemonManager> delegate;

    /**
     * Create a status facade for the internal manager.
     *
     * @param delegate The internal daemon manager
     */
    SafeDaemonManagerImpl(DaemonManager delegate)
    {
        this(() -> delegate);
    }

    /**
     * Create a status facade without initializing the internal manager.
     * @param delegate The current internal manager supplier
     */
    SafeDaemonManagerImpl(Supplier<DaemonManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Check whether a daemon is running.
     *
     * @param daemonName The daemon name
     * @return True if the daemon is running, otherwise false
     */
    @Override
    public boolean isRunning(String daemonName)
    {
        return delegate.get().isRunning(daemonName);
    }
}
