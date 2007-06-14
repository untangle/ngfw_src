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

package com.untangle.uvm.networking;

import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.UvmContextFactory;

import com.untangle.uvm.localapi.ArgonInterface;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.node.script.ScriptWriter;

class BridgeConfigurationWriter extends ScriptWriter
{
    private final Logger logger = Logger.getLogger(getClass());

    private final NetworkSpacesInternalSettings settings;

    BridgeConfigurationWriter( NetworkSpacesInternalSettings settings )
    {
        super();
        this.settings = settings;
    }

    void addBridgeConfiguration()
    {
        /* Iterate all of the network spaces, and dump out the configurations */
        for ( NetworkSpaceInternal space : this.settings.getNetworkSpaceList()) {
            addNetworkSpace( space );
            if ( !this.settings.getIsEnabled()) break;
        }
    }

    private void addNetworkSpace( NetworkSpaceInternal space )
    {
        /* If the space is not enabled, nothing to do */
        if ( !space.getIsEnabled()) return;

        List<InterfaceInternal> interfaceList = space.getInterfaceList();

        if ( interfaceList.isEmpty()) {
            logger.error( "Empty interface list for enabled space" );
            return;
        }

        String name = space.getDeviceName();
        if ( space.isBridge()) {
            /* Iterate all of the interfaces */
            for ( InterfaceInternal intf : interfaceList ) {
                ArgonInterface argonIntf = intf.getArgonIntf();

                boolean hasSecondaryInterface = argonIntf.hasSecondaryName();
                
                String physicalIntf = argonIntf.getPhysicalName();
                
                /* If there is no secondary interface, then the interface should be inside of the 
                 * bridge, otherwise, it won't be. */
                if ( !hasSecondaryInterface ) {
                    appendLine( name + " " + physicalIntf );
                }
                
                /* Always print the name of the device */
                appendLine( physicalIntf );
            }
        } else {
            appendLine( name );
        }
    }

    protected String header()
    {
        return "";
    }

}
