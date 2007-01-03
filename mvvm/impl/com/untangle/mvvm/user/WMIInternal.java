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

package com.untangle.mvvm.user;

import javax.wbem.client.PasswordCredential;
import javax.wbem.client.UserPrincipal;

import com.untangle.mvvm.networking.NetworkUtil;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ValidateException;

class WMIInternal
{
    /* XXX This should be separated out into a rule, this way you can have
     * a different WMI server for various networks.  the default
     * windows firewall rejects WMI requests that are not on your
     * local network. */
    private final boolean isEnabled;

    private final String username;
    private final String password;
    private final IPaddr address;
    private final String scheme;
    private final int port;
    private final String uri;

    private final UserPrincipal principal;
    private final PasswordCredential credentials;

    private WMIInternal( WMISettings settings, String uri )
    {
        this.isEnabled = settings.getIsEnabled();
        this.address = settings.getAddress();
        this.username = settings.getUsername();
        this.password = settings.getPassword();
        this.port = settings.getPort();
        this.scheme = settings.getScheme();
        this.uri = uri;

        this.principal = new UserPrincipal( this.username );
        this.credentials = new PasswordCredential( this.password );
    }

    boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    IPaddr getAddress()
    {
        return this.address;
    }

    String getURI() throws WMIException
    {
        if ( this.uri == null ) throw new WMIException( "Settings are invalid, unable to generate uri." );

        return this.uri;
    }

    UserPrincipal getPrincipal()
    {
        return this.principal;
    }

    PasswordCredential getCredentials()
    {
        return this.credentials;
    }

    WMISettings toSettings()
    {
        WMISettings settings = new WMISettings();
        settings.setIsEnabled( this.isEnabled );
        settings.setAddress( this.address );
        settings.setUsername( this.username );
        settings.setPassword( this.password );
        settings.setPort( this.port );
        settings.setScheme( this.scheme );
        return settings;
    }

    static WMIInternal makeInternal( WMISettings settings ) throws ValidateException
    {
        String uri = null;

        /* make sure the settings are legit */
        settings.validate();

        /* try to generate a uri */
        if ( settings.getIsEnabled()) {
            uri = settings.getScheme() + "://" + settings.getAddress() + ":" + settings.getPort();
        }

        return new WMIInternal( settings, uri );
    }


}