/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: UDPPacket.java,v 1.5 2004/12/30 23:05:03 rbscott Exp $
 */


package com.metavize.jnetcap;

public interface UDPPacket  {
    /**
     * Retrieve the destination info about the packet.
     */
    public IPTraffic traffic();
    
    /**
     * Retrieve the data from the packet, this will allocate a buffer of the proper size automatically. 
     */
    public byte[] data();

    /**
     * Retrieve the data from a packet and place the results into a preallocated buffer.</p>
     *
     * @return size of the data returned.
     */
    public int getData( byte[] buffer );

    /**
     * Send out this packet 
     */
    public void send();

    /**
     * Raze this packet 
     */
    public void raze();
}
