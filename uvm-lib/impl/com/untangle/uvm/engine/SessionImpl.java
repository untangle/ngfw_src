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

package com.untangle.uvm.engine;

import com.untangle.uvm.vnet.MPipe;
import com.untangle.uvm.vnet.Session;

/**
 * Abstract base class for all live sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class SessionImpl implements Session {

    // For when we use a two-element array to store state for both sides.
    protected static final int CLIENT = 0;
    protected static final int SERVER = 1;

    protected MPipeImpl mPipe;

    /**
     * The pipeline session that corresponds to this (node) Session.
     *
     */
    protected com.untangle.uvm.argon.Session pSession;

    protected volatile Object attachment = null;

    protected SessionImpl(MPipeImpl mPipe, com.untangle.uvm.argon.Session pSession)
    {
        this.mPipe = mPipe;
        this.pSession = pSession;
    }

    public MPipe mPipe()
    {
        return mPipe;
    }

    public int id()
    {
        return pSession.id();
    }

    public String user()
    {
        return pSession.user();
    }

    public Object attach(Object ob)
    {
        Object oldOb = attachment;
        attachment = ob;
        return oldOb;
    }

    public Object attachment()
    {
        return attachment;
    }

    /**
     * Number of bytes received from the client.
     */
    public long c2tBytes()
    {
        return this.pSession.c2tBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     */
    public long t2sBytes()
    {
        return this.pSession.t2sBytes();
    }

    /**
     * Number of bytes received from the server.
     */
    public long s2tBytes()
    {
        return this.pSession.s2tBytes();
    }

    /**
     * Number of bytes transmitted to the client.
     */
    public long t2cBytes()
    {
        return this.pSession.t2cBytes();
    }

    /**
     * Number of chunks received from the client.
     */
    public long c2tChunks()
    {
        return this.pSession.c2tChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     */
    public long t2sChunks()
    {
        return this.pSession.t2sChunks();
    }

    /**
     * Number of chunks received from the server.
     */
    public long s2tChunks()
    {
        return this.pSession.s2tChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     */
    public long t2cChunks()
    {
        return this.pSession.t2cChunks();
    }

}
