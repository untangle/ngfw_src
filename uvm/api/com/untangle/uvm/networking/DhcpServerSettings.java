/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.uvm.networking;

import java.util.List;

import com.untangle.uvm.node.IPaddr;

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
