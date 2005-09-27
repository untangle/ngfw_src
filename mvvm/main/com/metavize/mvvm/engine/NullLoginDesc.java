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
    static final NullLoginDesc NULL_LOGIN = new NullLoginDesc();

    private final TargetDesc targetDesc;

    // constructors -----------------------------------------------------------

    private NullLoginDesc()
    {
        super(null);

        MvvmLogin login = MvvmContextFactory.context().mvvmLogin();

        targetDesc = new TargetDesc(null, 0, new WeakReference(login));
    }

    // static factories -------------------------------------------------------

    static NullLoginDesc getLoginDesc()
    {
        return NULL_LOGIN;
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
        return targetDesc;
    }

    @Override
    TargetDesc getTargetDesc(int targetId)
    {
        return targetDesc;
    }
}

