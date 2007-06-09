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

package com.untangle.node.openvpn;

import javax.persistence.MappedSuperclass;

import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;
import org.hibernate.annotations.Type;

/**
 * A network that is available at a site.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@MappedSuperclass
public abstract class SiteNetwork extends Rule implements Validatable
{
    private static final long serialVersionUID = -2918169040527785684L;

    private IPaddr network;
    private IPaddr netmask;

    // constructors -----------------------------------------------------------

    public SiteNetwork() { }

    // accessors --------------------------------------------------------------

    /**
     * @return network exported by this client or server.
     */
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getNetwork()
    {
        return this.network;
    }

    public void setNetwork( IPaddr network )
    {
        this.network = network;
    }

    /**
     * Get the range of netmask on the client side(null for site->machine).
     *
     * @return This is the network that is reachable when this client connects.
     */
    @Type(type="com.untangle.uvm.type.IPaddrUserType")
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask( IPaddr netmask )
    {
        this.netmask = netmask;
    }

    public void validate() throws ValidateException
    {
        /* XXXXXXXXXXXXXXXXXXXXXXXXXXXXXX */
    }
}
