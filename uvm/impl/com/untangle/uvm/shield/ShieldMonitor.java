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

package com.untangle.uvm.shield;

import java.net.InetAddress;

import com.untangle.jnetcap.Shield;
import com.untangle.jnetcap.ShieldEventListener;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.EventLogger;
import org.apache.log4j.Logger;

public class ShieldMonitor implements ShieldEventListener
{
    private static ShieldMonitor INSTANCE = null;

    private final EventLogger eventLogger = UvmContextFactory.context().eventLogger();
    private final Logger logger = Logger.getLogger( this.getClass());

    private ShieldMonitor()
    {
    }

    public void rejectionEvent( InetAddress ip, byte clientIntf, double reputation, int mode,
                                int limited, int dropped, int rejected )

    {
        if ( Thread.currentThread().getContextClassLoader() == null ) {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader());
        }

        logger.warn( "Shield limited session(s) from " + ip + " with reputation " + reputation +
                     " limited: " + limited + " dropped: " + dropped + " rejected: " + rejected  );

        try {
            clientIntf = UvmContextFactory.context().localIntfManager().toArgon( clientIntf );

            eventLogger.log( new ShieldRejectionEvent( ip, clientIntf, reputation, mode, limited, dropped,
                                                       rejected ));
        } catch ( IllegalArgumentException e ) {
            logger.warn( "Invalid interface for shield rejection event: " + clientIntf );
        }
    }

    public void statisticEvent( int accepted, int limited, int dropped, int rejected, int relaxed,
                                int lax, int tight, int closed )
    {
        if ( Thread.currentThread().getContextClassLoader() == null ) {
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader());
        }

        eventLogger.log( new ShieldStatisticEvent( accepted, limited, dropped, rejected, relaxed,
                                                   lax, tight, closed ));
    }

    public synchronized static ShieldMonitor getInstance()
    {
        if ( INSTANCE == null ) {
            INSTANCE = new ShieldMonitor();
        }

        return INSTANCE;
    }

}
