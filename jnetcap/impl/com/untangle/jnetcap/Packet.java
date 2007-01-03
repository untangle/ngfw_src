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

public interface Packet 
{
    /**
     * Retrieve the destination info about the packet.
     */
    public IPTraffic traffic();
    
    /**
     * Retrieve the data from the packet, this will allocate a buffer of the proper size automatically. 
     */
    public byte[] data();

    /**
     * Retrieve the data from a packet and place the results into the preallocated <code>buffer</code>.
     * @param buffer - Location where the data should be stored.
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
