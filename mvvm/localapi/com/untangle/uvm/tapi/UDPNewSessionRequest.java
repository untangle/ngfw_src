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

package com.untangle.mvvm.tapi;

public interface UDPNewSessionRequest extends IPNewSessionRequest {
    /**
     * Retrieve the ICMP associated with the session
     */
    int icmpId();

    /**
     * Set the ICMP id for the session.
     * @param value - The value to set the icmp id to, set to -1 to not modify
     */
    void icmpId( int value );
}
