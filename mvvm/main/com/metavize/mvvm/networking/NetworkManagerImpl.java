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

import com.metavize.mvvm.argon.ArgonException;
import com.metavize.mvvm.tran.ValidateException;

import com.metavize.mvvm.tran.script.ScriptWriter;

class NetworkManagerImpl
{
    // private static final String ETC_INTERFACES_FILE = "/etc/network/interfaces";
    private static final String ETC_INTERFACES_FILE = "/localhome/rbscott/playground/networking/test-";
    
    private static final Logger logger = Logger.getLogger( NetworkManagerImpl.class );

    private static NetworkManagerImpl INSTANCE = new NetworkManagerImpl();

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
        throws NetworkException, ArgonException
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
        throws NetworkException, ArgonException, ValidateException
    {
        NetworkUtil.getInstance().validate( configuration );

        this.configuration = configuration;
        saveConfiguration();
    }

    public void refresh()
    {
        logger.error( "Refresh is very much totally busted." );
    }
    
    public void updateAddress()
    {
        
    }
    
    private void saveConfiguration() throws NetworkException, ArgonException
    {
        NetworkUtilPriv.getPrivInstance().complete( this.configuration );

        saveEtcFiles();
        saveBridgeConfiguration();
        saveIpTablesConfiguration();
        
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

    static NetworkManagerImpl getInstance()
    {
        return INSTANCE;
    }
}
