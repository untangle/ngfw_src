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

import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.protocol.ProtocolMatcherFactory;

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
