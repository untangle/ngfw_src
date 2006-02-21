/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.networking;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import java.io.Serializable;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;
import com.metavize.mvvm.tran.ValidateException;

/**
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_settings"
 */
public class NetworkSpacesSettingsImpl implements NetworkSpacesSettings, Serializable, Validatable
{
    private Long id;

    private SetupState setupState = SetupState.BASIC;
    private boolean isEnabled = false;

    private List interfaceList = new LinkedList();
    private List networkSpaceList = new LinkedList();
    private List routingTable = new LinkedList();
    private List redirectList = new LinkedList();
    
    private IPaddr defaultRoute = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns2 = NetworkUtil.EMPTY_IPADDR;

    /* This is a data class */
    public NetworkSpacesSettingsImpl()
    {
    }

    /**
     * @hibernate.id
     * column="settings_id"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Get whether or not the settings are enabled..
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="is_enabled"
     */
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }

    /**
     * The current setup state for this tranform.  (deprecated, unconfigured, basic, advanced).
     * @return The current setup state for this transform.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.SetupStateUserType"
     * @hibernate.column
     * name="setup_state"
     */
    public SetupState getSetupState()
    {
        return this.setupState;
    }

    public void setSetupState( SetupState newValue )
    {
        this.setupState = newValue;
    }

    /**
     * The list of interfaces.
     *
     * @return the list of interfaces
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.Interface"
     */
    public List getInterfaceList()
    {
        if ( this.interfaceList == null ) this.interfaceList = new LinkedList();
        return this.interfaceList;
    }
    
    public void setInterfaceList( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.interfaceList = newValue;
    }

    /**
     * The list of network spaces.
     *
     * @return the list of network spaces
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.NetworkSpace"
     */
    public List getNetworkSpaceList()
    {
        if ( this.networkSpaceList == null ) this.networkSpaceList = new LinkedList();
        return this.networkSpaceList;
    }
    
    public void setNetworkSpaceList( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.networkSpaceList = newValue;
    }

    /**
     * The routing table.
     *
     * @return the routing table
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="settings_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-one-to-many
     * class="com.metavize.mvvm.networking.Route"
     */
    public List getRoutingTable()
    {
        if ( this.routingTable == null ) this.routingTable = new LinkedList();
        return this.routingTable;
    }

    public void setRoutingTable( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.routingTable = newValue;
    }

    /**
     * Default route for the box.
     *
     * @return the default route for the box.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="default_route"
     * sql-type="inet"
     */
    public IPaddr getDefaultRoute()
    {
        if ( this.defaultRoute == null ) this.defaultRoute = NetworkUtil.EMPTY_IPADDR;
        return this.defaultRoute;
    }
    
    public void setDefaultRoute( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.defaultRoute = newValue;
    }

    /**
     * List of the redirect rules, and yes this has to be many-to-many since these are shared with
     * NatSettings.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all"
     * table="mvvm_redirects"
     * @hibernate.collection-key
     * column="setting_id"
     * @hibernate.collection-index
     * column="position"
     * @hibernate.collection-many-to-many
     * class="com.metavize.mvvm.networking.RedirectRule"
     * column="rule_id"
     */
    public List getRedirectList()
    {
        if ( this.redirectList == null ) this.redirectList = new LinkedList();
        return this.redirectList;
    }

    public void setRedirectList( List newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.redirectList = newValue;
    }
    
    /**
     * Address of the primary dns server
     *
     * @return Address of the primary dns server
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dns_1"
     * sql-type="inet"
     */
    public IPaddr getDns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }

    public void setDns1( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }
    
    /**
     * Address of the secondary dns server
     *
     * @return Address of the secondary dns server
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="dns_2"
     * sql-type="inet"
     */
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }

    public void setDns2( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /* Return true if there is a secondary DNS entry */
    public boolean hasDns2()
    {
        return (( this.dns2 == null ) || this.dns2.isEmpty());
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        
        sb.append( "Network Settings\n" );
        sb.append( "setup-state: " + getSetupState() + " isEnabled: " + getIsEnabled());
        
        sb.append( "\nInterfaces:\n" );
        for ( Iterator iter = getInterfaceList().iterator() ; iter.hasNext() ; ) {
            Interface intf = (Interface)iter.next();
            sb.append( intf + "\n" );
        }
        
        sb.append( "Network Spaces:\n" );
        for ( Iterator iter = getNetworkSpaceList().iterator() ; iter.hasNext() ; ) {
            NetworkSpace space = (NetworkSpace)iter.next();
            sb.append( space + "\n" );
        }
        
        sb.append( "Routing table:\n" );
        
        for ( Iterator iter = getRoutingTable().iterator() ; iter.hasNext() ; ) {
            Route route = (Route)iter.next();
            sb.append( route + "\n" );
        }
        
        sb.append( "dns1:     " + getDns1());
        sb.append( "\ndns2:     " + getDns2());
        sb.append( "\ngateway:  " + getDefaultRoute());

        return sb.toString();
    }

    public void validate() throws ValidateException
    {
        NetworkUtil.getInstance().validate( this );
    }
}