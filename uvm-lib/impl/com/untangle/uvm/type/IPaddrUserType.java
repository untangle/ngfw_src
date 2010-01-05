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

import com.untangle.uvm.node.IPaddr;

/**
 * Hibernate <code>UserType</code> for persisting
 * <code>IPaddr</code> objects.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class IPaddrUserType extends StringBasedUserType
{
    private static final IPaddr EMPTY_IPADDR = new IPaddr( null );

    /* special string used to represent an empty ip addr */
    private static final String EMPTY_STRING = "0.0.0.0/31";

    public Class<IPaddr> returnedClass()
    {
        return IPaddr.class;
    }

    protected String userTypeToString( Object v )
    {
        /* null (don't actually have to worry about null because of the StringBasedUserType.) */
        if ( v == null ) return null;

        IPaddr i = (IPaddr)v;

        if ( i.isEmpty()) return EMPTY_STRING;

        return ((IPaddr)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        /* the following are the conditions which should use the empty ip addr */

        /* null (don't actually have to worry about null because of the StringBasedUserType.) */
        if ( val == null ) return EMPTY_IPADDR;

        /* empty string */
        val = val.trim();
        if ( val.length() == 0 ) return EMPTY_IPADDR;

        /* This is a special string that is used to store an empty IP
         * address in the database.  this is because 0.0.0.0 (an
         * address of 0.0.0.0) differs from IPaddr( null ) (no address
         * at all). */
        if ( EMPTY_STRING.equals( val )) return EMPTY_IPADDR;

        return IPaddr.parse( val );
    }
}
