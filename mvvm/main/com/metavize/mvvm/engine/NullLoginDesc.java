/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.lang.ref.WeakReference;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.security.MvvmLogin;

final class NullLoginDesc extends LoginDesc
{
    static final NullLoginDesc LOCAL_LOGIN = new NullLoginDesc(true);
    static final NullLoginDesc REMOTE_LOGIN = new NullLoginDesc(false);

    private final boolean local;
    private final TargetDesc targetDesc;

    // constructors -----------------------------------------------------------

    private NullLoginDesc(boolean local)
    {
        super(null);

        this.local = local;
        MvvmLogin login = MvvmContextFactory.context().mvvmLogin(local);

        targetDesc = new TargetDesc(null, 0, new WeakReference(login));
    }

    // static factories -------------------------------------------------------

    static NullLoginDesc getLoginDesc(boolean local)
    {
        return local ? LOCAL_LOGIN : REMOTE_LOGIN;
    }

    // package protected methods ----------------------------------------------

    TargetDesc getTargetDesc()
    {
        return targetDesc;
    }

    // LoginDesc methods ------------------------------------------------------

    @Override
    TargetDesc getTargetDesc(Object target, TargetReaper targetReaper)
    {
        System.out.println("NULLOGINDESC FOR: " + target);
        return targetDesc;
    }

    @Override
    TargetDesc getTargetDesc(int targetId)
    {
        return targetDesc;
    }
}

