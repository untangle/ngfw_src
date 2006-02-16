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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.tran.Rule;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostNameList;

/**
 * Rule for storing DNS static hosts.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="dns_static_host_rule"
 */
public class DnsStaticHostRule extends Rule
{
    private HostNameList hostNameList  = HostNameList.getEmptyHostNameList();
    private IPaddr       staticAddress = null;

    // Constructors 
    /**
     * Hibernate constructor 
     */
    public DnsStaticHostRule()
    {
    }

    public DnsStaticHostRule( HostNameList hostNameList, IPaddr staticAddress )
    {
        this.hostNameList  = hostNameList;
        this.staticAddress = staticAddress;
    }

    
    /**
     * Host name list
     *
     * @return the host name list.
     * @hibernate.property
     * type="com.metavize.mvvm.type.HostNameListUserType"
     * @hibernate.column
     * name="hostname_list"
     */
    public HostNameList getHostNameList()
    {
        if ( hostNameList == null )
            hostNameList = HostNameList.getEmptyHostNameList();

        return hostNameList;
    }

    public void setHostNameList( HostNameList hostNameList )
    {
        this.hostNameList = hostNameList;
    }

    /**
     * Get static IP address
     *
     * @return desired static address.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="static_address"
     * sql-type="inet"
     */
    public IPaddr getStaticAddress()
    {
        return this.staticAddress;
    }
    
    public void setStaticAddress( IPaddr staticAddress ) 
    {
        this.staticAddress = staticAddress;
    }
}
