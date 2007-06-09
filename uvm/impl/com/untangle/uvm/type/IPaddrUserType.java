/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.uvm.type;

import com.untangle.uvm.node.IPaddr;

import java.net.UnknownHostException;

public class IPaddrUserType extends StringBasedUserType
{
    private static final IPaddr EMPTY_IPADDR = new IPaddr( null );

    /* special string used to represent an empty ip addr */
    private static final String EMPTY_STRING = "0.0.0.0/31";
    
    public Class returnedClass()
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
