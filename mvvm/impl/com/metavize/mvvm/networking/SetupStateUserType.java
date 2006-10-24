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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.type.IntBasedUserType;

public class SetupStateUserType extends IntBasedUserType
{
    public Class returnedClass()
    {
        return SetupState.class;
    }

    protected int userTypeToInt( Object v )
    {
        return ((SetupState)v).getType();
    }

    public Object createUserType( int val ) throws Exception
    {
        return SetupState.getInstance( val );
    }
}
