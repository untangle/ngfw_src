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

package com.untangle.uvm.engine;

import java.lang.ref.WeakReference;
import java.net.URL;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.security.UvmLogin;

final class NullLoginDesc extends LoginDesc
{
    private final TargetDesc targetDesc;

    // constructors -----------------------------------------------------------

    NullLoginDesc(URL url, int timeout)
    {
        super(url, timeout, null);

        UvmLogin login = ((UvmContextImpl)UvmContextFactory.context()).uvmLogin();

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

