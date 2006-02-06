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

import java.io.BufferedReader;
import java.io.FileReader;

import java.util.List;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.metavize.mvvm.argon.ArgonException;
import com.metavize.mvvm.argon.IntfConverter;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ValidateException;



/* Utilities that are only required inside of this package */
class NetworkUtilPriv extends NetworkUtil
{
    private static final Logger logger = Logger.getLogger( NetworkUtilPriv.class );

    private static final NetworkUtilPriv INSTANCE = new NetworkUtilPriv();

    /* Prefix for the bridge devices */
    private static final String BRIDGE_PREFIX  = "br";

    private NetworkUtilPriv()
    {
    }

    /* Convert an Advanced settings object into a basic settings object */
    BasicNetworkSettings toBasicNetworkSettings( NetworkSettings settings )
    {
        NetworkSpace space;

        logger.debug( "Converting to basic network settings" );
        List<NetworkSpace> networkSpaceList = (List<NetworkSpace>)settings.getNetworkSpaceList();
        
        if ( networkSpaceList.size() < 1 ) {
            logger.error( "Network settings must have at least one network space, null pointer time." );
            /* !!! Something has to deal with this */
            throw new NullPointerException( "state inconsistency" );
        }

        space = networkSpaceList.get( 0 );

        logger.debug( "Setting the primary network space to [" + space + "]" );

        List<IPNetworkRule> aliasList = new LinkedList<IPNetworkRule>( space.getNetworkList());
        IPNetwork primary = IPNetwork.getEmptyNetwork();
        if (( !space.getIsDhcpEnabled()) && ( aliasList.size() > 0 )) {
            primary = aliasList.remove( 0 ).getIPNetwork();
        }

        logger.debug( "done" );
        
        return new BasicNetworkSettings( settings, space, primary, aliasList );
    }
    
    /* Convert a basic settings object back to an advanced settings object */
    NetworkSettings toNetworkSettings( BasicNetworkSettings basicSettings ) throws NetworkException
    {
        NetworkSettings settings = basicSettings.getNetworkSettings();
        
        NetworkSpace space = basicSettings.getNetworkSpace();
        
        /* The alias list is the correct list to use, the list that is in
         * the network space is no longer valid after once NetworkSettings
         * are converted to BasicNetworkSettings. */
        List<IPNetworkRule> networkList = basicSettings.getAliasList();
        IPNetwork primaryAddress = basicSettings.getPrimaryAddress();
        
        if ( space.getIsDhcpEnabled()) {
            /* If DHCP is enabled, clear out dns and default route settings */
            
            settings.setDns1( null );
            settings.setDns2( null );
            settings.setDefaultRoute( null );
        } else {
            /* If DHCP is not enabled, add the primary network to the network list */
            if ( primaryAddress.getNetwork().isEmpty() || primaryAddress.getNetmask().isEmpty()) {
                logger.warn( "Primary address or netmask should not be empty." );
            } else {
                networkList.add( 0, new IPNetworkRule( primaryAddress ));
            }
        }

        space.setNetworkList( networkList );

        return settings;
    }

    /* Not a well named function, it is used before saving to update all of the indices
     * and the lists that go into the different objects that are referenced */
    void complete( NetworkSettings config ) throws NetworkException
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

    String getDeviceName( Interface intf ) throws NetworkException
    {
        IntfConverter ic = IntfConverter.getInstance();
        
        try {
            return ic.argonIntfToString( intf.getArgonIntf());
        } catch ( ArgonException e ) {
            throw new NetworkException( "Error getting interface string [" + intf.getArgonIntf() + "]", e );
        }
    }

    /* This retrieves a list of all of the DNS servers */
    List<IPaddr> getDnsServers()
    {
        List<IPaddr> dnsServers = new LinkedList<IPaddr>();

        BufferedReader in = null;

        /* Open up the interfaces file */
        try {
            in = new BufferedReader( new FileReader( NetworkManagerImpl.ETC_RESOLV_FILE ));
            String str;
            while (( str = in.readLine()) != null ) {
                str = str.trim();
                if ( str.startsWith( ResolvScriptWriter.NS_PARAM )) {
                    dnsServers.add( IPaddr.parse( str.substring( ResolvScriptWriter.NS_PARAM.length())));
                }
            }
        } catch ( Exception ex ) {
            logger.error( "Error reading file: ", ex );
        }

        try {
            if ( in != null ) in.close();
        } catch ( Exception ex ) {
            logger.error( "Unable to close file", ex );
        }

        return dnsServers;
    }


    static NetworkUtilPriv getPrivInstance()
    {
        return INSTANCE;
    }
}
