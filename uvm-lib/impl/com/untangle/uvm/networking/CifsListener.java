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

import org.apache.log4j.Logger;

import jcifs.Config;
import jcifs.netbios.NbtAddress;

import com.untangle.uvm.node.IPaddr;

import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;

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
        NetworkSpaceInternal internal = settings.getServiceSpace();
        
        logger.error( "CifsListener must use the settings from the interface database" );
        if ( internal == null ) return;
        
        IPNetwork network = internal.getPrimaryAddress();
        
        IPaddr address   = network.getNetwork();
        IPaddr netmask   = network.getNetmask();
        IPaddr broadcast = address.or( netmask.inverse());
                
        Config.setProperty( BROADCAST_PROPERTY,    broadcast.toString());
        Config.setProperty( SMB_BIND_PROPERTY,     address.toString());
        Config.setProperty( NETBIOS_BIND_PROPERTY, address.toString());

        NbtAddress.updateLocalAddress();
    }
}
