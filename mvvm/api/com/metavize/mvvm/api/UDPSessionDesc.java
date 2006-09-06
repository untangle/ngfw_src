/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPSessionDesc.java 6750 2006-08-09 21:49:12Z rbscott $
 */

package com.metavize.mvvm.api;

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
