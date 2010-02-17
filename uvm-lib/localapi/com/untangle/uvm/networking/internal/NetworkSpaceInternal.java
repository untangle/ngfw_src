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

package com.untangle.uvm.networking.internal;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.IPNetworkRule;
import com.untangle.uvm.networking.NetworkSpace;
import com.untangle.uvm.node.ValidateException;

public class NetworkSpaceInternal
{
    /** List of networks on this network space */
    private final List<IPNetwork> networkList;

    /* This is the primary address for this network space, it may come
     * from DHCP or from the network list, if always is never null and
     * never the empty address. */
    private final IPNetwork primaryAddress;

    /* */
    private final String name;
    private final String category;
    private final String description;


    /* List of interfaces in this network space */
    public NetworkSpaceInternal( NetworkSpace networkSpace, IPNetwork primaryAddress )
        throws ValidateException
    {
        /* Get the network list */
        /* Convert the list to ip network */
        /* This is going to lose the name/description/category info */
        List<IPNetwork> networkList = new LinkedList<IPNetwork>();
        for ( IPNetworkRule rule : (List<IPNetworkRule>)networkSpace.getNetworkList()) {
            networkList.add( rule.getIPNetwork());
        }
        this.networkList = Collections.unmodifiableList( networkList );

        /* Set the primary address */
        this.primaryAddress = primaryAddress;

        /** stuff from a rule */
        this.name = networkSpace.getName();
        this.category = networkSpace.getCategory();
        this.description = networkSpace.getDescription();
    }

    public List<IPNetwork> getNetworkList()
    {
        return this.networkList;
    }

    public List<IPNetworkRule> getNetworkRuleList()
    {
        List<IPNetworkRule> networkList = new LinkedList<IPNetworkRule>();
        for ( IPNetwork network : getNetworkList()) networkList.add( new IPNetworkRule( network ));
        return networkList;
    }

    public IPNetwork getPrimaryAddress()
    {
        return this.primaryAddress;
    }

    public String getName()
    {
        return this.name;
    }

    public String getDescription()
    {
        return this.description;
    }

    public String getCategory()
    {
        return this.category;
    }

    /* Create a new network space, use with caution, since the links between
     * interfaces and other network spaces cannot be maintained */
    public NetworkSpace toNetworkSpace()
    {
        /* Create a copy of the network list, since the current list is not modifiable */
        NetworkSpace s = new NetworkSpace();        
        s.setNetworkList( getNetworkRuleList());
        s.setName( getName());
        s.setDescription( getDescription());
        s.setCategory( getCategory());

        return s;
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "name:      "     + getName());
        sb.append( "\nnetworks:    " + getNetworkList());
        sb.append( "\nprimary:     " + getPrimaryAddress());

        return sb.toString();
    }
}
