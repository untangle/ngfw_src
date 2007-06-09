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

package com.untangle.mvvm.tran.firewall;

import java.net.InetAddress;

import org.apache.log4j.Logger;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.ArgonException;
import com.untangle.mvvm.IntfConstants;

import com.untangle.mvvm.tran.firewall.intf.IntfMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcher;

public class InterfaceAddressRedirect extends InterfaceRedirect
{
    /* Update every minute */
    private static final long UPDATE_TIME_MS = 60000;

    private final InetAddress redirectAddress;

    /* Buffer for tracking the argon interface */
    private byte argonIntf;

    /* Last time this rule was updated in milliseconds */
    private long lastUpdate;

    private final Logger logger = Logger.getLogger(getClass());

    public InterfaceAddressRedirect( ProtocolMatcher protocol,
                                     IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                                     IPMatcher   srcAddress, IPMatcher       dstAddress,
                                     PortMatcher srcPort,    PortMatcher     dstPort,
                                     InetAddress redirectAddress )
    {
        /* InterfaceRedirects are always active */
        super( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );

        this.redirectAddress = redirectAddress;
        this.lastUpdate = -1;
        this.argonIntf = IntfConstants.NETCAP_LOOPBACK;
    }

    public InterfaceAddressRedirect( TrafficIntfRule trafficRule, InetAddress redirectAddress )
    {
        super( trafficRule );
        this.redirectAddress = redirectAddress;
        this.lastUpdate = -1;
        this.argonIntf = IntfConstants.NETCAP_LOOPBACK;
    }

    public byte argonIntf( byte argonDstIntf )
    {
        /* If the redirect address is null, return the current destination interface */
        if ( null == this.redirectAddress ) return argonDstIntf;

        long check = this.lastUpdate;

        /* If then there is nothing to do */
        if (( System.currentTimeMillis() - check ) > UPDATE_TIME_MS ) updateInterface( check );

        return this.argonIntf;
    }

    private synchronized void updateInterface( long check )
    {
        /* If the redirect address is null or the update already occured, there is nothing to do */
        if (( null == this.redirectAddress ) || ( check != this.lastUpdate )) return;

        try {
            byte newIntf = MvvmContextFactory.context().argonManager().
                getOutgoingInterface( this.redirectAddress );
            if ( newIntf == IntfConstants.NETCAP_LOOPBACK ) {
                logger.info( "Redirect is destined to local host, matching sessions will be dropped." );
            }

            this.argonIntf = newIntf;
        } catch ( ArgonException e ) {
            logger.info( "Cannot create an interface redirect since the destination interface for " +
                         this.redirectAddress + " cannot be determined", e );

            this.argonIntf = IntfConstants.NETCAP_LOOPBACK;
        }

        logger.debug( "Redirect[" + this.redirectAddress +  "] is going out netcap interface " +
                      this.argonIntf );

        /* Update the last time the interface was modified */
        this.lastUpdate = System.currentTimeMillis();
    }
}
