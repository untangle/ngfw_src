/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;
import java.util.function.Supplier;

import com.untangle.uvm.util.SafeCheckParam;
import com.untangle.uvm.util.SafeType;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeMailSenderImpl implements SafeMailSender
{
    private final Supplier<MailSender> delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeMailSenderImpl(Supplier<MailSender> delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getSettings through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public MailSettings getSettings()
    {
        return delegate.get().getSettings();
    }

    /**
     * sendTestMessage through the reviewed RPC contract.
     * @param recipient The recipient argument
     * @return The delegated result
     */
    @Override
    public String sendTestMessage(@SafeCheckParam(SafeType.EMAIL) String recipient)
    {
        return delegate.get().sendTestMessage(recipient);
    }

    /**
     * setSettings through the reviewed RPC contract.
     * @param settings The settings argument
     */
    @Override
    public void setSettings(MailSettings settings)
    {
        delegate.get().setSettings(settings);
    }
}
