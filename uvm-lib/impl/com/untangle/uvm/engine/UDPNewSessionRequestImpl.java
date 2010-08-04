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

import com.untangle.uvm.vnet.UDPNewSessionRequest;
import com.untangle.uvm.argon.ArgonUDPNewSessionRequest;

/**
 * Implementation class for UDP new session requests
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
class UDPNewSessionRequestImpl extends IPNewSessionRequestImpl implements UDPNewSessionRequest
{

    protected UDPNewSessionRequestImpl(Dispatcher disp, ArgonUDPNewSessionRequest argonRequest)
    {
        super(disp, argonRequest);
    }

    /**
     * Returns true if this is a Ping session
     */
    public boolean isPing()
    {
        return ((ArgonUDPNewSessionRequest)argonRequest).isPing();
    }

    /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return ((ArgonUDPNewSessionRequest)argonRequest).icmpId();
    }

    /**
     * Set the ICMP id for this session.</p>
     * @param value - new icmp id value, -1 to not modify.
     */
    public void icmpId(int value)
    {
        ((ArgonUDPNewSessionRequest)argonRequest).icmpId(value);
    }

}
