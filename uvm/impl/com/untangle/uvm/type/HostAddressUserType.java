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

package com.untangle.mvvm.type;

import java.net.InetAddress;

import com.untangle.mvvm.tran.HostAddress;
import com.untangle.mvvm.tran.IPaddr;

public class HostAddressUserType extends StringBasedUserType
{
    private static final HostAddress EMPTY_IPADDR = new HostAddress( new IPaddr( null ));
    
    public Class returnedClass()
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
