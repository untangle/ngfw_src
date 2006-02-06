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

import java.net.InetAddress;

import com.metavize.mvvm.tran.Rule;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.ParseException;

/**
 * An IPNetwork that is to go into a list, this is only to 
 * allow lists of IPNetworks to be saved.  Normally, an IPNetwork is
 * just stored using the IPNetworkUserType.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="mvvm_network_ip"
 */
public class IPNetworkRule extends Rule
{
    private IPNetwork ipNetwork;

    public IPNetworkRule()
    {
    }

    public IPNetworkRule( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /**
     * The IPNetwork associated with this rule.
     * @return The IPNetwork associated with this rule.
     * @hibernate.property
     * type="com.metavize.mvvm.networking.IPNetworkUserType"
     * @hibernate.column
     * name="network"
     */
    public IPNetwork getIPNetwork()
    {
        return this.ipNetwork;
    }

    public void setIPNetwork( IPNetwork ipNetwork )
    {
        this.ipNetwork = ipNetwork;
    }

    /** The following are convenience methods, an IPNetwork is immutable, so the
     *  corresponding setters do not exist */
    public IPaddr getNetwork()
    {
        return this.ipNetwork.getNetwork();
    }

    public IPaddr getNetmask()
    {
        return this.ipNetwork.getNetmask();
    }

    public static IPNetworkRule parse( String value ) throws ParseException
    {
        return new IPNetworkRule( IPNetwork.parse( value ));
    }

    public static IPNetworkRule makeIPNetwork( InetAddress network, InetAddress netmask )
    {
        return new IPNetworkRule( IPNetwork.makeIPNetwork( network, netmask ));
    }

    public static IPNetworkRule makeIPNetwork( IPaddr network, IPaddr netmask )
    {
        return new IPNetworkRule( IPNetwork.makeIPNetwork( network, netmask ));
    }
}
