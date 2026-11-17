/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.logging.LoggingManager;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeLoggingManagerImpl implements SafeLoggingManager
{
    private final Supplier<LoggingManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeLoggingManagerImpl(Supplier<LoggingManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * resetAllLogs through the reviewed RPC contract.
     */
    @Override
    public void resetAllLogs()
    {
        delegate.get().resetAllLogs();
    }
}
