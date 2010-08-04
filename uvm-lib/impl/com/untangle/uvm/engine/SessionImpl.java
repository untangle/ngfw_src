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
import com.untangle.uvm.argon.ArgonSession;

import org.apache.log4j.Logger;

/**
 * Abstract base class for all live sessions
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
abstract class SessionImpl implements Session
{
    private final Logger logger = Logger.getLogger(SessionImpl.class);
    
    // For when we use a two-element array to store state for both sides.
    protected static final int CLIENT = 0;
    protected static final int SERVER = 1;

    protected MPipeImpl mPipe;

    /**
     * The pipeline session that corresponds to this (node) Session.
     */
    protected ArgonSession argonSession;

    protected volatile Object attachment = null;

    protected SessionImpl(MPipeImpl mPipe, ArgonSession argonSession)
    {
        this.mPipe = mPipe;
        this.argonSession = argonSession;
    }

    public MPipe mPipe()
    {
        return mPipe;
    }

    public int id()
    {
        return argonSession.id();
    }

    public String user()
    {
        return argonSession.user();
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


    public Object globalAttach(String key, Object ob)
    {
        logger.warn("globalAttach( " + key + " , " + ob + " )");
        return this.argonSession.sessionGlobalState().attach(key,ob);
    }

    public Object globalAttachment(String key)
    {
        return this.argonSession.sessionGlobalState().attachment(key);
    }
    
    /**
     * Number of bytes received from the client.
     */
    public long c2tBytes()
    {
        return this.argonSession.c2tBytes();
    }

    /**
     * Number of bytes transmitted to the server.
     */
    public long t2sBytes()
    {
        return this.argonSession.t2sBytes();
    }

    /**
     * Number of bytes received from the server.
     */
    public long s2tBytes()
    {
        return this.argonSession.s2tBytes();
    }

    /**
     * Number of bytes transmitted to the client.
     */
    public long t2cBytes()
    {
        return this.argonSession.t2cBytes();
    }

    /**
     * Number of chunks received from the client.
     */
    public long c2tChunks()
    {
        return this.argonSession.c2tChunks();
    }

    /**
     * Number of chunks transmitted to the server.
     */
    public long t2sChunks()
    {
        return this.argonSession.t2sChunks();
    }

    /**
     * Number of chunks received from the server.
     */
    public long s2tChunks()
    {
        return this.argonSession.s2tChunks();
    }

    /**
     * Number of chunks transmitted to the client.
     */
    public long t2cChunks()
    {
        return this.argonSession.t2cChunks();
    }

}
