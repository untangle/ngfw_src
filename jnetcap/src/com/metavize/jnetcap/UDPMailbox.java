/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPMailbox.java,v 1.5 2004/12/31 05:18:27 rbscott Exp $
 */

package com.metavize.jnetcap;

import java.util.EmptyStackException;

public interface UDPMailbox {
    /**
     * Wait forever reading a packet from the UDP mailbox.  Use with caution.
     */
    public UDPPacket read();
    
    /**
     * Timed wait to read a packet from the UDP mailbox.</p>
     * @param timeout - Timeout in milliseconds
     */
    public UDPPacket read( int timeout );

    /* Retrieve the value of the C pointer */
    public int pointer();
}
