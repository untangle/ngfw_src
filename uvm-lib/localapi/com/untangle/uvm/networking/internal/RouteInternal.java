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

import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.Route;
import com.untangle.uvm.node.IPaddr;

/** An immutable routing entry */
public class RouteInternal
{    
    private final IPNetwork destination;
    private final IPaddr nextHop;
    
    /* values from a rule */
    private final boolean isEnabled;
    private final String name;
    private final String category;
    private final String description;


    private RouteInternal( Route route )
    {
        this.destination = route.getDestination();
        this.nextHop = route.getNextHop();
        
        this.isEnabled = route.isLive();
        this.name = route.getName();
        this.category = route.getCategory();
        this.description = route.getDescription();
    }
    
    /** The network space this route belongs to, this will not be
     * supported until =RELEASE3.3=. */
    // public NetworkSpaceInternal getNetworkSpace()
    // {
    // return this.networkSpace;
    // }

    public boolean getIsEnabled()
    {
        return this.isEnabled;
    }

    /** destination network that triggers this routing entry. */
    public IPNetwork getDestination()
    {
        return this.destination;
    }

    /** The IP address of the next router. */
    public IPaddr getNextHop()
    {
        return this.nextHop;
    }

    public Route toRoute()
    {
        Route route = new Route( null, this.destination, this.nextHop );
        route.setLive( getIsEnabled());
        route.setName( getName());
        route.setDescription( getDescription());
        route.setCategory( getCategory());
        return route;
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

    public String toString()
    {
        return
            "name:        "   + getName() +
            "\ndestination: " + getDestination() +
            "\nnext-hop:    " + getNextHop();
    }

    public static RouteInternal makeInstance( Route route )
    {
        return new RouteInternal( route );
    }

    
}
