/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;


import com.metavize.jnetcap.NetcapSession;

import com.metavize.mvvm.networking.IPNetwork;
import com.metavize.mvvm.networking.NetworkSettingsListener;

import com.metavize.mvvm.networking.internal.InterfaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;

import com.metavize.mvvm.tran.ParseException;
import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;

/* Class used to determine if a session is going to be Nattd */
class NatChecker implements NetworkSettingsListener
{
    private List<NatMatcher> natMatcherList = new LinkedList<NatMatcher>();

    private final Logger logger = Logger.getLogger( getClass());

    NatChecker()
    {
    }
    
    boolean isNat( NetcapSession client )
    {
        for ( NatMatcher matcher : this.natMatcherList ) {
            if ( matcher.isMatch( client )) return true;
        }
        
        return false;
    }

    /* Update the list of NAT matchers */
    public void event( NetworkSpacesInternalSettings settings )
    {
        /* Indicate that the event occured */
        logger.info( "Updating the NAT matchers for argon." );

        List<NatMatcher> matchers = new LinkedList<NatMatcher>();
        for ( NetworkSpaceInternal space : settings.getNetworkSpaceList()) {
            logger.info( "Updating the network space: " + space.getIndex());
            if ( space.getIsEnabled() && space.getIsNatEnabled()) {
                for ( IPNetwork network : space.getNetworkList()) {
                    matchers.add( NatMatcher.makeMatcher( space, network ));
                }
            } else {
                logger.info( "NAT is disabled for network space:" + space.getIndex());
            }
        }

        /* Set this list once at the end to avoid locks */
        this.natMatcherList = Collections.unmodifiableList( matchers );        
    }
}

class NatMatcher
{
    /* Don't have to check the port or protocol, because all ports and
     * protocols match.  Don't have to check the server interface,
     * because, it is the same as the client interface. */
    private final IPMatcher   ipMatcher;
    private final IntfMatcher intfMatcher;
    private static final Logger logger = Logger.getLogger( NatChecker.class );
    

    NatMatcher( IPMatcher ipMatcher, IntfMatcher intfMatcher )
    {
        this.ipMatcher = ipMatcher;
        this.intfMatcher = intfMatcher;
    }

    boolean isMatch( NetcapSession netcapSession )
    {
        IntfConverter ic = IntfConverter.getInstance();

        byte clientIntf = ic.toArgon( netcapSession.clientSide().interfaceId());
        InetAddress clientAddr = netcapSession.clientSide().client().host();

        return this.intfMatcher.isMatch( clientIntf ) && this.ipMatcher.isMatch( clientAddr );
    }
    
    public String toString()
    {
        return "<NatMatcher: " + ipMatcher + "/" + intfMatcher + ">";
    }


    static NatMatcher makeMatcher( NetworkSpaceInternal space, IPNetwork network )
    {
        IPMatcher ip = IPMatcherFactory.getInstance().
            makeSubnetMatcher( network.getNetwork(), network.getNetmask());
        
        List<InterfaceInternal> intfList = space.getInterfaceList();

        byte intfArray[] = new byte[intfList.size()];
        
        int c = 0 ; 
        for ( InterfaceInternal intf : intfList ) intfArray[c++] = intf.getArgonIntf();
        
        IntfMatcher intfMatcher;
        try {
            intfMatcher = IntfMatcherFactory.getInstance().makeSetMatcher( intfArray );
        } catch ( ParseException e ) {
            logger.error( "Error making set matcher, using the nil matcher.", e ); 
            intfMatcher = IntfMatcherFactory.getInstance().getNilMatcher();
        }
        
        return new NatMatcher( ip, intfMatcher );
    }
}

