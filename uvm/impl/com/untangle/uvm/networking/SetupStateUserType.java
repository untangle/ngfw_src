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

package com.untangle.uvm.networking;

import com.untangle.uvm.type.IntBasedUserType;

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
