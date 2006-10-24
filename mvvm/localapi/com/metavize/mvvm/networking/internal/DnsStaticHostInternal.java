/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking.internal;

import java.util.Date;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.HostNameList;

import com.metavize.mvvm.tran.firewall.MACAddress;

import com.metavize.mvvm.networking.DnsStaticHostRule;


/**
 * Immutable representation of a static DHCP lease.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 */
public class DnsStaticHostInternal
{
    private final boolean    isLive;
    private final String     name;
    private final String     category;
    private final String     description;

    private final HostNameList hostnameList;
    private final IPaddr       staticAddress;

    // Constructors 
    public DnsStaticHostInternal( DnsStaticHostRule rule )
    {
        this( rule.isLive(), rule.getName(), rule.getCategory(), rule.getDescription(),
              rule.getHostNameList(), rule.getStaticAddress());
    }

    /* This is only used internally for merging lists together */
    public DnsStaticHostInternal( HostNameList hostnameList, IPaddr staticAddress )
    {
        this( true, Rule.EMPTY_NAME, Rule.EMPTY_CATEGORY, Rule.EMPTY_DESCRIPTION,
              hostnameList, staticAddress );
    }

    public DnsStaticHostInternal( boolean isLive, String name, String category, String description,
                                  HostNameList hostnameList, IPaddr staticAddress )
    {
        this.isLive        = isLive;
        this.name          = name;
        this.category      = category;
        this.description   = description;
        this.hostnameList  = hostnameList;
        this.staticAddress = staticAddress;

    }
    
    public HostNameList getHostNameList()
    {
        return hostnameList;
    }

    /** Get static IP address for this host name list */
    public IPaddr getStaticAddress()
    {
        return this.staticAddress;
    }
    
    /** The following are just so this can be converted back to a rule */
    public boolean isLive()
    {
        return this.isLive;
    }

    public String getName()
    {
        return this.name;
    }
    
    public String getDescription()
    {
        return this.description;
    }

    public String getCategory()
    {
        return this.category;
    }

    public DnsStaticHostRule toRule()
    {
        DnsStaticHostRule rule = new DnsStaticHostRule( getHostNameList(), getStaticAddress());

        rule.setLive( isLive );
        rule.setName( getName());
        rule.setCategory( getCategory());
        rule.setDescription( getDescription());
        
        return rule;
    }
}
