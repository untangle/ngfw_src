/*
 * $HeadURL$
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

package com.untangle.uvm.argon;

/* A node interface is an interface that can be added by a node.
 * presently this only exists for VPN.
 */
class NodeInterface
{
    private final int   argonIntf;
    private final String deviceName;
    
    NodeInterface( int argonIntf, String deviceName )
    {
        this.argonIntf = argonIntf;
        this.deviceName = deviceName;
    }

    public int argonIntf()
    {
        return this.argonIntf;
    }

    public String deviceName()
    {
        return this.deviceName;
    }
}
