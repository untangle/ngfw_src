
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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.Inet4Address;

import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.Equivalence;
import static com.untangle.mvvm.networking.NetworkUtil.EMPTY_IPADDR;

public class InterfaceAlias implements Serializable, Equivalence
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
