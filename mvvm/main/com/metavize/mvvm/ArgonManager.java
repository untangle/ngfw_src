/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: Argon.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm;

import java.net.InetAddress;

import java.util.List;

import com.metavize.mvvm.argon.ArgonException;
import com.metavize.mvvm.tran.firewall.InterfaceRedirect;

import com.metavize.mvvm.shield.ShieldNodeSettings;

public interface ArgonManager
{
    /* If interval is greater than zero, this will loop indefinitely,
     * sleep for interval milliseconds in between. */
    void shieldStatus( InetAddress ip, int port, int interval );

    void shieldReconfigure();
    
    /* Set the list of interface overrides */
    public void setInterfaceOverrideList( List<InterfaceRedirect>overrideList );

    /* Clear the list of interface overrides */
    public void clearInterfaceOverrideList();

    /* Get the outgoing argon interface for an IP address */
    public byte getOutgoingInterface( InetAddress destination ) throws ArgonException;

    /* Set the shield node rules */
    public void setShieldNodeSettings( List<ShieldNodeSettings> shieldNodeSettingsList ) 
        throws ArgonException;

    /* Index is the argon index of the interface, name is the device
     * name (eg tun0 or tap0).
     * @throws ArgonException: Index is IntfConstants.Internal,
     * IntfConstants.External or IntfConstants.DMZ. or the argonIndex is invalid */
    public void registerIntf( byte argonIntf, String name )
        throws ArgonException;

    /* Index is the argon index of the interface, name is the device
     * name (eg tun0 or tap0).
     * @throws ArgonException: Index is IntfConstants.Internal,
     * IntfConstants.External or IntfConstants.DMZ. or the argonIndex is invalid */
    public void deregisterIntf( byte argonIntf )
        throws ArgonException;

    /** Add an interface to the internal bridge */
    public void enableInternalBridgeIntf( NetworkingConfiguration netConfig, String intf )
        throws ArgonException;
    
    /** Remove all interfaces bridged with the internal interface */
    public void disableInternalBridgeIntf( NetworkingConfiguration netConfig )
        throws ArgonException;;
    
}
