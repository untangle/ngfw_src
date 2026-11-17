/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeNotificationManagerImpl implements SafeNotificationManager
{
    private final Supplier<NotificationManager> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeNotificationManagerImpl(Supplier<NotificationManager> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getNotifications through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public List<String> getNotifications()
    {
        return delegate.get().getNotifications();
    }
}
