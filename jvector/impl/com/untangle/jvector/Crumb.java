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

package com.untangle.jvector;

public abstract class Crumb
{
    protected static final int DATA_MASK     = 0x1100;
    protected static final int SHUTDOWN_MASK = 0x1200;

    public static final int TYPE_DATA        = DATA_MASK | 1;      // Data crumb, passed as is
    public static final int TYPE_UDP_PACKET  = DATA_MASK | 2;      // UDP Packet, this extends a PacketCrumb
    public static final int TYPE_ICMP_PACKET = DATA_MASK | 3;      // ICMP packet, this extends a PacketCrumb

    public static final int TYPE_SHUTDOWN    = SHUTDOWN_MASK | 1;  // Shutdown
    public static final int TYPE_RESET       = SHUTDOWN_MASK | 2;  // Reset

    public abstract void raze();
    public abstract int  type();

    public boolean isData()
    {
        return ( type() & DATA_MASK ) == DATA_MASK;
    }

    public boolean isShutdown()
    {
        return ( type() & SHUTDOWN_MASK ) == SHUTDOWN_MASK;
    }
}
