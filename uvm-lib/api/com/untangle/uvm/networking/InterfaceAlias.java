
/*
 * $HeadURL$
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

import static com.untangle.uvm.networking.NetworkUtil.EMPTY_IPADDR;

import java.io.Serializable;
import java.net.Inet4Address;
import java.net.InetAddress;

import com.untangle.uvm.node.IPaddr;

public class InterfaceAlias implements Serializable
{
    private static final long serialVersionUID = -2103291468092590446L;

    private IPaddr address;
    private IPaddr netmask;
    // Presently unused, but settable */
    private IPaddr broadcast;

    public InterfaceAlias()
    {
        this.address   = EMPTY_IPADDR;
        this.netmask   = EMPTY_IPADDR;
        this.broadcast = EMPTY_IPADDR;
    }

    public InterfaceAlias( IPaddr address, IPaddr netmask )
    {
        this.address   = address;
        this.netmask   = netmask;
        this.broadcast = EMPTY_IPADDR;
    }

    public InterfaceAlias( IPaddr address, IPaddr netmask, IPaddr broadcast )
    {
        this.address   = address;
        this.netmask   = netmask;
        this.broadcast = broadcast;
    }

    public InterfaceAlias( InetAddress address, InetAddress netmask, InetAddress broadcast )
    {
        this.address   = new IPaddr((Inet4Address)address );
        this.netmask   = new IPaddr((Inet4Address)netmask );
        this.broadcast = new IPaddr((Inet4Address)broadcast );
    }

    public IPaddr getAddress()
    {
        if ( this.address == null || this.address.isEmpty()) this.address = EMPTY_IPADDR;
        return this.address;
    }

    public void setAddress( IPaddr address)
    {
        if ( null == address || address.isEmpty()) address = EMPTY_IPADDR;
        this.address = address;
    }

    public IPaddr getNetmask()
    {
        if ( null == this.netmask || this.netmask.isEmpty()) this.netmask = EMPTY_IPADDR;
        return this.netmask;
    }

    public void setNetmask( IPaddr netmask)
    {
        if ( null == netmask || netmask.isEmpty()) netmask = EMPTY_IPADDR;
        this.netmask = netmask;
    }

    public IPaddr getBroadcast()
    {
        if ( null == this.broadcast || this.broadcast.isEmpty()) this.broadcast = EMPTY_IPADDR;
        return this.broadcast;
    }

    public void setBroadcast( IPaddr broadcast)
    {
        if ( null == broadcast || broadcast.isEmpty()) broadcast = EMPTY_IPADDR;
        this.broadcast = broadcast;
    }

    public boolean isValid()
    {
        if (( null == this.address ) || ( null == this.netmask )) return false;
        if ( this.address.isEmpty() || this.netmask.isEmpty())    return false;
        return true;
    }

    public boolean equals(Object newObject)
    {
        if (null == newObject ||
            false == (newObject instanceof InterfaceAlias)) {
            return false;
        }

        InterfaceAlias newIA = (InterfaceAlias) newObject;
        InterfaceAlias curIA = this;

        if (false == curIA.getAddress().equals(newIA.getAddress())) {
            return false;
        }

        if (false == curIA.getNetmask().equals(newIA.getNetmask())) {
            return false;
        }

        if (false == curIA.getBroadcast().equals(newIA.getBroadcast())) {
            return false;
        }

        return true;
    }
}
