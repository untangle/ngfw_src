/*
 * Copyright (c) 2003 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MPipeEvent.java,v 1.2 2005/01/03 02:22:57 jdi Exp $
 */

package com.metavize.mvvm.tapi.event;

import com.metavize.mvvm.tapi.MPipe;

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
