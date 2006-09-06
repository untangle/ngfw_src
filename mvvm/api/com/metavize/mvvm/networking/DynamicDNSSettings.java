/*
 * Copyright (c) 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;

import java.net.Inet4Address;
import java.net.InetAddress;

import com.metavize.mvvm.tran.IPaddr;

import java.io.Serializable;

/**
 * Dynamic DNS Configuration for the box.
 *
 * @version 1.0
 * @hibernate.class
 * table="mvvm_ddns_settings"
 */
public class DynamicDNSSettings implements Serializable
{
    // XXXXXXXX put serializable stuff in here
    
    private static final String PROVIDER_DYNDNS = "www.dyndns.org";
    private static final String PROVIDER_EASYDNS = "www.easydns.com";
    private static final String PROVIDER_ZONEEDIT = "www.zoneedit.com";

    private static final String PROTOCOL_DYNDNS = "dyndns2";
    private static final String PROTOCOL_EASYDNS = "easydns";
    private static final String PROTOCOL_ZONEEDIT = "zoneedit1";

    private static final String SERVER_DYNDNS = "members.dyndns.org";
    private static final String SERVER_EASYDNS = "members.easydns.com";
    private static final String SERVER_ZONEEDIT = "www.zoneedit.com";
    
    private static final String[] PROVIDER_ENUMERATION = { PROVIDER_DYNDNS, PROVIDER_EASYDNS, PROVIDER_ZONEEDIT };

    private Long id;
    private boolean enabled = false;
    private String provider = getProviderDefault();
    private String login = "";
    private String password = "";

    public DynamicDNSSettings()
    {
    }

    /**
     * @hibernate.id
     * column="settings_id"
     * generator-class="native"
     */
    Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * @return true if dynamic dns is enabled
     *
     * @hibernate.property
     * column="enabled"
     */
    public boolean isEnabled()
    {
        return this.enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * @return the provider for dynamic dns service
     *
     * @hibernate.property
     * column="provider"
     */
    public String getProvider()
    {
        return this.provider;
    }

    public void setProvider( String provider )
    {
        if ( provider == null || "".equals(provider) ) provider = getProviderDefault();
        this.provider = provider;
    }

    public static String[] getProviderEnumeration()
    {
        return PROVIDER_ENUMERATION;
    }

    public static String getProviderDefault()
    {
        return PROVIDER_ENUMERATION[0];
    }

    /**
     * @return the login used to log into the provider's service
     *
     * @hibernate.property
     * column="login"
     */
    public String getLogin()
    {
        return this.login;
    }

    public void setLogin( String login )
    {
        this.login = login;
    }

    /**
     * @return the password used to log into the provider's service
     *
     * @hibernate.property
     * column="password"
     */
    public String getPassword()
    {
        return this.password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }


    String getProtocol()
    {
        if (PROVIDER_DYNDNS.equals(provider))
            return PROTOCOL_DYNDNS;
        else if  (PROVIDER_EASYDNS.equals(provider))
            return PROTOCOL_EASYDNS;
        else if  (PROVIDER_ZONEEDIT.equals(provider))
            return PROTOCOL_ZONEEDIT;
        else
            throw new IllegalArgumentException("Unknown provider: " + provider);
    }

    String getServer()
    {
        if (PROVIDER_DYNDNS.equals(provider))
            return SERVER_DYNDNS;
        else if  (PROVIDER_EASYDNS.equals(provider))
            return SERVER_EASYDNS;
        else if  (PROVIDER_ZONEEDIT.equals(provider))
            return SERVER_ZONEEDIT;
        else
            throw new IllegalArgumentException("Unknown provider: " + provider);
    }
}
