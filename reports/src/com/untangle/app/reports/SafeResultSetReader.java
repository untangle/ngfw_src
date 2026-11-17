/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.app.reports;

import java.util.ArrayList;

/**
 * Reviewed callable operations for SafeResultSetReader.
 */
public interface SafeResultSetReader
{
    /**
     * closeConnection through the reviewed RPC contract.
     */
    void closeConnection();

    /**
     * getNextChunk through the reviewed RPC contract.
     * @param chunkSize The chunkSize argument
     * @return The delegated result
     */
    ArrayList<Object> getNextChunk(int chunkSize);

    /**
     * isClosed through the reviewed RPC contract.
     * @return The delegated result
     */
    boolean isClosed();
}
