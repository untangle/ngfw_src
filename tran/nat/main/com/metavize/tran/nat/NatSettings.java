/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterSettings.java,v 1.8 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.tran.protofilter;

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
    /* XXX Must be updated */
    private static final long serialVersionUID = 2664348127860496780L;

    private IPaddr internalAddress;
    private IPaddr internalSubnet;

    /* Also could be considered internal address, must set with ifconfig */
    private IPaddr gateway;

    
    /* Primary */
    private IPaddr nameserver1;

    /* Secondary */
    private IPaddr nameserver2;
    
    /* Tertiary */
    private IPaddr nameserver3;
    
    /* External Address */
    private IPaddr externalAddress;

    /* True if DNS Masquerading is enabled */
    private IPaddr isDnsMasqEnabled;

    /**
     * Hibernate constructor.
     */
    private NatSettings() {}

    /**
     * Real constructor
     */
    public NatSettings(Tid tid)
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
     * @return internal Address.
     * @hibernate.property
     * column="INTERNAL_ADDR"
     */
    public IPaddr getInternalAddress()
    {
        return internalAddress;
    }
    
    public void setInternalAddress( IPaddr addr ) 
    {
        internatlAddress = addr;
    }

    /**
     * Get the subnet of the internal addresses.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="INTERNAL_SUBNET"
     */
    public IPaddr getInternalSubnet()
    {
        return internalSubnet;
    }
    
    public void setInternalSubnet( IPaddr addr ) 
    {
        internatlSubnet = addr;
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
     * Get the primary nameserver to use for the internal network.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="NAMESERVER_A"
     */
    public IPaddr getNameserver1()
    {
        return nameserver1;
    }
    
    public void setNameserver1( IPaddr addr ) 
    {
        nameserver1 = addr;
    }


    /**
     * Get the secondary nameserver to use for the internal network.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="NAMESERVER_B"
     */
    public IPaddr getNameserver2()
    {
        return nameserver2;
    }
    
    public void setNameserver2( IPaddr addr ) 
    {
        nameserver2 = addr;
    }

    /**
     * Get the tertiary nameserver to use for the internal network.
     *
     * @return internal subnet.
     * @hibernate.property
     * column="NAMESERVER_B"
     */
    public IPaddr getNameserver3()
    {
        return nameserver3;
    }
    
    public void setNameserver3( IPaddr addr ) 
    {
        nameserver3 = addr;
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
}
