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

package com.untangle.uvm.type.firewall;

import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class IPMatcherUserType extends StringBasedUserType
{
    public Class<IPMatcher> returnedClass()
    {
        return IPMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IPMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new IPMatcher( val );
    }
}
