/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;

/**
 * Settings for the open vpn transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_vpn_settings"
 */
public class VpnSettings implements Serializable, Validatable
{
    // XXX update the serial version id
    // private static final long serialVersionUID = 4143567998376955882L;

    private final int KEY_SIZE_ENUMERATION[] = new int[] { 1024, 1536, 2048 };
    private final int KEY_SIZE_DEFAULT       = KEY_SIZE_ENUMERATION[0];

    private Long id;
    private Tid tid;

    private boolean isBridgeMode;

    private boolean keepAlive;

    /* The interface that clients from the client pool are associated with */
    private byte clientPoolIntf;

    private IPaddr clientPoolAddress;
    private IPaddr clientPoolNetmask;

    private boolean exposeClients;

    private int maxClients;

    /* Certificate information */
    private String  domain;
    private int     keySize = KEY_SIZE_DEFAULT;
    private String  country;
    private String  province;
    private String  organization;
    private String  organizationUnit;
    private String  email;
    private boolean isCaKeyOnUsb;
    
    public VpnSettings() 
    {
    }

    public VpnSettings( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Transform id for these settings.
     *
     * @return tid for these settings
     * @hibernate.many-to-one
     * column="TID"
     * not-null="true"
     */
    public Tid getTid()
    {
        return tid;
    }

    public void setTid( Tid tid )
    {
        this.tid = tid;
    }

    /** Network settings for the VPN */
    
    
    /**
     * @return whether the vpn is in bridge mode.
     * column="is_bridge"
     */
    public boolean isBridgeMode()
    {
        return this.isBridgeMode;
    }

    public void setBridgeMode( boolean isBridgeMode )
    {
        this.isBridgeMode = isBridgeMode;
    }

    /**
     * Get the pool of addresses for the clients.
     *
     * @return the pool address to send to the client, don't use in bridging mode.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="client_pool_address"
     * sql-type="inet"
     */
    public IPaddr clientPoolAddress()
    {
        return this.clientPoolAddress;
    }

    public void clientPoolAddress( IPaddr clientPoolAddress )
    {
        this.clientPoolAddress = clientPoolAddress;
    }

    /**
     * Get the pool of netmaskes for the clients, in bridging mode this must come from
     * the pool that the interface is bridged with.
     *
     * @return the pool netmask to send to the client
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="client_pool_netmask"
     * sql-type="inet"
     */
    public IPaddr getClientPoolNetmask()
    {
        return this.clientPoolNetmask;
    }

    public void setClientPoolNetmask( IPaddr clientPoolNetmask )
    {
        this.clientPoolNetmask = clientPoolNetmask;
    }

    /* XXX Use a string or byte */
    /**
     * @return Default interface to associate VPN traffic with.
     * column="is_bridge"
     */
    public byte getClientPoolIntf()
    {
        return this.clientPoolIntf;
    }

    public void setClientPoolIntf( byte clientPoolIntf )
    {
        this.clientPoolIntf = clientPoolIntf;
    }
   
    /**
     * True if clients should be allowed to see other clients 
     * @return whether the vpn is in bridge mode.
     * column="expose_clients"
     */
    public boolean getExposeClients()
    {
        return this.exposeClients;
    }
    
    public void setExposeClients( boolean exposeClients )
    {
        this.exposeClients = exposeClients;
    }

    /**
     * True if clients should keep the connection alive with pings. (may want to hide this from the user)
     * @return keep alive
     * column="keep_alive"
     */
    public boolean getKeepAlive()
    {
        return this.keepAlive;
    }
    
    public void setKeepAlive( boolean keepAlive )
    {
        this.keepAlive = keepAlive;
    }

    /**
     * Maximum number of concurrent clients.(probably not exposed)
     * @return  max clients.
     * column="max_clients"
     */
    public int getMaxClients()
    {
        return this.maxClients;
    }
    
    public void setMaxClients( int maxClients )
    {
        this.maxClients = maxClients;
    }

    public void validate() throws Exception
    {
    }
    
    /* Certificate information */
    
    
    /**
     * @return domain.
     * @hibernate.property
     * column="domain"
     */
    public String getDomain()
    {
        return this.domain;
    }

    public void setDomain( String domain )
    {
        this.domain = domain;
    }

    /**
     * @return key size.
     * @hibernate.property
     * column="key_size"
     */
    public int getKeySize()
    {
        return this.keySize;
    }

    public void setKeySize( int keySize )
    {
        this.keySize = keySize;
    }

    /**
     * @return country.
     * @hibernate.property
     * column="country"
     */
    public String getCountry()
    {
        return this.country;
    }

    public void setCountry( String country )
    {
        this.country = country;
    }

    /**
     * @return province.
     * @hibernate.property
     * column="province"
     */
    public String getProvince()
    {
        return this.province;
    }

    public void setProvince( String province )
    {
        this.province = province;
    }

    /**
     * @return organization.
     * @hibernate.property
     * column="organization"
     */
    public String getOrganization()
    {
        return this.organization;
    }

    public void setOrganization( String organization )
    {
        this.organization = organization;
    }

    /**
     * @return organizationUnit.
     * @hibernate.property
     * column="organizationUnit"
     */
    public String getOrganizationUnit()
    {
        return organizationUnit;
    }

    public void setOrganizationUnit( String organizationUnit )
    {
        this.organizationUnit = organizationUnit;
    }

    /**
     * @return email.
     * @hibernate.property
     * column="email"
     */
    public String getEmail()
    {
        return this.email;
    }

    public void setEmail( String email )
    {
        this.email = email;
    }

    /**
     * @return true if the CA private key is on a USB key.
     * @hibernate.property
     * column="domain"
     */
    public boolean isCaKeyOnUsb()
    {
        return this.isCaKeyOnUsb;
    }

    public void setIsCaKeyOnUsb( boolean isCaKeyOnUsb )
    {
        this.isCaKeyOnUsb = isCaKeyOnUsb;
    }
}
