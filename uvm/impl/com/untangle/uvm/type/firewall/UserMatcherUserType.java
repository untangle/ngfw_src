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

package com.untangle.uvm.type.firewall;

import com.untangle.uvm.type.StringBasedUserType;

import com.untangle.uvm.node.firewall.user.UserMatcher;
import com.untangle.uvm.node.firewall.user.UserMatcherFactory;

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
