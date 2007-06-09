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

import com.untangle.uvm.node.HostNameList;

public class HostNameListUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return HostNameList.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((HostNameList)v).toString();
    }

    public Object createUserType( String val )
    {
        try {
            return HostNameList.parse( val );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Invalid HostNameList" + e );
        }
    }


}
