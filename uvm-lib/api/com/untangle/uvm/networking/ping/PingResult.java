/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking.ping;

import java.io.Serializable;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PingResult implements Serializable
{
    private static final InetAddress EMPTY_ADDRESS;
    private static final List<PingPacket> EMPTY_LIST = Collections.emptyList();

    /* false if unable to resolve the host */
    private final boolean resolvedHost;

    /* The string that was attempted to be pingged */
    private final String addressString;
    
    /* Address that was pinged */
    private final InetAddress address;

    /* List of ping packets and the status from each one */
    private final List<PingPacket> pingPacketList;

    /* Total number of ping packets transmitted. */
    private final int totalTransmitted;
    
    private final long totalTimeMicros;
    
    public PingResult( String addressString, InetAddress address, List<PingPacket> pingPacketList, int totalTransmitted, long totalTimeMicros )
    {
        this( true, addressString, address, pingPacketList, totalTransmitted, totalTimeMicros );
    }

    public PingResult( String addressString )
    {
        this( false, addressString, EMPTY_ADDRESS, EMPTY_LIST, 0, 0 );
    }

    private PingResult( boolean resolvedHost, String addressString, InetAddress address, 
                        List<PingPacket> pingPacketList, int totalTransmitted, long totalTimeMicros )
    {
        this.resolvedHost = resolvedHost;
        this.addressString = addressString;
        this.address = address;
        this.pingPacketList = pingPacketList;
        this.totalTransmitted = totalTransmitted;
        this.totalTimeMicros = totalTimeMicros;
    }

    /* true if the addressString was able to be resolved */
    public boolean getResolvedHost()
    {
        return this.resolvedHost;
    }

    /* This is not a addressString object because it may be an ip address string */
    public String getAddressString()
    {
        return this.addressString;
    }
    
    /* get the IP address that was pinged */
    public InetAddress getAddress()
    {
        return this.address;
    }

    /* get the list of ping packets that were responsed to */
    public List<PingPacket> getPingPacketList()
    {
        return this.pingPacketList;
    }

    /* retrieve the total number of ping packets transmitted. */
    public int getTotalTransmitted()
    {
        return this.totalTransmitted;
    }

    /* retrieve the total duration of the test in micro-seconds. */
    public long getTotalTimeMicros()
    {
        return this.totalTimeMicros;
    }

    /* retrieve the percent of the transmitted packets that were answered */
    public int getPercentAnswered()
    {
        if ( this.totalTransmitted == 0 ) return 0;

        return (int)((((float)pingPacketList.size()) / this.totalTransmitted ) * 100);
    }

    /* Get the average round trip time for a packet */
    public long getAverageRoundTripMicros()
    {
        long average = 0;
        for ( PingPacket p : this.pingPacketList ) average += p.getMicros();

        return average / this.pingPacketList.size();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        /* print the ping results if able to resolve the host */
        if ( this.resolvedHost ) {
            sb.append( "PING " + this.addressString + " (" + this.address.getHostAddress() + ")\n"  );
            sb.append( "total tx: " + this.totalTransmitted + "," );
            sb.append( " total time: " + (float)this.totalTimeMicros / 1000 + " ms," );
            sb.append( " percent answered: " + getPercentAnswered() + "%\n" );
            for ( PingPacket pp : this.pingPacketList ) sb.append( "  " + pp + "\n" );
        } else {
            sb.append( "Unable to resolve the host: '" + this.addressString + "'" );
        }

        return sb.toString();
    }

    static
    {
        InetAddress address = null;

        try {
            address = InetAddress.getByName( "0.0.0.0" );
        } catch ( UnknownHostException e ) {
            System.err.println( "error parsing empty string" );
            address = null;
        }

        EMPTY_ADDRESS = address;
    }
}
