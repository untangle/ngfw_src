/*
 * Copyright (c) 2003 - 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.tapi.impl;

import java.net.InetAddress;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipe;

class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest {

    protected UDPNewSessionRequestImpl(Dispatcher disp, com.metavize.mvvm.argon.UDPNewSessionRequest pRequest) {
        super(disp, pRequest);
    }

        /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return ((com.metavize.mvvm.argon.UDPNewSessionRequest)pRequest).icmpId();
    }

    /**
     * Set the ICMP id for this session.</p>
     * @param value - new icmp id value, -1 to not modify.
     */
    public void icmpId(int value)
    {
        ((com.metavize.mvvm.argon.UDPNewSessionRequest)pRequest).icmpId(value);
    }

}
