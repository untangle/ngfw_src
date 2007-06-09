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

import com.untangle.mvvm.IntfEnum;

import com.untangle.mvvm.tran.RemoteIntfManager;
import com.untangle.mvvm.localapi.LocalIntfManager;

/** Passthru class for access to the api function inside of the interface manager */
public class RemoteIntfManagerImpl implements  RemoteIntfManager
{
    private final LocalIntfManager localIntfManager;

    RemoteIntfManagerImpl( LocalIntfManager lim )
    {
        this.localIntfManager = lim;
    }

    /* Retrieve the current interface enumeration */
    public IntfEnum getIntfEnum()
    {
        return this.localIntfManager.getIntfEnum();
    }
}