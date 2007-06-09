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

import com.untangle.mvvm.tran.firewall.port.PortMatcher;
import com.untangle.mvvm.tran.firewall.port.PortMatcherFactory;

public class PortMatcherUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return PortMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((PortMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return PortMatcherFactory.parse( val );
    }
}
