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

import com.untangle.uvm.node.IPaddr;

/* This is an immutable settings object used locally.  This has
 * already been processed and cannot be modified.  It represents the
 * current configuration of the UVM.  All of the lists inside of this
 * are immutable, and the objects they point to are immutable. */
public class NetworkSpacesInternalSettings
{
    private final List<InterfaceInternal> interfaceList;

    private final List<NetworkSpaceInternal> networkSpaceList;
 
    /* These may or may not come from DHCP, they are the actual values
     * that have been set. */
    private final IPaddr dns1;
    private final IPaddr dns2;
    private final IPaddr defaultRoute;

    public NetworkSpacesInternalSettings( List<InterfaceInternal> interfaceList,
                                          List<NetworkSpaceInternal> networkSpaceList,
                                          IPaddr dns1, IPaddr dns2, IPaddr defaultRoute )
    {
        this.interfaceList     = Collections.unmodifiableList( new LinkedList<InterfaceInternal>( interfaceList ));
        this.networkSpaceList  = Collections.unmodifiableList( new LinkedList<NetworkSpaceInternal>( networkSpaceList ));

        this.dns1          = dns1;
        this.dns2          = dns2;
        this.defaultRoute  = defaultRoute;
    }
    
    public List<InterfaceInternal> getInterfaceList()
    {
        return this.interfaceList;
    }

    public List<NetworkSpaceInternal> getNetworkSpaceList()
    {
        return this.networkSpaceList;
    }

    public InterfaceInternal getInterface( byte argonIndex )
    {
        for ( InterfaceInternal i : this.interfaceList ) {
            if ( i.getArgonIntf().getArgon() == argonIndex ) return i;
        }

        return null;
    }

    public NetworkSpaceInternal getNetworkSpace( byte argonIndex )
    {
        for ( InterfaceInternal i : this.interfaceList ) {
            if ( i.getArgonIntf().getArgon() == argonIndex ) {
                return i.getNetworkSpace();
            }
        }

        return null;
    }


    public IPaddr getDefaultRoute()
    {
        return this.defaultRoute;
    }
    
    public IPaddr getDns1()
    {
        return this.dns1;
    }

    public IPaddr getDns2()
    {
        return this.dns2;
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "Network Settings\n" );
        
        sb.append( "\nInterfaces:\n" );
        for ( InterfaceInternal intf : getInterfaceList()) sb.append( intf + "\n" );
        
        sb.append( "Network Spaces:\n" );
        for ( NetworkSpaceInternal space : getNetworkSpaceList()) sb.append( space + "\n" );
            
        sb.append( "dns1:     " + getDns1());
        sb.append( "\ndns2:     " + getDns2());
        sb.append( "\ngateway:  " + getDefaultRoute());
        return sb.toString();
    }
}

