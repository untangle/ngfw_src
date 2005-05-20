/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MimeTypeRule.java 229 2005-04-07 22:25:00Z amread $
 */

package com.metavize.tran.nat;

import java.util.Date;

import com.metavize.mvvm.tran.Rule;

import com.metavize.mvvm.tran.IPNullAddr;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.MACAddress;


/**
 * Rule for storing DNS static hosts.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="DNS_STATIC_HOST_RULE"
 */
public class DnsStaticHostRule extends Rule
{

    private String     hostname        = "";
    private IPaddr     staticAddress   = null;//IPAddr.getNullAddr();

    // Constructors 
    /**
     * Hibernate constructor 
     */
    public DnsStaticHostRule()
    {
    }

    public DnsStaticHostRule( String hostname, IPaddr staticAddress )
    {
        this.hostname       = hostname;
        this.staticAddress  = staticAddress;
    }

    
    /**
     * Host name
     *
     * @return the desired/assigned host name for this machine.
     * @hibernate.property
     * @hibernate.column
     * name="HOSTNAME"
     */
    public String getHostname()
    {
        if ( hostname == null )
            return "";

        return hostname;
    }

    public void setHostname( String hostname )
    {
        this.hostname = hostname;
    }


    /**
     * Get static IP address
     *
     * @return desired static address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="STATIC_ADDRESS"
     * sql-type="inet"
     */
    public IPaddr getStaticAddress()
    {
        if ( this.staticAddress == null ) return null;//( this.staticAddress = IPNullAddr.getNullAddr());

        return this.staticAddress;
    }
    
    public void setStaticAddress( IPaddr staticAddress ) 
    {
        this.staticAddress = staticAddress;
    }
    
}
