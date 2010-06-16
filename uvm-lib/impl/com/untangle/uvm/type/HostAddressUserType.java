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

package com.untangle.uvm.type;

import com.untangle.uvm.node.HostAddress;
import com.untangle.uvm.node.IPaddr;

@SuppressWarnings("serial")
public class HostAddressUserType extends StringBasedUserType
{
    private static final HostAddress EMPTY_IPADDR = new HostAddress( new IPaddr( null ));
    
    public Class<HostAddress> returnedClass()
    {
        return HostAddress.class;
    }

    protected String userTypeToString( Object v )
    {
        /* null (don't actually have to worry about null because of the StringBasedUserType.) */
        if ( v == null ) return null;
                
        return ((HostAddress)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        /* the following are the conditions which should use the empty ip addr */

        /* null (don't actually have to worry about null because of the StringBasedUserType.) */
        if ( val == null ) return EMPTY_IPADDR;

        /* empty string */
        val = val.trim();
        if ( val.length() == 0 ) return EMPTY_IPADDR;
       
        return HostAddress.parse( val );
    }
}
