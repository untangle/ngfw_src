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

package com.untangle.mvvm.networking;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

/**
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="mvvm_network_settings", schema="settings")
public class NetworkSpacesSettingsImpl implements NetworkSpacesSettings, Serializable, Validatable
{
    private Long id;

    private SetupState setupState = SetupState.BASIC;
    private boolean isEnabled = false;
    private boolean hasCompletedSetup = true;

    private List<Interface> interfaceList = new LinkedList<Interface>();
    private List<NetworkSpace> networkSpaceList = new LinkedList();
    private List<Route> routingTable = new LinkedList<Route>();
    private List<RedirectRule> redirectList = new LinkedList<RedirectRule>();

    private IPaddr defaultRoute = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;
    private IPaddr dns2 = NetworkUtil.EMPTY_IPADDR;

    /* This is a data class */
    public NetworkSpacesSettingsImpl()
    {
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
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
     */
    @Column(name="is_enabled", nullable=false)
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
     */
    @Column(name="setup_state")
    @Type(type="com.untangle.mvvm.networking.SetupStateUserType")
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
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<Interface> getInterfaceList()
    {
        if ( this.interfaceList == null ) {
            this.interfaceList = new LinkedList<Interface>();
        }
        return this.interfaceList;
    }

    public void setInterfaceList( List<Interface> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.interfaceList = newValue;
    }

    /**
     * The list of network spaces.
     *
     * @return the list of network spaces
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<NetworkSpace> getNetworkSpaceList()
    {
        if ( this.networkSpaceList == null ) {
            this.networkSpaceList = new LinkedList<NetworkSpace>();
        }
        return this.networkSpaceList;
    }

    public void setNetworkSpaceList( List<NetworkSpace> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<NetworkSpace>();
        this.networkSpaceList = newValue;
    }

    /**
     * The routing table.
     *
     * @return the routing table
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<Route> getRoutingTable()
    {
        if ( this.routingTable == null ) this.routingTable = new LinkedList<Route>();
        return this.routingTable;
    }

    public void setRoutingTable( List<Route> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<Route>();
        this.routingTable = newValue;
    }

    /**
     * Default route for the box.
     *
     * @return the default route for the box.
     */
    @Column(name="default_route")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getDefaultRoute()
    {
        if ( this.defaultRoute == null ) this.defaultRoute = NetworkUtil.EMPTY_IPADDR;
        return this.defaultRoute;
    }

    public void setDefaultRoute( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.defaultRoute = newValue;
    }

    /**
     * List of the redirect rules, and yes this has to be many-to-many since these are shared with
     * NatSettings.
     *
     * @return the list of the redirect rules.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="mvvm_redirects",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<RedirectRule> getRedirectList()
    {
        if ( this.redirectList == null ) this.redirectList = new LinkedList<RedirectRule>();
        return this.redirectList;
    }

    public void setRedirectList( List<RedirectRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<RedirectRule>();
        this.redirectList = newValue;
    }

    /**
     * Address of the primary dns server
     *
     * @return Address of the primary dns server
     */
    @Column(name="dns_1")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getDns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }

    public void setDns1( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }

    /**
     * Address of the secondary dns server
     *
     * @return Address of the secondary dns server
     */
    @Column(name="dns_2")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }

    public void setDns2( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /* Return true if there is a secondary DNS entry */
    public boolean hasDns2()
    {
        return (( this.dns2 == null ) || this.dns2.isEmpty());
    }

    /**
     * Property indicating whether the user has completed the setup wizard.
     * Only false if the wizard has never finished the wizard.  If the user
     * ever hits save from inside of the standard gui, this is set to true,
     * and should never return to false;
     *
     * @return true if the user never finished the wizard.
     */
    @Column(name="completed_setup", nullable=false)
    public boolean getHasCompletedSetup()
    {
        return this.hasCompletedSetup;
    }

    public void setHasCompletedSetup( boolean newValue )
    {
        this.hasCompletedSetup = newValue;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Network Settings\n" );
        sb.append( "setup-state: " + getSetupState() + " isEnabled: " + getIsEnabled());
        sb.append( " completed-setup: " + getHasCompletedSetup());

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
