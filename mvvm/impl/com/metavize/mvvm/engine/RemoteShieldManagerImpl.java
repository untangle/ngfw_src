/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import com.metavize.mvvm.shield.ShieldNodeSettings;

import java.net.InetAddress;

import java.util.List;

import com.metavize.mvvm.api.RemoteShieldManager;
import com.metavize.mvvm.localapi.LocalShieldManager;

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
