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

package com.untangle.mvvm.networking;

import com.untangle.mvvm.type.IntBasedUserType;

public class EthernetMediaUserType extends IntBasedUserType
{
    public Class returnedClass()
    {
        return EthernetMedia.class;
    }

    protected int userTypeToInt( Object v )
    {
        return ((EthernetMedia)v).getType();
    }

    public Object createUserType( int val ) throws Exception
    {
        return EthernetMedia.getInstance( val );
    }
}
