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

package com.metavize.mvvm.tran.firewall;

import com.metavize.mvvm.argon.IntfConverter;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfSimpleMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPSimpleMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortSimpleMatcher;
import org.apache.log4j.Logger;

public class InterfaceStaticRedirect extends InterfaceRedirect
{
    private final byte argonIntf;
    private final Logger logger = Logger.getLogger(getClass());

   /* Null matcher, these are automatically removed before adds */
    private static final InterfaceRedirect NIL_REDIRECT =
        new InterfaceStaticRedirect( ProtocolMatcher.MATCHER_NIL,
                                     IntfSimpleMatcher.getNilMatcher(), IntfSimpleMatcher.getNilMatcher(),
                                     IPSimpleMatcher.getNilMatcher(), IPSimpleMatcher.getNilMatcher(),
                                     PortSimpleMatcher.getNilMatcher(), PortSimpleMatcher.getNilMatcher(),
                                     (byte)0 );

    public InterfaceStaticRedirect( ProtocolMatcher protocol,
                                    IntfMatcher srcIntf,    IntfMatcher     dstIntf,
                                    IPMatcher   srcAddress, IPMatcher       dstAddress,
                                    PortMatcher srcPort,    PortMatcher     dstPort,
                                    byte argonIntf )
    {
        /* InterfaceRedirects are always active */
        super( protocol, srcIntf, dstIntf, srcAddress, dstAddress, srcPort, dstPort );

        this.argonIntf = argonIntf;
    }

    public byte netcapIntf( byte argonDstIntf )
    {
        return IntfConverter.toNetcap( this.argonIntf );
    }

    public byte argonIntf( byte argonDstIntf )
    {
        return this.argonIntf;
    }

    public static InterfaceRedirect getNilRedirect()
    {
        return NIL_REDIRECT;
    }
}
