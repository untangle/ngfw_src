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

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.metavize.jvector.IncomingSocketQueue;
import com.metavize.jvector.OutgoingSocketQueue;
import com.metavize.jvector.Source;
import com.metavize.jvector.Sink;
import com.metavize.jvector.UDPSink;

public class UDPSessionImpl extends IPSessionImpl implements UDPSession
{
    protected final byte ttl;
    protected final byte tos;
    protected final byte options[];
    protected final int  icmpId;

    private static final Logger logger = Logger.getLogger( UDPSessionImpl.class );

    public UDPSessionImpl( UDPNewSessionRequest request )
    {
        super( request );
        
        ttl     = request.ttl();
        tos     = request.tos();
        options = request.options();
        icmpId  = request.icmpId();
    }
    
    /**
     * Retrieve the TTL for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TTL value inside of them)
     */
    public byte ttl() 
    { 
        return ttl;
    }

    /**
     * Retrieve the TOS for a session, this only has an impact for the last session in the chain
     * when passing data crumbs (UDPPacketCrumbs have TOS value inside of them).
     */
    public byte tos()
    {
        return tos;
    }

    /**
     * Retrieve the options associated with the first UDP packet in the session.
     */
    public byte[] options()
    {
        return options;
    }

    /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return icmpId;
    }
}
