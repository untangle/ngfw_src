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

package com.metavize.mvvm.networking;

import java.util.List;

import com.metavize.mvvm.networking.internal.InterfaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.tran.script.ScriptWriter;
import org.apache.log4j.Logger;

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
        for ( NetworkSpaceInternal space : this.settings.getNetworkSpaceList()) addNetworkSpace( space );
    }

    private void addNetworkSpace( NetworkSpaceInternal space )
    {
        /* If the space is not enabled, nothing to do */
        if ( !space.getIsEnabled()) return;

        List<InterfaceInternal> interfaceList = space.getInterfaceList();

        if ( interfaceList.size() < 1 ) {
            logger.error( "Empty interface list for enabled space" );
            return;
        }

        String name = space.getDeviceName();
        if ( space.isBridge()) {
            /* Iterate all of the interfaces */
            for ( InterfaceInternal intf : interfaceList ) {
                String dev = intf.getIntfName();
                appendLine( name + " " + dev );
                appendLine( dev );
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
