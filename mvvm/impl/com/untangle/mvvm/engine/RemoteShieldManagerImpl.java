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

import com.untangle.mvvm.shield.ShieldNodeSettings;

import java.net.InetAddress;

import java.util.List;

import com.untangle.mvvm.api.RemoteShieldManager;
import com.untangle.mvvm.localapi.LocalShieldManager;

/* Wrapper class for a local shield manager */
class RemoteShieldManagerImpl implements RemoteShieldManager
{
    private final LocalShieldManager lsm;

    RemoteShieldManagerImpl( LocalShieldManager lsm )
    {
        this.lsm = lsm;
    }

    public void shieldStatus( InetAddress ip, int port, int interval )
    {
        this.lsm.shieldStatus( ip, port, interval );
    }

    public void shieldReconfigure()
    {
        this.lsm.shieldReconfigure();
    }
}
