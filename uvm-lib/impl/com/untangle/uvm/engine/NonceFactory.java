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
 * $Id$
 */

package com.untangle.uvm.engine;

import java.security.Principal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A global factory to retrieve a nonce from.  This should be shared amongst
 * any realm that is compatible with the nonce system.
 */
class NonceFactory // implements Map<String,Principal>
{
    // XXX Very small memory leak here if the nonce is never used (quite rare)
    private Map<String, Principal> nonces;
    
    NonceFactory()
    {
        this.nonces = Collections.synchronizedMap( new HashMap<String, Principal>());
    }

    public void clear()
    {
        this.nonces.clear();
    }
   
    public boolean containsKey( Object key )
    {
        return this.nonces.containsKey( key );
    }

    public boolean containsValues( Object value )
    {
        return this.nonces.containsValue( value );
    }

    public Set<Map.Entry<String,Principal>> entrySet()
    {
        return this.nonces.entrySet();
    }

    public boolean equals( Object o )
    {
        if (!( o instanceof NonceFactory )) return false;

        return ((NonceFactory)o).nonces.equals( this.nonces );
    }

    public Principal get( Object key )
    {
        return this.nonces.get( key );
    }

    public int hashCode()
    {
        return this.nonces.hashCode();
    }

    public boolean isEmpty()
    {
        return this.nonces.isEmpty();
    }

    public Set<String> keySet()
    {
        return this.nonces.keySet();
    }

    public Principal put( String key, Principal value )
    {
        return this.nonces.put( key, value );
    }

    public void putAll( Map<String,Principal> t )
    {
        this.nonces.putAll( t );
    }

    public Principal remove( Object key )
    {
        return this.nonces.remove( key );
    }
    
    public int size()
    {
        return this.nonces.size();
    }

    public Collection<Principal> values()
    {
        return this.nonces.values();
    }
}
