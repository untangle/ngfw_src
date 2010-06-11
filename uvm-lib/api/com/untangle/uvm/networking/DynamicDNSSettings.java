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

package com.untangle.uvm.networking;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Dynamic DNS Configuration for the box.
 *
 * @version 1.0
 */
@Entity
@Table(name="u_ddns_settings", schema="settings")
public class DynamicDNSSettings implements Serializable
{
    
    private static final String PROVIDER_DYNDNS = "www.dyndns.com";
    private static final String PROVIDER_DYNDNS_OLD = "www.dyndns.org";
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

    @SuppressWarnings("unused")
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
	else if (PROVIDER_DYNDNS_OLD.equals(provider))
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
	else if (PROVIDER_DYNDNS_OLD.equals(provider))
            return SERVER_DYNDNS;
        else if  (PROVIDER_EASYDNS.equals(provider))
            return SERVER_EASYDNS;
        else if  (PROVIDER_ZONEEDIT.equals(provider))
            return SERVER_ZONEEDIT;
        else
            throw new IllegalArgumentException("Unknown provider: " + provider);
    }
}
