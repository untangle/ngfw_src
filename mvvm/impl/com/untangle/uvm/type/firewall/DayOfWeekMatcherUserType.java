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

import com.untangle.mvvm.tran.firewall.time.DayOfWeekMatcher;
import com.untangle.mvvm.tran.firewall.time.DayOfWeekMatcherFactory;

public class DayOfWeekMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return DayOfWeekMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((DayOfWeekMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return DayOfWeekMatcherFactory.parse( val );
    }
}
