/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPNewSessionRequest.java,v 1.5 2005/01/06 01:01:29 jdi Exp $
 */

package com.metavize.mvvm.argon;

import com.metavize.jnetcap.*;

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
