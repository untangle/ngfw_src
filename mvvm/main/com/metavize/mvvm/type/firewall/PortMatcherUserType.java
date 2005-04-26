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

import com.metavize.mvvm.tran.firewall.PortMatcher;

public class PortMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return PortMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((PortMatcher)v).toString();
    }

    public Object createUserType( String val )
    {
        return PortMatcher.parse( val );
    }
}
