/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.reports;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Concrete restricted facade; the full delegate is never exported.
 */
public final class SafeResultSetReaderImpl implements SafeResultSetReader
{
    private final ResultSetReader delegate;

    /**
     * Create the restricted facade.
     * @param delegate The internal delegate
     */
    SafeResultSetReaderImpl(ResultSetReader delegate)
    {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * closeConnection through the reviewed RPC contract.
     */
    @Override
    public void closeConnection()
    {
        delegate.closeConnection();
    }

    /**
     * getNextChunk through the reviewed RPC contract.
     * @param chunkSize The chunkSize argument
     * @return The delegated result
     */
    @Override
    public ArrayList<Object> getNextChunk(int chunkSize)
    {
        return delegate.getNextChunk(chunkSize);
    }

    /**
     * isClosed through the reviewed RPC contract.
     * @return The delegated result
     */
    @Override
    public boolean isClosed()
    {
        return delegate.isClosed();
    }
}
