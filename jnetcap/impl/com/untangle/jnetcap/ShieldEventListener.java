/*
 * $HeadURL:$
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

package com.untangle.jnetcap;

import java.net.InetAddress;

public interface ShieldEventListener
{
    /** 
     * ip:         Ip that was limited, rejected or dropped
     * clientIntf: Interface where the events where generated
     * reputation: Current reputation of the IP.
     * mode:       Mode was in at the time of the rejection.
     * limited:    Number of limited responses since the last event.
     * rejected:   Number of rejected responses since the last event.
     * dropped:    Number of dropped sessions since the last event.
     */
    void rejectionEvent( InetAddress ip, byte clientIntf, double reputation, int mode, int limited,
                         int dropped, int rejected );
                         

    /**
     * Event triggered to log statistics
     * accepted:  Number of accepted sessions since the last event.
     * limited:   Number of limited sessions since the last event.
     * dropped:   Number of dropped sessions since the last event.
     * rejected:  Number of rejected sessions since the last event.
     * relaxed:   Number of ticks spent in relaxed mode.
     * lax:       Number of ticks spent in lax mode.
     * tight:     Number of ticks spent in tight mode.
     * closed:    Number of ticks spent in closed mode.
     */
    void statisticEvent( int accepted, int limited, int dropped, int rejected, int relaxed,
                         int lax, int tight, int closed );
}
