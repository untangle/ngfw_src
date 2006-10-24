/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import java.util.List;

import org.apache.log4j.Logger;

import com.metavize.mvvm.ArgonException;
import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.localapi.ArgonInterface;
import com.metavize.mvvm.networking.internal.InterfaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.tran.script.ScriptWriter;

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
