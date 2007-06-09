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

package com.untangle.mvvm.networking.internal;

import java.util.Date;

import com.untangle.mvvm.tran.IPNullAddr;
import com.untangle.mvvm.tran.firewall.MACAddress;

import com.untangle.mvvm.networking.DhcpLeaseRule;


/**
 * Immutable representation of a static DHCP lease.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 */
public class DhcpLeaseInternal
{
    private final boolean    isLive;
    private final String     name;
    private final String     category;
    private final String     description;
    private final MACAddress macAddress;
    private final String     hostname;
    private final IPNullAddr staticAddress;
    private final boolean    resolvedByMac; /* Presently unused */

    // Constructors 
    public DhcpLeaseInternal( DhcpLeaseRule rule )
    {
        this.isLive        = rule.isLive();
        this.name          = rule.getName();
        this.category      = rule.getCategory();
        this.description   = rule.getDescription();
        this.macAddress    = rule.getMacAddress();
        this.staticAddress = rule.getStaticAddress();
        this.resolvedByMac = rule.getResolvedByMac(); /* This is presently unsupported */
        
        /* When resolving by hostname, the hostname is important for the static rule */
        if ( this.resolvedByMac == false ) {
            this.hostname = rule.getHostname();
        } else {
            this.hostname = "";
        }
    }

    public MACAddress getMacAddress()
    {
        return macAddress;
    }
    
    /** Get static IP address for this MAC address */
    public IPNullAddr getStaticAddress()
    {
        return this.staticAddress;
    }
    
    /**
     * Resolve by MAC
     * @return true if the MAC address is used to resolve this rule, false if the hostname should be used.
     * this is presently unsupported.
     */
    public boolean getResolvedByMac()
    {
        return this.resolvedByMac;
    }

    /** The hostname */
    public String getHostname()
    {
        return this.hostname;
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

    public DhcpLeaseRule toRule()
    {
        DhcpLeaseRule rule = new DhcpLeaseRule( getMacAddress(), getHostname(), (IPNullAddr)null, 
                                                getStaticAddress(), (Date)null, getResolvedByMac());
                                                

        rule.setLive( isLive );
        rule.setName( getName());
        rule.setCategory( getCategory());
        rule.setDescription( getDescription());
        
        return rule;
    }
}
