/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.type.firewall;

import com.metavize.mvvm.type.StringBasedUserType;

import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;

public class IntfMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return IntfMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IntfMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return IntfMatcherFactory.parse( val );
    }
}
