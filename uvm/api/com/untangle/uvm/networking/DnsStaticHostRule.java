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

package com.untangle.uvm.networking;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.untangle.uvm.node.HostNameList;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Rule;
import org.hibernate.annotations.Type;

/**
 * Rule for storing DNS static hosts.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="uvm_dns_static_host_rule", schema="settings")
public class DnsStaticHostRule extends Rule
{
    private static final long serialVersionUID = -9166468521319948021L;

    /** The list of hostnames for this rule */
    private HostNameList hostNameList = HostNameList.getEmptyHostNameList();

    /** The IP address that all of these hostnames resolves to */
    private IPaddr staticAddress = null;

    // Constructors
    public DnsStaticHostRule() { }

    public DnsStaticHostRule(HostNameList hostNameList, IPaddr staticAddress)
    {
        this.hostNameList  = hostNameList;
        this.staticAddress = staticAddress;
    }

    /**
     * This is the list of hostnames that should resolve to
     * <code>staticAddress</code>.
     *
     * @return The list of hostnames that resolve to
     * <code>staticAddress</code>.
     */
    @Column(name="hostname_list")
    @Type(type="com.untangle.uvm.type.HostNameListUserType")
    public HostNameList getHostNameList()
    {
        if ( hostNameList == null )
            hostNameList = HostNameList.getEmptyHostNameList();

        return hostNameList;
    }

    /**
     * Set the list of hostnames that should resolve to
     * <code>staticAddress</code>.
     *
     * @param hostnameList The list of hostnames that resolve to
     * <code>staticAddress</code>.
     */
    public void setHostNameList( HostNameList hostNameList )
    {
        this.hostNameList = hostNameList;
    }

    /**
     * Get the IP address of this entry.
     *
     * @return The IP address for this entry.
     */
    @Column(name="static_address")
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getStaticAddress()
    {
        return this.staticAddress;
    }

    /**
     * Set the IP address of this entry.
     *
     * @param staticAddress The IP address for this entry.
     */
    public void setStaticAddress( IPaddr staticAddress )
    {
        this.staticAddress = staticAddress;
    }
}
