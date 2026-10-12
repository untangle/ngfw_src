/**
 * $Id$
 */
package com.untangle.uvm;

/**
 * Exposes daemon status without exposing lifecycle or monitoring operations.
 * Callable references are registered by concrete class, so this facade must
 * remain separate from the full DaemonManager implementation.
 */
public final class SafeDaemonManagerImpl implements SafeDaemonManager
{
    private final DaemonManager delegate;

    /**
     * Create a status facade for the internal manager.
     *
     * @param delegate The internal daemon manager
     */
    SafeDaemonManagerImpl(DaemonManager delegate)
    {
        this.delegate = delegate;
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
        return delegate.isRunning(daemonName);
    }
}
