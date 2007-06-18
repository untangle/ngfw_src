/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.jnetcap;

import java.net.InetAddress;

import java.util.List;

public final class Shield 
{
    private static final Shield INSTANCE = new Shield();

    private static final ShieldEventListener NULL_EVENT_LISTENER = new ShieldEventListener()
        {
            public void rejectionEvent( InetAddress ip, byte clientIntf, double reputation, int mode, 
                                        int limited, int dropped, int rejected )
            {
                /* Null event listener nothing to do */
            }

            public void statisticEvent( int accepted, int limited, int dropped, int rejected, int relaxed,
                                        int lax, int tight, int closed )
            {
                /* Null event listener, nothing to do */
            }
        };

    private ShieldEventListener listener = NULL_EVENT_LISTENER;

    private Shield()
    {
    }

    /**
     * Adds a chunk for a particular IP.  This should only be used for TCP since the UDP chunks
     * are added automatically.
     * @param ip - a <code>InetAddress</code> containing the ip for the user.
     * @param protocol - Either Netcap.IPPROTO_TCP or Netcap.IPPROTO_UDP
     * @param size - Size of the chunk in bytes.
     */
    public void addChunk( InetAddress address, short protocol, int size )
    {
        addChunk( Inet4AddressConverter.toLong( address ), protocol, size );
    }
        
    /**
     * Dump out the state of the shield in XML and return it in String
     */
    public void status( InetAddress ip, int port )
    {
        status( Inet4AddressConverter.toLong( ip ), port );
    }

    public void registerEventListener( ShieldEventListener listener )
    {
        this.listener = listener;

        /* This just tells the netcap to call into java */
        registerEventListener();
    }

    public void unregisterEventListener()
    {
        this.listener = NULL_EVENT_LISTENER;
    }

    public void setNodeSettings( List<ShieldNodeSettings> settings ) throws JNetcapException
    {
        /* Create some new arrays to pass in the settings */
        double divider[] = new double[settings.size()];
        long address[]   = new long[settings.size()];
        long netmask[]   = new long[settings.size()];

        int c = 0;
        for ( ShieldNodeSettings setting : settings ) {
            divider[c] = setting.divider();
            address[c] = setting.addressLong();
            netmask[c++] = setting.netmaskLong();
        }
        
        try {
            setNodeSettings( divider, address, netmask );
        } catch( Exception e ) {
            throw new JNetcapException( "Unable to set node settings", e );
        }
    }

    /* This function is called from C to get into java */
    private void callRejectionEventListener( long ip, byte clientIntf, double reputation, int mode, 
                                             int limited, int dropped, int rejected )
    {
        this.listener.rejectionEvent( Inet4AddressConverter.toAddress( ip ), clientIntf, reputation, mode, 
                                      limited, dropped, rejected );
    }

    /* This function is called from C to get into java */
    private void callStatisticEventListener( int accepted, int limited, int dropped, int rejected, 
                                             int relaxed, int lax, int tight, int closed )
    {
        this.listener.statisticEvent( accepted, limited, dropped, rejected, relaxed, lax, tight, closed );
    }

    /****************** Native methods ****************/

    /**
     * Load a shield configuration from a file, This will throw an error if there
     * is an error loading the configuration.
     *
     * @param fileName a <code>String</code> value that defines the path to the file to load.
     */    
    public native void config( String fileName );

    /**
     * Adds a chunk for a particular IP.  This should only be used for TCP since the UDP chunks
     * are added automatically.
     * @param ip - a <code>long</code> containing the ip for the user.
     * @param protocol - Either Netcap.IPPROTO_TCP or Netcap.IPPROTO_UDP
     * @param size - Size of the chunk in bytes.
     */
    public native void addChunk( long address, short protocol, int size );

    private native void status( long ip, int port );
    private native void registerEventListener();
    private native void removeEventListener();

    private native void setNodeSettings( double divider[], long address[], long netmask[] );

    /* Singleton enforcement */    
    public static Shield getInstance()
    {
        return INSTANCE;
    }
}
