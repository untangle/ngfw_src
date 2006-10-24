/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.jnetcap;

import java.util.EmptyStackException;

public interface PacketMailbox
{
    /**
     * Wait forever reading a packet from the packet mailbox.  Use with caution.
     */
    public Packet read();
    
    /**
     * Timed wait to read a packet from the packet mailbox.</p>
     * @param timeout - Timeout in milliseconds
     */
    public Packet read( int timeout );

    /* Retrieve the value of the C pointer */
    public int pointer();
}
