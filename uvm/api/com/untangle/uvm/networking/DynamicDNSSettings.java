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

package com.untangle.uvm.networking;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.IPaddr;

/**
 * Dynamic DNS Configuration for the box.
 *
 * @version 1.0
 */
@Entity
@Table(name="uvm_ddns_settings", schema="settings")
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

    public DynamicDNSSettings() { }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
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
     */
    @Column(nullable=false)
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

    @Transient
    public static String[] getProviderEnumeration()
    {
        return PROVIDER_ENUMERATION;
    }

    @Transient
    public static String getProviderDefault()
    {
        return PROVIDER_ENUMERATION[0];
    }

    /**
     * @return the login used to log into the provider's service
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
     */
    public String getPassword()
    {
        return this.password;
    }

    public void setPassword( String password )
    {
        this.password = password;
    }

    @Transient
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

    @Transient
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
