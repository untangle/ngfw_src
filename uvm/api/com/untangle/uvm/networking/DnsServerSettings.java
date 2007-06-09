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

import java.util.List;

import com.untangle.uvm.node.HostName;
import com.untangle.uvm.node.IPaddr;

/**
 * The settings for the DNS server running on the Untangle.
 */
public interface DnsServerSettings
{
    /**
     * Retrieve the on/off switch for the DNS server.
     *
     * @return True iff the dns server is enabled.
     */
    public boolean getDnsEnabled();

    /**
     * Set if the DNS server is enabled.
     *
     * @param newValue True iff the dns server is enabled.
     */
    public void setDnsEnabled( boolean newValue );

    /**
     * Get the local DNS domain, this is the domain for the internal
     * private network.
     *
     * @return The local DNS domain.
     */
    public HostName getDnsLocalDomain();

    /**
     * Set the local DNS domain, this is the domain for the internal
     * private network.
     *
     * @param newValue The new local DNS domain.
     */
    public void setDnsLocalDomain( HostName newValue );

    /**
     * Set the additional list of dns entries that should resolve.
     * The DNS server will serve entries for machines that register
     * with DHCP (The Dynamic Host List) as well as the list of
     * addresses that are in this list.
     *
     * @return The list of static DNS entries.
     */
    public List<DnsStaticHostRule> getDnsStaticHostList();

    /**
     * Set the static list of DNS entries.
     *
     * @param newValue The new list of static DNS entries.
     */
    public void setDnsStaticHostList( List<DnsStaticHostRule> newValue );
}
