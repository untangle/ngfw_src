/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
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
    protected com.metavize.mvvm.argon.Session pSession;

    protected volatile Object attachment = null;

    protected SessionImpl(MPipeImpl mPipe, com.metavize.mvvm.argon.Session pSession)
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
}
