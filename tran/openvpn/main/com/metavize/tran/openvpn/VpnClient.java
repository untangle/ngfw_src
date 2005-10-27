/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import java.io.Serializable;

import com.metavize.mvvm.security.Tid;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;

/**
 * the configuration for a vpn client.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_vpn_client"
 */
public abstract class VpnClient extends Rule implements Validatable
{
    // XXX update the serial version id
    // private static final long serialVersionUID = 4143567998376955882L;

    private IPaddr address;            // nullable
    private IPaddr netmask;            // nullable

    // The address group to pull this client address, may be null.
    private VpnAddressGroup addressGroup;  // nullable

    private IPaddr siteAddress;            // nullable
    private IPaddr siteNetmask;            // nullable

    /* The interface that this clients is associated with, null to use clientPoolIntf from VpnSettings */
    private byte clientIntf;

    private boolean isEdgeguard = false;
    
    // constructors -----------------------------------------------------------
    
    /**
     * Hibernate constructor.
     */
    public VpnClient()
    {
    }

    // accessors --------------------------------------------------------------

    /**
     * @return the address group this client belongs to, null if the client has a static address.
     * column="address_group"
     */
    VpnAddressGroup getAddressGroup()
    {
        return this.addressGroup;
    }

    void setAddressGroup( VpnAddressGroup addressGroup )
    {
        this.addressGroup = addressGroup;
    }

    /* have to somehow convey that each user actually uses two address */

    /**
     * Static address of this client, this can only be set if address group is null.
     *   not available in bridge mode.
     *
     * @return static address of the machine.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="address"
     * sql-type="inet"
     */
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress( IPaddr address )
    {
        this.address = address;
    }

    /* XXX This may need to be a list of addresses */

    /**
     * Get the range of addresses on the client side(null for site->machine).
     *
     * @return This is the network that is reachable when this client connects.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="site_address"
     * sql-type="inet"
     */
    public IPaddr getSiteAddress()
    {
        return this.siteAddress;
    }

    public void setSideAddress( IPaddr siteAddress )
    {
        this.siteAddress = siteAddress;
    }
    
    /**
     * Get the range of netmask on the client side(null for site->machine).
     *
     * @return This is the network that is reachable when this client connects.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="site_netmask"
     * sql-type="inet"
     */
    public IPaddr getSiteNetmask()
    {
        return this.siteNetmask;
    }

    public void setSideNetmask( IPaddr siteNetmask )
    {
        this.siteNetmask = siteNetmask;
    }
    
    /**
     * @return whether the other side is an edgeguard.
     * column="is_bridge"
     */
    public boolean isEdgeguard()
    {
        return this.isEdgeguard;
    }

    public void setEdgeguard( boolean isEdgeguard )
    {
        this.isEdgeguard = isEdgeguard;
    }
}
