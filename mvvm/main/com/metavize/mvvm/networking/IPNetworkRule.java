/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.tran.Rule;

/**
 * An IPNetwork that is to go into a list, this is only to 
 * allow lists of IPNetworks to be saved.  Normally, an IPNetwork is
 * just stored using the IPNetworkUserType.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_mvvm_network_route"
 */
public class IPNetworkRule extends Rule
{
    private IPNetwork network;

    public IPNetworkRule()
    {
    }

    public IPNetworkRule( IPNetwork network )
    {
        this.network = network;
    }

    /**
     * The IPNetwork associated with this rule.
     * @return The IPNetwork associated with this rule.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.IPNetworkUserType"
     * @hibernate.column
     * name="network"
     */
    public IPNetwork getNetwork()
    {
        return this.network;
    }

    public void setNetwork( IPNetwork network )
    {
        this.network = network;
    }
}
