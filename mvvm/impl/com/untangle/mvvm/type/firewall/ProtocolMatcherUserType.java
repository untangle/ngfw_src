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

import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcher;
import com.untangle.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;

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

    public Object createUserType( String val ) throws Exception
    {
        return ProtocolMatcherFactory.parse( val );
    }
}
