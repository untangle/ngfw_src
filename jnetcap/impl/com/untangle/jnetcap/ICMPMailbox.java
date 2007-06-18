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

package com.untangle.jnetcap;

/**
 * This is just a holder for the CPointer so that other packages can 
 * have a holder to the mailbox without actually being able to access
 * the value of the pointer.
 * Nothing is read from this mailbox, to access ICMP packets, use the
 * PacketMailbox which can contain both ICMP and UDP packets.
 */
public class ICMPMailbox
{
    private final CPointer pointer;
    
    /**
     * This class can only be created inside of jnetcap
     */
    ICMPMailbox( CPointer pointer ) 
    {
        this.pointer = pointer;
    }

    CPointer pointer()
    {
        return this.pointer;
    }

}
