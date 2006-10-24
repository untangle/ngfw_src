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

package com.metavize.mvvm.type.firewall;

import com.metavize.mvvm.type.StringBasedUserType;

import com.metavize.mvvm.tran.firewall.user.UserMatcher;
import com.metavize.mvvm.tran.firewall.user.UserMatcherFactory;

public class UserMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return UserMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((UserMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return UserMatcherFactory.parse( val );
    }
}
