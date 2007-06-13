/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.user;

import java.net.InetAddress;

import com.untangle.uvm.license.ProductIdentifier;
import com.untangle.uvm.node.ValidateException;
import com.untangle.node.util.MVLogger;

class DefaultPhoneBookImpl implements LocalPhoneBook
{
    private final MVLogger logger = new MVLogger( getClass());

    DefaultPhoneBookImpl()
    {
    }

    /* ----------------- Public ----------------- */

    /* retrieve the WMI settings */
    public WMISettings getWMISettings()
    {
        logger.warn( "using invalid WMI settings" );
        return new WMISettings();
    }

    /* set the WMI settings */
    public void setWMISettings( WMISettings settings ) throws ValidateException
    {
        logger.warn( "ignoring save settings." );
    }

    /* Lookup the corresponding user user information object user the address */
    public UserInfo lookup( InetAddress address )
    {
        logger.debug( "ignoring lookup." );
        
        /* Always returns null */
        return null;
    }

    public void updateEntry( UserInfo info )
    {
        logger.debug( "ignoring update entry." );
    }

    /* Register a phone book assistant which is used to help with addres lookups */
    public void registerAssistant( Assistant newAssistant )
    {
        logger.debug( "ignoring register assistant." );
    }

    /* Unregister a phone book assistant */
    public void unregisterAssistant( Assistant assistant )
    {
        logger.debug( "ignoring unregister assistant." );
    }

    public void init()
    {
        logger.debug( "ignoring init." );
    }

    public void destroy()
    {
        logger.debug( "ignoring init." );
    }

    public String productIdentifier()
    {
        return ProductIdentifier.PHONE_BOOK;
    }

    /* ----------------- Package ----------------- */

    /* ----------------- Private ----------------- */
}
