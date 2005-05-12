/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.jnetcap;

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
