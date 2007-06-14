/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.router;

import com.untangle.uvm.node.IPaddr;

public interface RouterBasicSettings extends RouterCommonSettings
{
    /** Get whether or not nat is enabled. */
    public boolean getNatEnabled();

    public void setNatEnabled( boolean newValue );

    /** Get the base of the internal address. */
    public IPaddr getNatInternalAddress();

    public void setNatInternalAddress( IPaddr newValue );

    /** Get the subnet of the internal addresses. */
    public IPaddr getNatInternalSubnet();

    public void setNatInternalSubnet( IPaddr newValue );

    /**  Get whether or not DMZ is being used. */
    public boolean getDmzEnabled();

    public void setDmzEnabled( boolean newValue );

    /** Get whether or not DMZ events should be logged.*/
    public boolean getDmzLoggingEnabled();

    public void setDmzLoggingEnabled( boolean newValue );

    /** Get the address of the dmz host */
    public IPaddr getDmzAddress();

    public void setDmzAddress( IPaddr newValue );
}
