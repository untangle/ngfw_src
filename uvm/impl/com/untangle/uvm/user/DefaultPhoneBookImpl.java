/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.user;

import java.net.InetAddress;

import com.untangle.node.util.UtLogger;
import com.untangle.uvm.license.ProductIdentifier;
import com.untangle.uvm.node.ValidateException;

class DefaultPhoneBookImpl implements LocalPhoneBook
{
    private final UtLogger logger = new UtLogger( getClass());

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
