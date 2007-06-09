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

package com.untangle.uvm.tapi.client;

import java.io.Serializable;

import com.untangle.uvm.tapi.SessionDesc;
import com.untangle.uvm.tapi.SessionStats;

abstract class SessionDescImpl implements SessionDesc, Serializable {
    private static final long serialVersionUID = 2962776047684793850L;

    protected int id;

    protected String user;

    protected SessionStats stats;

    protected SessionDescImpl(int id, SessionStats stats)
    {
        this.id = id;
        this.stats = stats;
    }

    public int id()
    {
        return id;
    }

    public String user()
    {
        return user;
    }

    public SessionStats stats()
    {
        return stats;
    }

    /**
     * Number of bytes received from the client.
     */
    public long c2tBytes()
    {
        return this.stats.c2tBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     */
    public long t2sBytes()
    {
        return this.stats.t2sBytes();
    }

    /**
     * Number of bytes received from the server.
     */
    public long s2tBytes()
    {
        return this.stats.s2tBytes();
    }
    
    /**
     * Number of bytes transmitted to the client.
     */
    public long t2cBytes()
    {
        return this.stats.t2cBytes();
    }

    /**
     * Number of chunks received from the client.
     */
    public long c2tChunks()
    {
        return this.stats.c2tChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     */
    public long t2sChunks()
    {
        return this.stats.t2sChunks();
    }

    /**
     * Number of chunks received from the server.
     */
    public long s2tChunks()
    {
        return this.stats.s2tChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     */
    public long t2cChunks()
    {
        return this.stats.t2cChunks();
    }

}
