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
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

/**
 * Settings for the open vpn transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_settings"
 */
public class VpnSettings implements Serializable, Validatable
{
    // XXX SERIALVER private static final long serialVersionUID = 1032713361795879615L;

    static private final int KEY_SIZE_ENUMERATION[] = new int[] 
        { 
            1024, 1152, 1280, 1408,
            1536, 1664, 1792, 1920,
            2048
        };

    static private final int KEY_SIZE_DEFAULT       = KEY_SIZE_ENUMERATION[4];
    
    private Long id;
    private Tid tid;

    private boolean isBridgeMode = false;
    private boolean isEdgeGuardClient = false;

    /* The virtual address of the vpn server */
    private IPaddr  serverAddress;
    
    /* List of addresses that should be visible to the VPN */
    private List    exportedAddressList;

    private boolean keepAlive;

    private boolean exposeClients;

    private int maxClients = 100;

    private List groupList;
    private List clientList;
    private List siteList;

    /* Certificate information */
    private String  domain = "";
    private int     keySize = KEY_SIZE_DEFAULT;
    private String  country = "";
    private String  locality = "";
    private String  province = "";
    private String  organization = "";
    private String  organizationUnit = "";
    private String  email;
    private boolean caKeyOnUsb;
    
    public VpnSettings() 
    {
    }

    public VpnSettings( Tid tid )
    {
        this.tid = tid;
    }

    public void validate() throws Exception
    {
        /* XXXXXXXXXXX */
        
        /* That is the only setting required for edgeguard client */
        if ( isEdgeGuardClient ) return;
        
        if (( groupList == null ) || ( groupList.size() == 0 )) throw new ValidateException( "No groups" );
        
        GroupList validateGroupList = new GroupList( this.groupList );
        validateGroupList.validate();
        
        ClientList validateClientList = new ClientList( this.clientList );
        validateClientList.validate();
        
        SiteList validateSiteList = new SiteList( this.siteList );
        validateSiteList.validate( validateClientList );

        ExportList validateExportList = new ExportList( this.exportedAddressList );
        validateExportList.validate();
        
        /* XXX Check for overlap in all of the settings */        
    }

    /* Typically private, but package access so the ID can be reused */
    /**
     * @hibernate.id
     * column="ID"
     * generator-class="native"
     */
    Long getId()
    {
        return id;
    }

    void setId( Long id )
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
     * @hibernate.property
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
     * @return whether this is an openvpn of another edgeguard client.
     * @hibernate.property
     * column="is_edgeguard_client"
     */
    public boolean getIsEdgeGuardClient()
    {
        return this.isEdgeGuardClient;
    }

    public void setIsEdgeGuardClient( boolean isEdgeGuardClient )
    {
        this.isEdgeGuardClient = isEdgeGuardClient;
    }

    /**
     * The list of VPN groups associated with this vpn configuration.
     * ??? This may just be infrastructure for down the line, and the current GUI
     * may only support one address group.
     *
     * @return the list of vpn address groups.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.openvpn.VpnGroup"
     */
    public List getGroupList()
    {
        if ( this.groupList == null ) this.groupList = new LinkedList();

        return this.groupList;
    }
    public void setGroupList( List groupList )
    {
        this.groupList = groupList;
    }

    /**
     * The list of VPN clients.
     *
     * @return the list of Patterns
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.openvpn.VpnClient"
     */
    public List getClientList()
    {
        if ( this.clientList == null ) this.clientList = new LinkedList();
        
        return this.clientList;
    }
    
    public void setClientList( List clientList )
    {
        this.clientList = clientList;
    }

    /**
     * The list of VPN clients.
     *
     * @return the list of Patterns
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.openvpn.VpnSite"
     */
    public List getSiteList()
    {
        if ( this.siteList == null ) this.siteList = new LinkedList();
        
        return this.siteList;
    }
    
    public void setSiteList( List siteList )
    {
        this.siteList = siteList;
    }

    /**
     * @return a new list containing all of the clients and the sites. A VpnSite is a subclass of a 
     * VpnClient.
     */
    public List getCompleteClientList()
    {
        /* ??? Is there a better way to do this */
        List completeList = new LinkedList();
        completeList.addAll( getClientList());
        completeList.addAll( getSiteList());
        return completeList;
    }

    /**
     * Static address for the openvpn server.
     *
     * @return virtual address of the open vpn server.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="server_address"
     * sql-type="inet"
     */
    public IPaddr getServerAddress()
    {
        return this.serverAddress;
    }

    public void setServerAddress( IPaddr serverAddress )
    {
        this.serverAddress = serverAddress;
    }

    /**
     * The list of exported networks for this site.
     *
     * @return the list of exported networks for this site.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.tran.openvpn.ServerSiteNetwork"
     */
    public List getExportedAddressList()
    {
        if ( this.exportedAddressList == null ) this.exportedAddressList = new LinkedList();

        return this.exportedAddressList;
    }
    public void setExportedAddressList( List exportedAddressList )
    {
        this.exportedAddressList = exportedAddressList;
    }
   
    /**
     * True if clients should be allowed to see other clients 
     * @return whether the vpn is in bridge mode.
     * @hibernate.property
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
     * @hibernate.property
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
     * @return Maximum number of concurrent clients.(probably not exposed)
     * @hibernate.property
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
    
    static public int[] getKeySizeEnumeration()
    {
        return KEY_SIZE_ENUMERATION;
    }

    static public int getKeySizeDefault()
    {
        return KEY_SIZE_DEFAULT;
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
     * @return locality(city).
     * @hibernate.property
     * column="locality"
     */
    public String getLocality()
    {
        return this.locality;
    }

    public void setLocality( String locality )
    {
        this.locality = locality;
    }

    /**
     * @return organization.
     * @hibernate.property
     * column="org"
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
     * column="org_unit"
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
     * @return true if the settings have been configured
     */
    boolean isConfigured()
    {
        if ( isEdgeGuardClient ) return true;
        return ( !( this.organizationUnit == null ) && ( this.organizationUnit.length() > 0 ));
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
     * column="is_ca_on_usb"
     */
    public boolean getCaKeyOnUsb()
    {
        return this.caKeyOnUsb;
    }

    public void setCaKeyOnUsb( boolean caKeyOnUsb )
    {
        this.caKeyOnUsb = caKeyOnUsb;
    }
}
