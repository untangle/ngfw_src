/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.type;

import com.metavize.mvvm.tran.HostName;

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

    public Object createUserType( String val )
    {
        try {
            return HostName.parse( val );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Invalid HostName" + e );
        }
    }
}
