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

import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;

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
