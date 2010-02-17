/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking.internal;

import com.untangle.uvm.networking.AddressSettings;
import com.untangle.uvm.networking.NetworkUtil;
import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

/** These are settings related to remote access to the untangle. */
public class AddressSettingsInternal
{
    private final int httpsPort;

    /* The following should go into address settings */
    private final HostName hostname;
    private final boolean isHostNamePublic;
    
    private final boolean isPublicAddressEnabled;
    /* publicAddress is computed using publicIPaddr and publicPort */
    private final String publicAddress;  
    private final IPaddr publicIPaddr;
    private final int publicPort;
    
    private final int currentPublicPort;
    private final HostAddress currentPublicAddress;
    private final String url;
    
    private AddressSettingsInternal( AddressSettings settings, HostAddress currentPublicAddress,
                                     int currentPublicPort )
    {
        this.httpsPort = settings.getHttpsPort();
        this.hostname = settings.getHostName();

        this.isHostNamePublic = settings.getIsHostNamePublic();
        this.isPublicAddressEnabled = settings.getIsPublicAddressEnabled();
        this.publicAddress = settings.getPublicAddress();
        this.publicIPaddr = settings.getPublicIPaddr();
        this.publicPort = settings.getPublicPort();

        /*** Fields that do not come from the settings object */
        this.currentPublicAddress = currentPublicAddress;
        this.currentPublicPort = currentPublicPort;

        String url = this.currentPublicAddress.toString();
        
        if (( NetworkUtil.DEF_HTTPS_PORT != currentPublicPort ) && 
            ( currentPublicPort > 0 ) && ( currentPublicPort < 0xFFFF )) {
            url += ":" + this.currentPublicPort;
        }
        
        this.url = url;
    }

    /* Get the port to run HTTPs on in addition to port 443. */
    public int getHttpsPort()
    {
        return this.httpsPort;
    }

    /** The hostname for the box(this is the hostname that goes into certificates). */
    public HostName getHostName()
    {
        return this.hostname;
    }
    
    /* Returns if the hostname for this box is publicly resolvable to this box */
    public boolean getIsHostNamePublic()
    {
        return this.isHostNamePublic;
    }

    /* True if the public address should be used */
    public boolean getIsPublicAddressEnabled()
    {
        return this.isPublicAddressEnabled;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getPublicAddress()
    {
        return this.publicAddress;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public IPaddr getPublicIPaddr()
    {
        return this.publicIPaddr;
    }

    /** @return the public port */
    public int getPublicPort()
    {
        return this.publicPort;
    }

    /** ******* the following Settings that are computed. */
    public HostAddress getCurrentAddress()
    {
        return this.currentPublicAddress;
    }

    /** @return the public url for the box, this is the address (may be hostname or ip address) */
    public String getCurrentURL()
    {
        return this.url;
    }

    public int getCurrentPublicPort()
    {
        return this.currentPublicPort;
    }

    public AddressSettings toSettings()
    {
        AddressSettings settings = new AddressSettings();
        settings.setHttpsPort( getHttpsPort());
        settings.setHostName( getHostName());
        settings.setIsHostNamePublic( getIsHostNamePublic());
        settings.setIsPublicAddressEnabled( getIsPublicAddressEnabled());
        settings.setPublicIPaddr( getPublicIPaddr());
        settings.setPublicPort( getPublicPort());
        return settings;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "https-port: " + this.httpsPort );
        sb.append( "\nhostname: " + this.hostname );
        sb.append( "\nis-hostname-public: " + this.isHostNamePublic );
        sb.append( "\nis-public-address-enabled: " + this.isPublicAddressEnabled );
        sb.append( "\npublic-address: " + this.publicAddress );
        sb.append( "\npublic-ip-addr: " + this.publicIPaddr );
        sb.append( "\npublic-port: " + this.publicPort );
        sb.append( "\ncurrent-public-address: " +  this.currentPublicAddress );
        sb.append( "\ncurrent-public-port: " +  this.currentPublicPort );
        sb.append( "\nurl: " +  this.url );
        return sb.toString();
    }

    public static AddressSettingsInternal makeInstance( AddressSettings settings, HostAddress currentAddress,
                                                        int currentPort )
    {
        return new AddressSettingsInternal( settings, currentAddress, currentPort ); 
    }
}


