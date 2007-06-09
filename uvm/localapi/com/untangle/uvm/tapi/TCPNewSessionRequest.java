/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.tapi;

public interface TCPNewSessionRequest extends IPNewSessionRequest {
    /**
     * <code>acked</code> returns true if the new session has already been ACKed to the client.
     * This occurs when the SYN shield has been activated.</p>
     *
     * If false, the SYN has not yet been ACKed.  In this case, the option to
     * <code>rejectReturnRst</code> is still available and if used will look to the client
     * as if no server was listening on that port.</p>
     *
     * @return True if the session was acked, false otherwise.
     */
    boolean acked();
    
    /**
     * <code>rejectReturnRst</code> rejects the new connection and sends a RST to the client.
     * Note that if <code>acked</code> is true, then a simple close is done instead.
     */
    void rejectReturnRst();

    /**
     * <code>rejectReturnRst</code> rejects the new connection and sends a RST to the client.
     * Note that if <code>acked</code> is true, then a simple close is done instead.
     *
     * @param needsFinalization Set to true if this wants finalization.
     */
    void rejectReturnRst(boolean needsFinalization);
}
