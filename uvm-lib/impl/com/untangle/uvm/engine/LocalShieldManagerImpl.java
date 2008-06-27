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

package com.untangle.uvm.engine;

import com.untangle.uvm.shield.ShieldNodeSettings;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.untangle.uvm.ArgonException;
import com.untangle.uvm.localapi.LocalShieldManager;

class LocalShieldManagerImpl implements LocalShieldManager
{
    private boolean isShieldEnabled = true;
    private String shieldFile       = null;

    private final Logger logger = Logger.getLogger( this.getClass());

    LocalShieldManagerImpl()
    {
    }

    /* Toggle whether or not the shield is enabled, the real
     * determininant is actually controlled by Netcap.init which is
     * called from Argon. (It is presently hard to turn the shield on
     * and then off.) */
    public void setIsShieldEnabled( boolean isEnabled )
    {
        this.isShieldEnabled = isEnabled;
    }

    public void shieldReconfigure()
    {
        /* Do nothing if the shield is not enabled */
        if ( !this.isShieldEnabled ) {
            logger.debug( "The shield is disabled" );
            return;
        }

        logger.warn( "implement me" );
    }

    /* Update the shield node settings */
    public void setShieldNodeSettings( Set<ShieldNodeSettings> shieldNodeSettings ) 
        throws ArgonException
    {
        /* Do nothing if the shield is not enabled */
        if ( !this.isShieldEnabled ) {
            logger.debug( "The shield is disabled" );
            return;
        }

        List <com.untangle.jnetcap.ShieldNodeSettings> settingsList = 
            new LinkedList<com.untangle.jnetcap.ShieldNodeSettings>();

        for ( ShieldNodeSettings settings : shieldNodeSettings ) {
            InetAddress netmask;
            try {
                byte full = (byte)255;
                netmask = InetAddress.getByAddress( new byte[]{ full, full, full, full } );
            } catch( UnknownHostException e ) {
                logger.error( "Unable to parse default netmask", e );
                throw new ArgonException( e );
            }

            if ( settings == null ) {
                logger.error( "NULL Settings in list\n" );
                continue;
            }

            if ( !settings.isLive()) {
                logger.debug( "Ignoring disabled settings" );
                continue;
            }

            if ( settings.getAddress() == null || settings.getAddress().isEmpty()) {
                logger.error( "Settings with empty address, ignoring" );
                continue;
            }
            
            if ( settings.getNetmask() != null && !settings.getNetmask().isEmpty()) {
                logger.warn( "Settings with non-empty netmask, ignoring netmask using 255.255.255.255" );
            }
            
            logger.debug( "Adding shield node setting " + settings.getAddress().toString() + "/" + 
                          netmask.getHostAddress() + " divider: " + settings.getDivider());
                        
            settingsList.add( new com.untangle.jnetcap.ShieldNodeSettings( settings.getDivider(), 
                                                                           settings.getAddress().getAddr(), 
                                                                           netmask ));
        }

        logger.warn( "implement me" );
    }
}
