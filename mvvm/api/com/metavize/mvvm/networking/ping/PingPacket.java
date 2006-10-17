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

/** Represents the response of a single ping packet */
public class PingPacket implements Serializable
{
    private final int sequence;
    private final int ttl;
    private final long micros;
    private final int size;

    public PingPacket( int sequence, int ttl, long micros, int size )
    {
        this.sequence = sequence;
        this.ttl = ttl;
        this.micros = micros;
        this.size = size;
    }

    public int getSequence()
    {
        return this.sequence;
    }

    public int getTtl()
    {
        return this.ttl;
    }

    public float getMicros()
    {
        return this.micros;
    }

    public int getSize()
    {
        return this.size;
    }

    public String toString()
    {
        return "sequence: " + this.sequence + ", ttl: " + this.ttl + ", delay: " + this.micros + " us, size: " + this.size + " bytes";
    }
    


}
