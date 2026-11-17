/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

import java.util.Objects;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeExecManagerResultReaderImpl implements SafeExecManagerResultReader
{
    private final ExecManagerResultReader delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeExecManagerResultReaderImpl(ExecManagerResultReader delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * getResult through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public Integer getResult()
    {
        return delegate.getResult();
    }

    /**
     * readFromOutput through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public String readFromOutput()
    {
        return delegate.readFromOutput();
    }
}
