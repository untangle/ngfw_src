/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPSessionDesc.java,v 1.3 2005/01/05 20:05:24 rbscott Exp $
 */

package com.metavize.mvvm.argon;

public interface UDPSessionDesc extends IPSessionDesc {
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
}
