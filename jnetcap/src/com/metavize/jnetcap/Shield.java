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

import java.net.InetAddress;

public final class Shield 
{
    private static final Shield INSTANCE = new Shield();

    private static final ShieldEventListener NULL_EVENT_LISTENER = new ShieldEventListener()
        {
            public void event( InetAddress ip, double reputation, int mode, 
                               int limited, int rejected, int dropped )
            {
                /* Null event listener nothing to do */
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

    /* This function is called from C to get into java */
    private void callEventListener( long ip, double reputation, int mode, 
                                    int limited, int rejected, int dropped )
    {
        this.listener.event( Inet4AddressConverter.toAddress( ip ), reputation, mode, 
                             limited, rejected, dropped );
    }
    
    private native void status( long ip, int port );
    private native void registerEventListener();
    private native void removeEventListener();                                      


    /* Singleton enforcement */    
    public static Shield getInstance()
    {
        return INSTANCE;
    }
}
