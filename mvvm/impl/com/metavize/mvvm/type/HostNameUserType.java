/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.type;

import com.metavize.mvvm.tran.HostName;

public class HostNameUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return HostName.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((HostName)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return HostName.parse( val );
    }
}
