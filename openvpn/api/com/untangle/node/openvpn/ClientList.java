/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

public class ClientList implements Serializable, Validatable
{ 

    List<VpnClient> clientList;

    public ClientList()
    {
        this( new LinkedList<VpnClient>());
    }

    public ClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }

    public List<VpnClient> getClientList()
    {
        return this.clientList;
    }
    
    public void setClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        Set<String> nameSet = new HashSet<String>();
        for ( VpnClient client : this.clientList ) {
            client.validate();
            String name = client.getInternalName();
            if ( !nameSet.add( name )) {
                throw new ValidateException( "Client and site names must all be unique: '" + name + "'" );
            }
        }
    }
}
