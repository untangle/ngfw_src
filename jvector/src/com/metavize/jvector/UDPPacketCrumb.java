/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UDPPacketCrumb.java,v 1.6 2005/01/10 23:21:32 rbscott Exp $
 */

package com.metavize.jvector;

import com.metavize.jnetcap.*;

public class UDPPacketCrumb extends DataCrumb
{
    private final UDPPacketDesc desc;

    /**
     * Create a new UDP Packet Crumb.</p>
     *
     * @param desc   - Structure describing the non-endpoint UDP related data.
     * @param data   - Byte array containing the data.
     * @param offset - Offset in the byte array.
     * @param limit  - Limit of the data.
     */
    public UDPPacketCrumb( UDPPacketDesc desc, byte[] data, int offset, int limit )
    {
        super( data, offset, limit );
        this.desc = desc;        
    }

    protected UDPPacketCrumb( UDPPacketDesc desc, byte[] data, int limit )
    {
        this( desc, data, 0, limit );
    }

    protected UDPPacketCrumb( UDPPacketDesc desc, byte[] data )
    {
        this( desc, data, data.length );
    }

    public int type()
    {
        return TYPE_UDP_PACKET;
    }

    public UDPPacketDesc desc()
    {
        return desc;
    }

    public void raze()
    {
        /* XXX What should go in here, C structure is freed automatically */
    }
}
