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

import com.metavize.mvvm.tran.firewall.MACAddress;

public class MACAddressUserType extends StringBasedUserType
{
    public Class returnedClass()
    {
        return MACAddress.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((MACAddress)v).toString();
    }

    public Object createUserType( String val )
    {
        try {
            return MACAddress.parse( val );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Invalid MACAddress: " + e );
        }
    }
}
