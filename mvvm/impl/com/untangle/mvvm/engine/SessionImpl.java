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

package com.untangle.mvvm.engine;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tapi.*;
// import org.apache.commons.jxpath.JXPathContext;

abstract class SessionImpl implements Session {

    // For when we use a two-element array to store state for both sides.
    protected static final int CLIENT = 0;
    protected static final int SERVER = 1;

    protected MPipeImpl mPipe;

    /**
     * The pipeline session that corresponds to this (transform) Session.
     *
     */
    protected com.untangle.mvvm.argon.Session pSession;

    protected volatile Object attachment = null;

    protected SessionImpl(MPipeImpl mPipe, com.untangle.mvvm.argon.Session pSession)
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

    /*
    public ExtendedPreferences sessionNode()
    {
        return MvvmContextFactory.mvvmContext().preferencesManager()
            .sessionPreferences(id());
    }
    */

    /*
    public String sessionPath()
    {
        return MvvmContextFactory.mvvmContext().preferencesManager()
            .sessionPreferencesPath(id());
    }
    */

    /*
    public JXPathContext sessionContext()
    {
        return JXPathContext.newContext(MvvmContextFactory.mvvmContext()
                                        .preferencesManager()
                                        .sessionPreferences(id()));
    }
    */

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
