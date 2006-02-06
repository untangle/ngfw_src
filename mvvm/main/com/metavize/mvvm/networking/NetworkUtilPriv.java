/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.argon.ArgonException;
import com.metavize.mvvm.argon.IntfConverter;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ValidateException;



/* Utilities that are only required inside of this package */
class NetworkUtilPriv extends NetworkUtil
{
    private static final NetworkUtilPriv INSTANCE = new NetworkUtilPriv();

    /* Prefix for the bridge devices */
    private static final String BRIDGE_PREFIX  = "br";

    private NetworkUtilPriv()
    {
    }

    /* Not a well named function, it is used before saving to update all of the indices
     * and the lists that go into the different objects that are referenced */
    void complete( NetworkSettings config ) throws NetworkException, ArgonException
    {
        int index = 1;
        IntfConverter ic = IntfConverter.getInstance();

        for ( NetworkSpace space : (List<NetworkSpace>)config.getNetworkSpaceList()) {
            /* Set the index of this network space */
            space.setIndex( index );
            
            /* Create a list of all of the interfaces beloning to this network space */
            List<Interface> spaceInterfaceList = new LinkedList<Interface>();
            Interface primary = null;
            for ( Interface intf : (List<Interface>)config.getInterfaceList()) {
                /* Keep track of the first interface, use this to set the device name later */
                if ( intf.getNetworkSpace().equals( space )) {
                    if ( primary == null ) primary = intf;
                    spaceInterfaceList.add( intf );
                }
            }

            if ( primary == null ) {
                throw new NetworkException( "The space [" + space + "] doesn't have any interfaces" );
            }
            
            space.setInterfaceList( spaceInterfaceList );

            /* Last set the name of the device */
            if ( space.isBridge()) {
                space.setDeviceName( BRIDGE_PREFIX + index );
            } else {
                space.setDeviceName( getDeviceName( primary ));
            }
            
            index++;
        }
    }

    String getDeviceName( Interface intf ) throws ArgonException
    {
        IntfConverter ic = IntfConverter.getInstance();
        
        return ic.argonIntfToString( intf.getArgonIntf());
    }

    static NetworkUtilPriv getPrivInstance()
    {
        return INSTANCE;
    }
}
