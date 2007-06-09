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

import com.untangle.uvm.node.firewall.MACAddress;

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
