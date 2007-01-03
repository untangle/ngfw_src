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

public interface DhcpServerSettings
{
    // !!!!! private static final long serialVersionUID = 4349679825783697834L;

    /**
     * Returns If DHCP is enabled.
     */
    public boolean getDhcpEnabled();

    public void setDhcpEnabled( boolean b );

    /**
     * Get the start address of the range of addresses to server.
     */
    public IPaddr getDhcpStartAddress();

    public void setDhcpStartAddress( IPaddr newValue );

    /**
     * Get the end address of the range of addresses to server.
     */
    public IPaddr getDhcpEndAddress();

    public void setDhcpEndAddress( IPaddr newValue );

    /** Set the starting and end address of the dns server */
    public void setDhcpStartAndEndAddress( IPaddr start, IPaddr end );

    /**
     * Get the default length of the DHCP lease in seconds.
     */
    public int getDhcpLeaseTime();

    public void setDhcpLeaseTime( int time );

    /**
     * List of the dhcp leases.
     */
    public List<DhcpLeaseRule> getDhcpLeaseList();

    public void setDhcpLeaseList( List<DhcpLeaseRule> s );
}
