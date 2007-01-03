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

package com.untangle.mvvm.type.firewall;

import com.untangle.mvvm.type.StringBasedUserType;

import com.untangle.mvvm.tran.firewall.ip.IPMatcher;
import com.untangle.mvvm.tran.firewall.ip.IPMatcherFactory;

public class IPMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return IPMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IPMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return IPMatcherFactory.parse( val );
    }
}
