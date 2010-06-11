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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.IndexColumn;
import org.hibernate.annotations.Type;

import com.untangle.node.util.UvmUtil;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

/**
 * Settings for the network spaces.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_network_settings", schema="settings")
@SuppressWarnings("serial")
public class NetworkSpacesSettingsImpl implements NetworkSpacesSettings, Serializable, Validatable
{
    private Long id;

    /** The current setup state */
    private SetupState setupState = SetupState.BASIC;

    /* Whether or not network spaces are enabled */
    private boolean isEnabled = false;

    /* Whether or not the box has gone through the setup wizard */
    private boolean hasCompletedSetup = true;

    /* The list of interfaces */
    private List<Interface> interfaceList = new LinkedList<Interface>();

    /* The current list of network spaces. */
    private List<NetworkSpace> networkSpaceList = new LinkedList();

    /* The routing table */
    private List<Route> routingTable = new LinkedList<Route>();

    /* The list of redirects */
    private List<RedirectRule> redirectList = new LinkedList<RedirectRule>();

    /* Default Route for the untangle */
    private IPaddr defaultRoute = NetworkUtil.EMPTY_IPADDR;

    /* Primary DNS server */
    private IPaddr dns1 = NetworkUtil.EMPTY_IPADDR;

    /* Secondary DNS server */
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
     * Retrieve whether or not the advanced settings are enabled.
     *
     * @return True iff advanced settings are enabled.
     */
    @Column(name="is_enabled", nullable=false)
    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    /**
     * Set whether or not network spaces are enabled.
     *
     * @param newValue True iff network spaces are enabled.
     */
    public void setIsEnabled( boolean newValue )
    {
        this.isEnabled = newValue;
    }

    /**
     * Retrieve the setup state of network spaces.
     * (deprecated, unconfigured, basic, advanced).
     *
     * @return The current setup state.
     */
    @Column(name="setup_state")
    @Type(type="com.untangle.uvm.networking.SetupStateUserType")
    public SetupState getSetupState()
    {
        return this.setupState;
    }

    /**
     * Retrieve whether or not the Untangle Platform has completed the
     * setup wizard.
     *
     * @return True if the untangle has completed setup.
     */
    public void setSetupState( SetupState newValue )
    {
        this.setupState = newValue;
    }

    /**
     * Retrieve a list of interfaces
     *
     * @return A list of the interfaces.
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
        return UvmUtil.eliminateNulls(this.interfaceList);
    }

    /**
     * Set the list of interfaces
     *
     * @param newValue A list of the interfaces.
     */
    public void setInterfaceList( List<Interface> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList();
        this.interfaceList = newValue;
    }


    /**
     * Retrieve the list of network spaces for the box.
     *
     * @return The lists of network spaces.
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
        return UvmUtil.eliminateNulls(this.networkSpaceList);
    }


    /**
     * Set The list of network spaces for the box.
     *
     * @param newValue The lists of network spaces.
     */
    public void setNetworkSpaceList( List<NetworkSpace> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<NetworkSpace>();
        this.networkSpaceList = newValue;
    }

    /**
     * Retrieve the routing table for the box.
     *
     * @return The routing table.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinColumn(name="settings_id")
    @IndexColumn(name="position")
    public List<Route> getRoutingTable()
    {
        if ( this.routingTable == null ) this.routingTable = new LinkedList<Route>();
        return UvmUtil.eliminateNulls(this.routingTable);
    }

    /**
     * Set the routing table for the box.
     *
     * @param newValue The routing table.
     */
    public void setRoutingTable( List<Route> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<Route>();
        this.routingTable = newValue;
    }

    /**
     * Get the IP address of the default route.
     *
     * @return The current default route for the untangle.
     */
    @Column(name="default_route")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDefaultRoute()
    {
        if ( this.defaultRoute == null ) this.defaultRoute = NetworkUtil.EMPTY_IPADDR;
        return this.defaultRoute;
    }

    /**
     * Set the IP address of the default route.
     *
     * @param newValue The new default route for the untangle.
     */
    public void setDefaultRoute( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.defaultRoute = newValue;
    }

    /**
     * Retrieve the list of redirects for the box.
     *
     * This has to be many-to-many since these are shared with
     * NatSettings.
     *
     * @return Get the current list of redirects.
     */
    @OneToMany(fetch=FetchType.EAGER)
    @Cascade({ org.hibernate.annotations.CascadeType.ALL,
                   org.hibernate.annotations.CascadeType.DELETE_ORPHAN })
    @JoinTable(name="u_redirects",
               joinColumns=@JoinColumn(name="setting_id"),
               inverseJoinColumns=@JoinColumn(name="rule_id"))
    @IndexColumn(name="position")
    public List<RedirectRule> getRedirectList()
    {
        if ( this.redirectList == null ) this.redirectList = new LinkedList<RedirectRule>();
        return UvmUtil.eliminateNulls(this.redirectList);
    }

    /**
     * Set the list of redirects for the box
     *
     * @param newValue The new list of redirects.
     */
    public void setRedirectList( List<RedirectRule> newValue )
    {
        if ( newValue == null ) newValue = new LinkedList<RedirectRule>();
        this.redirectList = newValue;
    }

    /**
     * Get IP address of the primary dns server, may be empty (dhcp is
     * enabled)
     *
     * @return The primay DNS server.
     */
    @Column(name="dns_1")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDns1()
    {
        if ( this.dns1 == null ) this.dns1 = NetworkUtil.EMPTY_IPADDR;
        return this.dns1;
    }

    /**
     * Set IP address of the primary dns server, may be empty (dhcp is
     * enabled)
     *
     * @param newValue The primay DNS server.
     */
    public void setDns1( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns1 = newValue;
    }

    /**
     * IP address of the secondary DNS server, may be empty
     *
     * @return The IP address of the secondary DNS server.
     */
    @Column(name="dns_2")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getDns2()
    {
        if ( this.dns2 == null ) this.dns2 = NetworkUtil.EMPTY_IPADDR;
        return this.dns2;
    }


    /**
     * Set the IP address of the secondary DNS server, may be empty.
     *
     * @param newValue The IP address of the secondary DNS server.
     */
    public void setDns2( IPaddr newValue )
    {
        if ( newValue == null || newValue.isEmpty()) newValue = NetworkUtil.EMPTY_IPADDR;
        this.dns2 = newValue;
    }

    /**
     * Check if the secondary DNS entry is empty.
     *
     * @return True iff the is a secondary DNS entry
     */
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

    /**
     * Set whether this Untangle Server has completed setup.
     *
     * @param newValue whether this Untangle Server has completed
     * setup.
     */
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

    /**
     * Validate that the settings are free of errors.
     *
     * @exception ValidateException If the settings contain errors.
     */
    public void validate() throws ValidateException
    {
        NetworkUtil.getInstance().validate( this );
    }
}
