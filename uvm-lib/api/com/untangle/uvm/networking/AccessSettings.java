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

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

/**
 * These are settings related to limitting and granting remote access
 * to the untangle.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="u_access_settings", schema="settings")
@SuppressWarnings("serial")
public class AccessSettings implements Serializable, Validatable
{

    /* boolean which can be used by the untangle to determine if the
     * object returned by a user interface has been modified. */
    private boolean isClean = false;

    private Long id;

    /** True iff remote untangle support is enabled */
    private boolean isSupportEnabled;

    /* True iff internal HTTP access is enabled */
    private boolean isInsideInsecureEnabled;

    /* This is the port that blockpages are rendered on. */
    private int blockPagePort = 80;

    /* True iff Access to the external HTTPs port is allowed */
    private boolean isOutsideAccessEnabled;

    /* True iff access to the external HTTPs port is restriced with
     * <code>outsideNetwork / outsideNetmask</code>. */
    private boolean isOutsideAccessRestricted;

    /* When <code>isOutsideAccessRestricted</code> is true this is the
     * network that access is restricted to. */
    private IPaddr outsideNetwork;

    /* When <code>isOutsideAccessRestricted</code> is true this is the
     * netmask that access is restricted to. */
    private IPaddr outsideNetmask;

    /* True iff administration is allowed from outside. */
    private boolean isOutsideAdministrationEnabled;

    /* True iff quarantine is allowed from outside. */
    private boolean isOutsideQuarantineEnabled;

    /* True iff reporting is allowed from outside. */
    private boolean isOutsideReportingEnabled;

    public AccessSettings()
    {
        this.isClean = false;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    public Long getId()
    {
        return id;
    }

    public void setId( Long id )
    {
        this.id = id;
    }

    /**
     * Get whether or not remote untangle support is enabled.
     *
     * @return true iff remote untangle support is enabled.
     */
    @Column(name="allow_ssh")
    public boolean getIsSupportEnabled()
    {
        return this.isSupportEnabled;
    }


    /**
     * Set whether or not remote untangle support is enabled.
     *
     * @param newValue true iff remote untangle support is enabled.
     */
    public void setIsSupportEnabled( boolean newValue )
    {
        if ( newValue != this.isSupportEnabled ) this.isClean = false;
        this.isSupportEnabled = newValue;
    }

    /**
     * Get whether or not local insecure access is enabled.
     *
     * @return true iff remote untangle support is enabled.
     */
    @Column(name="allow_insecure")
    public boolean getIsInsideInsecureEnabled()
    {
        return this.isInsideInsecureEnabled;
    }

    /**
     * Set whether or not local insecure access is enabled.
     *
     * @param newValue true iff remote untangle support is enabled.
     */
    public void setIsInsideInsecureEnabled( boolean newValue )
    {
        if ( newValue != this.isInsideInsecureEnabled ) this.isClean = false;
        this.isInsideInsecureEnabled = newValue;
    }


    /**
     * Get the port to render blockpage on.
     *
     * @return the port blockpages are rendered on
     */
    @Column(name="block_page_port")
    public int getBlockPagePort()
    {
        return this.blockPagePort;
    }

    /**
     * Set the blockPage port.
     *
     * @param newValue the new port to put the blockPage on.
     */
    public void setBlockPagePort( int newValue )
    {
        if ( newValue != this.blockPagePort ) this.isClean = false;
        this.blockPagePort = newValue;
    }

    /**
     * Get whether or not external remote access is enabled.  This is
     * is no longer used, as access to the external https port is
     * automatically opened whenever a service that requires it is
     * enabled.
     *
     * @return true iff external access is enabled.
     */
    @Column(name="allow_outside")
    public boolean getIsOutsideAccessEnabled()
    {
        return this.isOutsideAccessEnabled;
    }

    /**
     * Set external remote access
     * 
     * @param True iff external access is allowed
     */
    public void setIsOutsideAccessEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessEnabled ) this.isClean = false;
        this.isOutsideAccessEnabled = newValue;
    }

    /**
     * Retrieve whether or not outside access is restricted.
     *
     * @return True iff outside access is restricted.
     */
    @Column(name="restrict_outside")
    public boolean getIsOutsideAccessRestricted()
    {
        return this.isOutsideAccessRestricted;
    }

    /**
     * Set whether or not outside access is restricted.
     *
     * @param newValue True iff outside access is restricted.
     */
    public void setIsOutsideAccessRestricted( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessRestricted ) this.isClean = false;
        this.isOutsideAccessRestricted = newValue;
    }

    /**
     * The netmask of the network/host that is allowed to administer
     * the box from outside.  This is ignored if restrict outside
     * access is not enabled.
     *
     * @return The network that is allowed to administer the box from
     * the internet.
     */
    @Column(name="outside_network")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getOutsideNetwork()
    {
        if ( this.outsideNetwork == null ) this.outsideNetwork = NetworkUtil.DEF_OUTSIDE_NETWORK;
        return this.outsideNetwork;
    }

    /**
     * Set the network of the network/host that is allowed to
     * administer the box from outside.  This is ignored if restrict
     * outside access is not enabled.
     *
     * @param newValue The network that is is allowed to administer the box from
     * the internet.
     */
    public void setOutsideNetwork( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETWORK;
        if ( !IPaddr.equals( this.outsideNetwork, newValue )) this.isClean = false;
        this.outsideNetwork = newValue;
    }

    /**
     * The netmask of the network/host that is allowed to administer
     * the box from outside.  This is ignored if restrict outside
     * access is not enabled.
     *
     * @return The netmask that is allowed to administer the box from
     * the internet.
     */
    @Column(name="outside_netmask")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getOutsideNetmask()
    {
        if ( this.outsideNetmask == null ) this.outsideNetmask = NetworkUtil.DEF_OUTSIDE_NETMASK;
        return this.outsideNetmask;
    }

    /**
     * Set the netmask of the network/host that is allowed to
     * administer the box from outside.  This is ignored if restrict
     * outside access is not enabled.
     *
     * @param newValue The netmask for the network that is is allowed to administer the box from
     * the internet.
     */
    public void setOutsideNetmask( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETMASK;
        if ( !IPaddr.equals( this.outsideNetmask, newValue )) this.isClean = false;
        this.outsideNetmask = newValue;
    }

    /**
     * Retrieve whether or not administration from the internet is allowed.
     *
     * @return True iff able to administer from the internet.
     */
    @Column(name="allow_outside_admin")
    public boolean getIsOutsideAdministrationEnabled()
    {
        return this.isOutsideAdministrationEnabled;
    }

    /**
     * Set whether or not administration from the internet is allowed.
     *
     * @param newValue True iff able to administer from the internet.
     */
    public void setIsOutsideAdministrationEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAdministrationEnabled ) this.isClean = false;
        this.isOutsideAdministrationEnabled = newValue;
    }

    /**
     * Retrieve whether or not to access the user quarantine from the
     * internet is allowed.
     *
     * @return True iff able to access user quarantines from the
     * internet.
     */
    @Column(name="allow_outside_quaran")
    public boolean getIsOutsideQuarantineEnabled()
    {
        return this.isOutsideQuarantineEnabled;
    }

    /**
     * Set whether or not to access the user quarantine from the
     * internet is allowed.
     *
     * @param newValue True iff able to access user quarantines from the
     * internet.
     */
    public void setIsOutsideQuarantineEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideQuarantineEnabled ) this.isClean = false;
        this.isOutsideQuarantineEnabled = newValue;
    }

    /**
     * Retrieve whether access is allowed to reports from the internet.
     *
     * @return True iff able to access reports from the internet.
     */
    @Column(name="allow_outside_report")
    public boolean getIsOutsideReportingEnabled()
    {
        return this.isOutsideReportingEnabled;
    }

    /**
     * Set whether access is allowed to reports from the internet.
     *
     * @param newValue True iff able to access reports from the
     * internet.
     */
    public void setIsOutsideReportingEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideReportingEnabled ) this.isClean = false;
        this.isOutsideReportingEnabled = newValue;
    }

    /**
     * Return true iff the settings haven't been modified since the
     * last time <code>isClean( true )</code> was called.
     */
    @Transient
    public boolean isClean()
    {
        return this.isClean;
    }

    /**
     * Clear or set the isClean flag.
     *
     * @param newValue The new value for the isClean flag.
     */
    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    /**
     * Validate that the settings are free of errors.
     */
    @Transient
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}
