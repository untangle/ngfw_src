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
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Validatable;
import com.untangle.mvvm.tran.ValidateException;
import org.hibernate.annotations.Type;

/** These are settings related to remote access to the untangle. */
@Entity
@Table(name="mvvm_access_settings", schema="settings")
public class AccessSettings implements Serializable, Validatable
{
    private boolean isClean = false;

    private Long id;

    private boolean isSshEnabled;
    private boolean isInsideInsecureEnabled;
    private boolean isOutsideAccessEnabled;
    private boolean isOutsideAccessRestricted;

    private IPaddr outsideNetwork;
    private IPaddr outsideNetmask;

    private boolean isOutsideAdministrationEnabled;
    private boolean isOutsideQuarantineEnabled;
    private boolean isOutsideReportingEnabled;

    /* The following should go into address settings */

    public AccessSettings()
    {
        this.isClean = false;
    }

    @Id
    @Column(name="settings_id")
    @GeneratedValue
    Long getId()
    {
        return id;
    }

    void setId( Long id )
    {
        this.id = id;
    }

    /* Get whether or not ssh is enabled. */
    @Column(name="allow_ssh")
    public boolean getIsSshEnabled()
    {
        return this.isSshEnabled;
    }

    /* Set whether or not ssh is enabled. */
    public void setIsSshEnabled( boolean newValue )
    {
        if ( newValue != this.isSshEnabled ) this.isClean = false;
        this.isSshEnabled = newValue;
    }

    /** True if insecure access from the inside is enabled. */
    @Column(name="allow_insecure")
    public boolean getIsInsideInsecureEnabled()
    {
        return this.isInsideInsecureEnabled;
    }

    public void setIsInsideInsecureEnabled( boolean newValue )
    {
        if ( newValue != this.isInsideInsecureEnabled ) this.isClean = false;
        this.isInsideInsecureEnabled = newValue;
    }

    /** True if outside (secure) access is enabled. */
    @Column(name="allow_outside")
    public boolean getIsOutsideAccessEnabled()
    {
        return this.isOutsideAccessEnabled;
    }

    public void setIsOutsideAccessEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessEnabled ) this.isClean = false;
        this.isOutsideAccessEnabled = newValue;
    }

    /** True if outside (secure) access is restricted. */
    @Column(name="restrict_outside")
    public boolean getIsOutsideAccessRestricted()
    {
        return this.isOutsideAccessRestricted;
    }

    public void setIsOutsideAccessRestricted( boolean newValue )
    {
        if ( newValue != this.isOutsideAccessRestricted ) this.isClean = false;
        this.isOutsideAccessRestricted = newValue;
    }

    /**
     * The netmask of the network/host that is allowed to administer the box from outside
     * This is ignored if outside access is not enabled, null for just
     * one host.
     */

    /** The restricted network of machines allowed to connect to the box. */
    @Column(name="outside_network")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getOutsideNetwork()
    {
        if ( this.outsideNetwork == null ) this.outsideNetwork = NetworkUtil.DEF_OUTSIDE_NETWORK;
        return this.outsideNetwork;
    }

    public void setOutsideNetwork( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETWORK;
        if ( !IPaddr.equals( this.outsideNetwork, newValue )) this.isClean = false;
        this.outsideNetwork = newValue;
    }

    /** The restricted netmask of machines allowed to connect to the box. */
    @Column(name="outside_netmask")
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getOutsideNetmask()
    {
        if ( this.outsideNetmask == null ) this.outsideNetmask = NetworkUtil.DEF_OUTSIDE_NETMASK;
        return this.outsideNetmask;
    }

    public void setOutsideNetmask( IPaddr newValue )
    {
        if ( newValue == null ) newValue = NetworkUtil.DEF_OUTSIDE_NETMASK;
        if ( !IPaddr.equals( this.outsideNetmask, newValue )) this.isClean = false;
        this.outsideNetmask = newValue;
    }

    /** --- HTTPs access configuration.  This shouldn't be here, --- **/
    /** --- rearchitect, networking is already far too large.    --- **/
    @Column(name="allow_outside_admin")
    public boolean getIsOutsideAdministrationEnabled()
    {
        return this.isOutsideAdministrationEnabled;
    }

    public void setIsOutsideAdministrationEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideAdministrationEnabled ) this.isClean = false;
        this.isOutsideAdministrationEnabled = newValue;
    }

    @Column(name="allow_outside_quaran")
    public boolean getIsOutsideQuarantineEnabled()
    {
        return this.isOutsideQuarantineEnabled;
    }

    public void setIsOutsideQuarantineEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideQuarantineEnabled ) this.isClean = false;
        this.isOutsideQuarantineEnabled = newValue;
    }

    @Column(name="allow_outside_report")
    public boolean getIsOutsideReportingEnabled()
    {
        return this.isOutsideReportingEnabled;
    }

    public void setIsOutsideReportingEnabled( boolean newValue )
    {
        if ( newValue != this.isOutsideReportingEnabled ) this.isClean = false;
        this.isOutsideReportingEnabled = newValue;
    }

    @Transient
    public boolean isClean()
    {
        return this.isClean;
    }

    public void isClean( boolean newValue )
    {
        this.isClean = newValue;
    }

    @Transient
    public void validate() throws ValidateException
    {
        /* nothing appears to be necessary here for now */
    }
}
