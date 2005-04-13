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
    
    private native void status( long ip, int port );

    /* Singleton enforcement */    
    public static Shield getInstance()
    {
        return INSTANCE;
    }
}
