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

import java.lang.ref.WeakReference;
import java.net.URL;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.security.MvvmLogin;

final class NullLoginDesc extends LoginDesc
{
    private final TargetDesc targetDesc;

    // constructors -----------------------------------------------------------

    NullLoginDesc(URL url, int timeout)
    {
        super(url, timeout, null);

        MvvmLogin login = ((MvvmContextImpl)MvvmContextFactory.context()).mvvmLogin();

        targetDesc = new TargetDesc(url, timeout, null, 0,
                                    new WeakReference(login));
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

