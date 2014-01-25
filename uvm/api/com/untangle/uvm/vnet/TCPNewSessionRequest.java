/**
 * $Id$
 */
package com.untangle.uvm.vnet;

/**
 * The TCP specific version of new session request
 */
public interface TCPNewSessionRequest extends IPNewSessionRequest
{
    /**
     * <code>rejectReturnRst</code> rejects the new connection and sends a RST to the client.
     * Note that if <code>acked</code> is true, then a simple close is done instead.
     */
    void rejectReturnRst();
}
