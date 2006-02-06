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

import org.apache.log4j.Logger;

import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.IntfEnum;

import com.metavize.mvvm.argon.ArgonException;

import com.metavize.mvvm.tran.ValidateException;
import com.metavize.mvvm.tran.script.ScriptWriter;

/* XXX This shouldn't be public */
public class NetworkManagerImpl implements NetworkManager
{
    private static NetworkManager INSTANCE = new NetworkManagerImpl();

    // private static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    private static final String ETC_INTERFACES_FILE = "/localhome/rbscott/playground/networking/test-";
    
    private static final Logger logger = Logger.getLogger( NetworkManagerImpl.class );

    private NetworkSettings configuration = new NetworkSettings();

    /* XXX This is just for testing */
    private static int testIteration = 1;
    /* XXXX */
    
    private NetworkManagerImpl()
    {
    }

    public BasicNetworkSettings getBasicNetworkSettings()
    {
        refresh();
        return this.configuration.toBasicConfiguration();
    }

    public void setNetworkSettings( BasicNetworkSettings configuration )
        throws NetworkException, ValidateException
    {
        this.configuration = configuration.getNetworkSettings();
        saveConfiguration();
    }
    
    public NetworkSettings getNetworkSettings()
    {
        refresh();
        return this.configuration;
    }

    public void setNetworkSettings( NetworkSettings configuration )
        throws NetworkException, ValidateException
    {
        NetworkUtil.getInstance().validate( configuration );

        this.configuration = configuration;
        saveConfiguration();
    }

    /* Get the external HTTPS port */
    public int getExternalHttpsPort()
    {
        /* !!!!!!!!!!!!! */
        return 443;
    }
    
    /* Renew the DHCP address and return a new network settings with the updated address */
    public NetworkSettings renewDhcpLease() throws Exception
    {
        /* XXXXXX!!!!!!!!!!!!!! renew the leases */
        return this.configuration;
    }

    /* Retrieve the enumeration of all of the active interfaces */
    public IntfEnum getIntfEnum()
    {
        return null;
    }

    public String getHostname()
    {
        return this.configuration.getHostname();
    }

    public String getPublicAddress()
    {
        return this.configuration.getPublicAddress();
    }


    public void refresh()
    {
        logger.error( "Refresh is very much totally busted." );
    }
    
    public void updateAddress()
    {
        
    }
    
    private void saveConfiguration() throws NetworkException
    {
        try {
            NetworkUtilPriv.getPrivInstance().complete( this.configuration );

            saveEtcFiles();
            saveBridgeConfiguration();
            saveIpTablesConfiguration();
        } catch ( ArgonException e ) {
            logger.error( "Unable to save network settings" );
        }
        
        testIteration++;
    }

    private void saveEtcFiles() throws NetworkException, ArgonException
    {
        saveInterfaces();
        saveResolvConf();
    }

    /* This is for /etc/network/interfaces interfaces */
    private void saveInterfaces() throws NetworkException, ArgonException
    {
        /* This is a script writer customized to generate etc interfaces files */
        InterfacesScriptWriter isw = new InterfacesScriptWriter( this.configuration );
        
        isw.addNetworkSettings();
        isw.writeFile( String.format( ETC_INTERFACES_FILE + "%02d/interfaces", testIteration ));
    }

    private void saveResolvConf()
    {
    }

    private void saveBridgeConfiguration()
    {
    }

    private void saveIpTablesConfiguration()
    {
        
    }

    /* XXXXXX Check permission */
    public static NetworkManager getInstance()
    {
        return INSTANCE;
    }
}
