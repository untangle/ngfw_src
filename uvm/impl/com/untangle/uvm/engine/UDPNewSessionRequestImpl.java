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

package com.untangle.uvm.engine;

import com.untangle.uvm.tapi.UDPNewSessionRequest;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest {

    protected UDPNewSessionRequestImpl(Dispatcher disp,
                                       com.untangle.uvm.argon.UDPNewSessionRequest pRequest,
                                       boolean isInbound) {
        super(disp, pRequest, isInbound);
    }

    /**
     * Returns true if this is a Ping session
     */
    public boolean isPing()
    {
        return ((com.untangle.uvm.argon.UDPNewSessionRequest)pRequest).isPing();
    }
    
    /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return ((com.untangle.uvm.argon.UDPNewSessionRequest)pRequest).icmpId();
    }

    /**
     * Set the ICMP id for this session.</p>
     * @param value - new icmp id value, -1 to not modify.
     */
    public void icmpId(int value)
    {
        ((com.untangle.uvm.argon.UDPNewSessionRequest)pRequest).icmpId(value);
    }

}
