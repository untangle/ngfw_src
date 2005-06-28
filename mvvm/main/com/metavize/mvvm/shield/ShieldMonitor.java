/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.shield;

import java.net.InetAddress;
import org.apache.log4j.Logger;

import com.metavize.jnetcap.Shield;
import com.metavize.jnetcap.ShieldEventListener;

import com.metavize.mvvm.MvvmContextFactory;

public class ShieldMonitor implements ShieldEventListener
{
    private static ShieldMonitor INSTANCE = null;

    private Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private ShieldMonitor()
    {        
    }

    public void event( InetAddress ip, double reputation, int mode, int limited, int rejected, int dropped )
    {
        if ( Thread.currentThread().getContextClassLoader() == null ) {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader());
        }

        eventLogger.info( new ShieldEvent( ip, reputation, mode, limited, rejected, dropped ));
    }

    public synchronized static ShieldMonitor getInstance()
    {
        if ( INSTANCE == null ) {
            INSTANCE = new ShieldMonitor();
        }

        return INSTANCE;
    }
    
}
