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

package com.untangle.mvvm.argon;






import org.apache.log4j.Logger;
public class UDPSessionImpl extends IPSessionImpl implements UDPSession
{
    protected final byte ttl;
    protected final byte tos;
    protected final byte options[];
    protected final int  icmpId;
    protected final boolean isPing;

    private final Logger logger = Logger.getLogger(getClass());

    public UDPSessionImpl( UDPNewSessionRequest request )
    {
        super( request );

        ttl     = request.ttl();
        tos     = request.tos();
        options = request.options();
        icmpId  = request.icmpId();
        isPing  = request.isPing();
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
     * Returns true if this is a Ping session
     */
    public boolean isPing()
    {
        return this.isPing;
    }

    /**
     * Retrieve the ICMP associated with the session
     */
    public int icmpId()
    {
        return icmpId;
    }
}
