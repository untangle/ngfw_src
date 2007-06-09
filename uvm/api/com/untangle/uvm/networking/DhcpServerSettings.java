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

import java.util.List;

import com.untangle.mvvm.tran.IPaddr;

/**
 * The settings for the DHCP server on the untangle.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
public interface DhcpServerSettings
{
    /**
     * Returns whether or not the DHCP server is enabled.
     *
     * @return True iff the DHCP server is enabled.
     */
    public boolean getDhcpEnabled();

    /**
     * Set whether or not the DHCP server is enabled.
     *
     * @param newValue True iff the DHCP server is enabled.
     */
    public void setDhcpEnabled( boolean newValue );

    /**
     * Retrieve the start of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @return The start of the DHCP dynamic range.
     */
    public IPaddr getDhcpStartAddress();

    /**
     * Set the start of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @param newValue The start of the DHCP dynamic range.
     */
    public void setDhcpStartAddress( IPaddr newValue );

    /**
     * Retrieve the end of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @return The end of the DHCP dynamic range.
     */
    public IPaddr getDhcpEndAddress();

    /**
     * Set the end of the range of addresses the DHCP server
     * can distribute dynamically.
     *
     * @param newValue The end of the DHCP dynamic range.
     */
    public void setDhcpEndAddress( IPaddr newValue );

    /**
     * Set the range of addresses the DHCP server can distribute
     * dynamically.  This should automatically swap start and end if
     * necessary.
     *
     * @param start The start of the DHCP dynamic range.
     * @param end The end of the DHCP dynamic range.
     */
    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end );

    /**
     * Retrieve the number of seconds that a dynamic DHCP lease should
     * be valid for.
     *
     * @return The length of the lease in seconds.
     */
    public int getDhcpLeaseTime();

    /**
     * Set the number of seconds that a dynamic DHCP lease should be
     * valid for.
     *
     * @param newValue The length of the lease in seconds.
     */
    public void setDhcpLeaseTime( int newValue );

    /**
     * Retrieve the current list of DHCP leases.  This includes both
     * static and dynamic leases.
     *
     * @return The current DHCP leases.
     */
    public List<DhcpLeaseRule> getDhcpLeaseList();

    /**
     * Set the current list of DHCP leases.  Dynamic DHCP leases are
     * not saved.
     *
     * @param newValue The new list of leases.
     */
    public void setDhcpLeaseList( List<DhcpLeaseRule> newValue );
}
