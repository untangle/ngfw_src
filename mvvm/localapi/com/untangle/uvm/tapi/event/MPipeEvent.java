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

package com.untangle.mvvm.tapi.event;

import com.untangle.mvvm.tapi.MPipe;

public abstract class MPipeEvent extends java.util.EventObject
{
    private MPipe mPipe;

    protected MPipeEvent(MPipe mPipe, Object source)
    {
        super(source);
        this.mPipe = mPipe;
    }

    public MPipe mPipe() 
    {
        return mPipe;
    }
}
