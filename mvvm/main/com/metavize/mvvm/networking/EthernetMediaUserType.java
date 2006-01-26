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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.type.IntBasedUserType;

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
