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

package com.untangle.uvm.argon;

import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.jnetcap.NetcapSession;
import com.untangle.uvm.localapi.LocalIntfManager;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.NetworkSettingsListener;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import org.apache.log4j.Logger;

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

        if ( settings.getIsEnabled()) {
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
        } else {
            logger.debug( "Network spaces are disabled skipping all network spaces" );
        }

        /* Set this list once at the end to avoid locks */
        this.natMatcherList = Collections.unmodifiableList( matchers );
    }

    static class NatMatcher
    {
        /* Don't have to check the port or protocol, because all ports and
         * protocols match.  Don't have to check the server interface,
         * because, it is the same as the client interface. */
        private final IPMatcher   ipMatcher;
        private final IntfMatcher intfMatcher;
        private final Logger logger = Logger.getLogger(getClass());
        
        
        NatMatcher( IPMatcher ipMatcher, IntfMatcher intfMatcher )
        {
            this.ipMatcher = ipMatcher;
            this.intfMatcher = intfMatcher;
        }
        
        boolean isMatch( NetcapSession netcapSession )
        {
            LocalIntfManager lim = Argon.getInstance().getIntfManager();
            
            byte clientIntf = lim.toArgon( netcapSession.clientSide().interfaceId());
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
            for ( InterfaceInternal intf : intfList ) intfArray[c++] = intf.getArgonIntf().getArgon();
            
            IntfMatcher intfMatcher;
            try {
                intfMatcher = IntfMatcherFactory.getInstance().makeSetMatcher( intfArray );
            } catch ( ParseException e ) {
                Logger logger = Logger.getLogger(NatChecker.class);
                logger.error( "Error making set matcher, using the nil matcher.", e );
                intfMatcher = IntfMatcherFactory.getInstance().getNilMatcher();
            }
            
            return new NatMatcher( ip, intfMatcher );
        }
    }
}

