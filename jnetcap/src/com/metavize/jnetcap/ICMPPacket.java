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
}
