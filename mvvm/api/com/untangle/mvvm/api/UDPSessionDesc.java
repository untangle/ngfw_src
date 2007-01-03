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

package com.untangle.mvvm.api;

public interface UDPSessionDesc extends IPSessionDesc
{
    /**
     * Retrieve the TTL for a session, this only has an "impact" for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    byte ttl();

    /**
     * Retrieve the TOS for a session, this only has an "impact" for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    byte tos();
    
    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    byte[] options();
    
    /**
     * Returns true if this is a Ping session
     */
    boolean isPing();
    
    /**
     * Retrieve the ICMP associated with the session
     */
    int icmpId();
}
