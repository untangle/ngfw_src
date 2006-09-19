/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.localapi;

import java.util.List;

import com.metavize.mvvm.ArgonException;

import com.metavize.mvvm.api.RemoteIntfManager;
import com.metavize.mvvm.localapi.ArgonInterface;

public interface LocalIntfManager extends RemoteIntfManager
{
    /* Convert from an argon interface to a netcap interface */
    public byte toNetcap( byte argonIntf );
    
    /* Convert from a netcpa interface to an argon interface */
    public byte toArgon( byte netcapIntf );

    /* Convert from an argon interface to the physical name of the interface */
    public String argonIntfToString( byte argonIntf ) throws ArgonException;
      
    /* Retrieve the interface that corresponds to a specific argon interface */
    public ArgonInterface getIntfByArgon( byte argonIntf ) throws ArgonException;

    /* Retrieve the interface that corresponds to a specific netcap interface */
    public ArgonInterface getIntfByNetcap( byte netcapIntf ) throws ArgonException;

    /* Retrieve the interface that corresponds to the name */
    public ArgonInterface getIntfByName( String name ) throws ArgonException;

    /* Get the External interface */
    public ArgonInterface getExternal();

    /* Get the Internal interface */
    public ArgonInterface getInternal();

    /* This maybe null */
    public ArgonInterface getDmz();

    List<ArgonInterface> getIntfList();
    
    /* This is a list of non-physical interfaces (everything except for internal, external and dmz ).
     * This list would contain interfaces like VPN. */
    public List<ArgonInterface> getCustomIntfList();

    /* Return an array of the argon interfaces */
    public byte[] getArgonIntfArray();
    
    /* Register a replacement or custom interface.  EG. VPN or PPP0 */
    public void registerIntf( String name, byte argon ) throws ArgonException;

    /* Deregister a custom interface or DMZ. */
    public void deregisterIntf( byte argon ) throws ArgonException;
}

