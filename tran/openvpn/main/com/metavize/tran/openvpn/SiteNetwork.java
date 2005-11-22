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

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Validatable;

import com.metavize.mvvm.tran.ValidateException;

/**
 * A network that is available at a site.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 */
public abstract class SiteNetwork extends Rule implements Validatable
{
    // XXX update the serial version id
    // private static final long serialVersionUID = 4143567998376955882L;

    private IPaddr network;
    private IPaddr netmask;
    
    // constructors -----------------------------------------------------------
    
    /**
     * Hibernate constructor.
     */
    public SiteNetwork()
    {
    }

    // accessors --------------------------------------------------------------

    /**
     *
     * @return network exported by this client or server.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="network"
     * sql-type="inet"
     */
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
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPaddrUserType"
     * @hibernate.column
     * name="netmask"
     * sql-type="inet"
     */
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
