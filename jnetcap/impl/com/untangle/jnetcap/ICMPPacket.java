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

package com.untangle.jnetcap;

import java.net.InetAddress;

public interface ICMPPacket extends Packet
{
    /**
     * ICMP Type of the packet
     */
    byte icmpType();

    /** 
     * ICMP code for the packet
     */
    byte icmpCode();

    /**
     * If Relevant, return the source of the ICMP packet, otherwise return null
     */
    InetAddress icmpSource( byte data[], int limit );
}
