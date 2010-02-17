/*
 * $HeadURL$
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

package com.untangle.uvm.networking;

import jcifs.Config;
import jcifs.netbios.NbtAddress;

import org.apache.log4j.Logger;

import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.IPaddr;

class CifsListener implements NetworkSettingsListener
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String BROADCAST_PROPERTY    = "jcifs.netbios.baddr";
    private static final String SMB_BIND_PROPERTY     = "jcifs.smb.client.laddr";
    private static final String NETBIOS_BIND_PROPERTY = "jcifs.netbios.laddr";

    CifsListener()
    {
    }
    
    public void event( NetworkSpacesInternalSettings settings )
    {       
        NetworkSpaceInternal space = settings.getNetworkSpace( IntfConstants.INTERNAL_INTF );

        if ( space == null ) {
            logger.warn( "Internal interface doesn't have a network space." );
            return;
        }

        IPNetwork network = space.getPrimaryAddress();

        if ( network == null ) {
            logger.warn( "Internal interface doesn't have a primary address." );
            return;
        }
        
        IPaddr address   = network.getNetwork();
        IPaddr netmask   = network.getNetmask();
        IPaddr broadcast = address.or( netmask.inverse());

        logger.debug( "Upading the local address: " + address + "/" + netmask + "," + broadcast );
                
        Config.setProperty( BROADCAST_PROPERTY,    broadcast.toString());
        Config.setProperty( SMB_BIND_PROPERTY,     address.toString());
        Config.setProperty( NETBIOS_BIND_PROPERTY, address.toString());

        NbtAddress.updateLocalAddress();
    }
}
