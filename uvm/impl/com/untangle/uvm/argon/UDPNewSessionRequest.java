/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.argon;

import com.untangle.jnetcap.*;

public interface UDPNewSessionRequest extends IPNewSessionRequest, UDPSessionDesc
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
     * Retrieve the ICMP associated with the session
     */
    int icmpId();

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

    /**
     * Returns true if this is an Ping session
     */
    boolean isPing();

    /**
     * Set the ICMP id for the session.
     * @param value - The value to set the icmp id to, set to -1 to not modify
     */
    void icmpId( int value );
}
