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

package com.untangle.uvm.type;

import com.untangle.uvm.node.HostName;

public class HostNameUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return HostName.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((HostName)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return HostName.parse( val );
    }
}
