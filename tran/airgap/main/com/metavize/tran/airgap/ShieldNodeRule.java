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

package com.metavize.tran.airgap;

import java.net.UnknownHostException;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.shield.ShieldNodeSettings;

import com.metavize.mvvm.tran.ParseException;


/**
 * Rule for shield node settings.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_AIRGAP_SHIELD_NODE_RULE"
 */
public class ShieldNodeRule extends Rule implements ShieldNodeSettings
{
    private static final long serialVersionUID = -6393845685680833335L;

    /* ip address this is configuring */
    private IPaddr address;
    
    /* Netmask that this rule applies to */
    private IPaddr netmask;
    
    /* divider for this rule (between0 and whatever, not inclusive) */
    private float divider;

    /* Hibernate constructor */
    public ShieldNodeRule()
    {
    }
    
    public ShieldNodeRule( boolean isLive, IPaddr address, IPaddr netmask, float divider, String category, 
                           String description )
    {
        setLive( isLive );
        setCategory( category );
        setDescription( description );
        this.address = address;
        this.netmask = netmask;
        this.divider = divider;
    }
    
    /**
     * Node being modified.
     *
     * @return the node to modify
     *
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    public void setAddress( String addressString ) throws UnknownHostException, ParseException
    {
        setAddress( IPaddr.parse( addressString ));
    }

    public String getAddressString()
    {
        if ( address == null || address.isEmpty()) return "";
            
        return address.toString();
    }
   

    /**
     * Netmask onto which to apply this configuration.
     *
     * @return the netmask
     *
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="NETMASK"
     * sql-type="inet"
     */
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask( IPaddr netmask )
    {
        this.netmask = netmask;
    }

    public void setNetmask( String netmaskString ) throws UnknownHostException, ParseException
    {
        setNetmask( IPaddr.parse( netmaskString ));
    }

    public String getNetmaskString()
    {
        if ( netmask == null || netmask.isEmpty()) return "";
            
        return netmask.toString();
    }

    /**
     * Divider up to which this applies, 0 is the highest and not recommended.
     *
     * @return the port to redirect to.
     * @hibernate.property
     * column="DIVIDER"
     */
    public float getDivider()
    {
        return divider;
    }

    public void setDivider( float divider )
    {
        this.divider = divider;
    }


    
    
    
}
