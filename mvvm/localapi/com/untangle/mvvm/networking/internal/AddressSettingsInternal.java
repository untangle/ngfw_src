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

package com.untangle.mvvm.networking.internal;

import com.untangle.mvvm.networking.AddressSettings;
import com.untangle.mvvm.networking.NetworkUtil;

import com.untangle.mvvm.tran.HostAddress;
import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.tran.IPaddr;

/** These are settings related to remote access to the untangle. */
public class AddressSettingsInternal
{
    private final int httpsPort;

    /* The following should go into address settings */
    private final HostName hostname;
    private final boolean isHostnamePublic;
    
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

        this.isHostnamePublic = settings.getIsHostNamePublic();
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
            url = ":" + this.currentPublicPort;
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
        return this.isHostnamePublic;
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
        /* *** the next call is a helper functino that just sets the public ip address and port.  */
        // settings.setPublicAddress( getPublicAddress());
        settings.setPublicIPaddr( getPublicIPaddr());
        settings.setPublicPort( getPublicPort());
        return settings;
    }

    public static AddressSettingsInternal makeInstance( AddressSettings settings, HostAddress currentAddress, 
                                                        int currentPort )
                                                        
    {
        return new AddressSettingsInternal( settings, currentAddress, currentPort ); 
    }
}


