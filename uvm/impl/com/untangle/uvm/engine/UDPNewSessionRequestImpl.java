/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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
