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

import com.untangle.mvvm.tran.IPaddr;

import java.net.UnknownHostException;

public class IPaddrUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return IPaddr.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IPaddr)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return IPaddr.parse( val );
    }
}
