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

import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;

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
