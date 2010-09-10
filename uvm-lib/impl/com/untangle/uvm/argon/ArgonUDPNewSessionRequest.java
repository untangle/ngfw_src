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

package com.untangle.uvm.argon;


public interface ArgonUDPNewSessionRequest extends ArgonIPNewSessionRequest, ArgonUDPSessionDesc
{
    /**
     * Retrieve the TTL for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    byte ttl();

    /**
     * Retrieve the TOS for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    byte tos();

    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    byte[] options();

    /**
     * Set the TTL for a session.</p>
     * @param value - new TTL value.
     */
    void ttl( byte value );
    
    /**
     * Set the TOS for a session.</p>
     * @param value - new TOS value.
     */
    void tos( byte value );

    /**
     * Set the options for this session.</p>
     * @param value - The new options.
     */
    void options( byte[] value );

}
