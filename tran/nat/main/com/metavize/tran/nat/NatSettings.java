/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.List;

import com.metavize.mvvm.security.Tid;


import com.metavize.mvvm.tran.IPaddr;

/**
 * Settings for the Nat transform.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_SETTINGS"
 */
public class NatSettings implements java.io.Serializable
{
    private Long id;
    private Tid tid;

    /* XXX Must be updated */
    private static final long serialVersionUID = 2664348127860496780L;

    private boolean natEnabled;
    private IPaddr natInternalAddress;
    private IPaddr natInternalSubnet;

    /* Also could be considered internal address, must set with ifconfig */
    private IPaddr gateway;

    /* The Suggested nameserver, greyed out if DNS Masq is disabled. */
    private IPaddr nameserver;
    
    /* External Address */
    private IPaddr externalAddress;

    /* True if DNS Masquerading is enabled */
    private boolean isDnsMasqEnabled;

    /* Redirect rules */
    private List redirectList = null;

    /**
     * Hibernate constructor.
     */
    private NatSettings()
    {
    }

    /**
     * Real constructor
     */
    public NatSettings( Tid tid )
    {
        this.tid = tid;
    }

    /**
     * @hibernate.id
     * column="SETTINGS_ID"
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
     * unique="true"
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

    /**
     * Get the base of the internal address.
     *
     * @return is NAT is being used.
     * @hibernate.property
     * column="NAT_ENABLED"
     */
    public boolean getNatEnabled()
    {
	return natEnabled;
    }

    public void setNatEnabled( boolean enabled )
    {
	natEnabled = enabled;
    }
    
    /**
     * Get the base of the internal address.
     *
     * @return internal Address.
     * @hibernate.property
     * column="NAT_INTERNAL_ADDR"
     */
    public IPaddr getNatInternalAddress()
    {
        return natInternalAddress;
    }
    
    public void setNatInternalAddress( IPaddr addr ) 
    {
        natInternalAddress = addr;
    }

    /**
     * Get the subnet of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="NAT_INTERNAL_SUBNET"
     */
    public IPaddr getNatInternalSubnet()
    {
        return natInternalSubnet;
    }
    
    public void setNatInternalSubnet( IPaddr addr ) 
    {
        natInternalSubnet = addr;
    }

    /**
     * Get the gateway of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="INTERNAL_SUBNET"
     */
    public IPaddr getGateway()
    {
        return gateway;
    }
    
    public void setGateway( IPaddr addr ) 
    {
        gateway = addr;
    }

    /**
     * Get the subnet of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="EXTERNAL_ADDR"
     */
    public IPaddr getExternalAddress()
    {
        return externalAddress;
    }
    
    public void setExternalAddress( IPaddr addr ) 
    {
        externalAddress = addr;
    }

    /**
     * If true, exit on the first positive or negative match.  Otherwise, exit
     * on the first negative match.
     *
     * @hibernate.property
     * column="DNS_MASQ_EN"
     */
    public boolean isDnsMasqEnabled()
    {
        return isDnsMasqEnabled;
    }

    public void setDnsMasqEnabled( boolean b ) 
    {
        this.isDnsMasqEnabled = b;
    }

        /**
     * Pattern rules.
     *
     * @return the list of Patterns
     * @hibernate.list
     * cascade="all-delete-orphan"
     * @hibernate.collection-key
     * column="SETTINGS_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-one-to-many
     * class="com.metavize..RedirectRule"
     */
    public List getRedirectList() 
    {
        return redirectList;
    }
    
    public void setRedirectList( List s ) 
    { 
        redirectList = s;
    }
}
