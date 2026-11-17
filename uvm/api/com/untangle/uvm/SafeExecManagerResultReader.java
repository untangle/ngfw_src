/**
 * Restricted JSON-RPC contracts for this subsystem.
 */
package com.untangle.uvm;

/**
 * Reviewed callable operations for SafeExecManagerResultReader.
 */
public interface SafeExecManagerResultReader
{
    /**
     * getResult through the reviewed RPC contract.
     * @return The delegated result
     */
    Integer getResult();

    /**
     * readFromOutput through the reviewed RPC contract.
     * @return The delegated result
     */
    String readFromOutput();
}
