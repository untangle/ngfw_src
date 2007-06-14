/*
 * $HeadURL:$
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

