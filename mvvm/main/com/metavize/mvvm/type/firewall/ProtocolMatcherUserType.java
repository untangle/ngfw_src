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

package com.metavize.mvvm.type.firewall;

import com.metavize.mvvm.type.StringBasedUserType;

import com.metavize.mvvm.tran.firewall.ProtocolMatcher;

public class ProtocolMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return ProtocolMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((ProtocolMatcher)v).toString();
    }

    public Object createUserType( String val )
    {
        return ProtocolMatcher.parse( val );
    }
}
