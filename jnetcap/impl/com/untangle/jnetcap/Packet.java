/*
 * $HeadURL: svn://chef/work/src/jnetcap/impl/com/untangle/jnetcap/Packet.java $
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

public interface Packet 
{
    /**
     * Retrieve the destination info about the packet.
     */
    public IPTraffic traffic();
    
    /**
     * Retrieve the data from the packet, this will allocate a buffer of the proper size automatically. 
     */
    public byte[] data();

    /**
     * Retrieve the data from a packet and place the results into the preallocated <code>buffer</code>.
     * @param buffer - Location where the data should be stored.
     *
     * @return size of the data returned.
     */
    public int getData( byte[] buffer );

    /**
     * Send out this packet 
     */
    public void send();

    /**
     * Raze this packet 
     */
    public void raze();
}
