/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking.ping;

import java.io.Serializable;

import java.net.InetAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingResult implements Serializable
{
    /* Address that was pinged */
    private final InetAddress address;

    /* List of ping packets and the status from each one */
    private final List<PingPacket> pingPacketList;

    /* Total number of ping packets transmitted. */
    private final int totalTransmitted;
    
    private final long totalTimeMicros;
    
    public PingResult( InetAddress address, List<PingPacket> pingPacketList, int totalTransmitted, long totalTimeMicros )
    {
        this.address = address;
        this.pingPacketList = Collections.unmodifiableList( new ArrayList( pingPacketList ));
        this.totalTransmitted = totalTransmitted;
        this.totalTimeMicros = totalTimeMicros;
    }

    public InetAddress getAddress()
    {
        return this.address;
    }

    public List<PingPacket> getPingPacketList()
    {
        return this.pingPacketList;
    }

    public int getTotalTransmitted()
    {
        return this.totalTransmitted;
    }

    public long getTotalTimeMicros()
    {
        return this.totalTimeMicros;
    }

    public int getPercentAnswered()
    {
        return (int)((((float)pingPacketList.size()) / pingPacketList.size()) * 100);
    }

    public long getAverageRoundTripMicros()
    {
        long average = 0;
        for ( PingPacket p : this.pingPacketList ) average += p.getMicros();

        return average / this.pingPacketList.size();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "total tx: " + this.totalTransmitted + " total time: " + this.totalTimeMicros + "\n" );
        for ( PingPacket pp : this.pingPacketList ) sb.append( "  " + pp + "\n" );
        return sb.toString();
    }
}
