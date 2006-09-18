/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.argon;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.Properties;

import java.io.File;
import java.io.FileOutputStream;

import org.apache.log4j.Logger;

import com.metavize.jnetcap.Netcap;
import com.metavize.jnetcap.InterfaceData;
import com.metavize.jnetcap.JNetcapException;

import com.metavize.mvvm.ArgonException;
import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.localapi.SessionMatcher;

import com.metavize.mvvm.tran.firewall.InterfaceRedirect;

public class ArgonManagerImpl implements ArgonManager
{
    private static final ArgonManagerImpl INSTANCE = new ArgonManagerImpl();
    
    private final Netcap netcap = Netcap.getInstance();

    private final Logger logger = Logger.getLogger( this.getClass());

    private boolean isShutdown = false;
        
    private ArgonManagerImpl()
    {
    }

    /* Indicate that the shutdown process has started, this is used to prevent NAT from both
     * re-enabling the bridge during shutdown, Argon will do that automatically.
     * This also prevents transforms from registering or deregistering interfaces after shutdown 
     */
    synchronized void isShutdown() 
    {
        isShutdown = true;
    }    
            
    /* Set the interface override list. */
    public void setInterfaceOverrideList( List<InterfaceRedirect> overrideList )
    {
        InterfaceOverride.getInstance().setOverrideList( overrideList );
    }

    /* Set the interface override list. */
    public void clearInterfaceOverrideList()
    {
        InterfaceOverride.getInstance().clearOverrideList();
    }
    
    /* Get the outgoing argon interface for an IP address */
    public byte getOutgoingInterface( InetAddress destination ) throws ArgonException
    {
        try {
            byte netcapIntf = netcap.getOutgoingInterface( destination );
            return IntfConverter.toArgon( netcapIntf );
        } catch ( JNetcapException e ) {
            throw new ArgonException( e );
        }
    }

    public byte[] getArgonIntfArray()
    {
        return IntfConverter.getInstance().argonIntfArray();
    }

    public void registerIntf( byte argonIntf, String deviceName )
        throws ArgonException
    {
        if ( isShutdown ) {
            logger.info( "Already shutdown, unable to register an interface" );
            return;
        }
        
        if ( !IntfConverter.getInstance().registerIntf( argonIntf, deviceName )) {
            logger.debug( "Ignoring interface that is already registered" );
            return;
        }
    }
    
    public void deregisterIntf( byte argonIntf )
        throws ArgonException
    {
        if ( isShutdown ) {
            logger.info( "Already shutdown, unable to deregister interface" );
            return;
        }

        if ( !IntfConverter.getInstance().deregisterIntf( argonIntf )) {
            logger.debug( "Ignoring interface that is not registered" );
            return;
        }
    }

    
    /** Get the number of sessions from the VectronTable */
    public int getSessionCount()
    {
        return VectronTable.getInstance().count();
    }
    
    /** Shutdown all of the sessions that match <code>matcher</code> */
    public void shutdownMatches( SessionMatcher matcher )
    {
        VectronTable.getInstance().shutdownMatches( matcher );
    }

    public static final ArgonManagerImpl getInstance()
    {
        return INSTANCE;
    }

    private void pause()
    {
        try {
            Thread.sleep( 2000 );
        } catch ( Exception e ) {
            logger.warn( "Interrupted while pausing", e );
        }
    }
}
