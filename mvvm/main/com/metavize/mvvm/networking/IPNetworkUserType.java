/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.networking;

import com.metavize.mvvm.type.StringBasedUserType;

public class IPNetworkUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return IPNetwork.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IPNetwork)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return IPNetwork.parse( val );
    }
}
