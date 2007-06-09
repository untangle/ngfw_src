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

package com.untangle.mvvm.tapi;

public class MPipeException extends Exception
{
    protected transient MPipe myMPipe;

    public MPipeException(MPipe myMPipe)
    {
        this.myMPipe = myMPipe;
    }

    public MPipeException(MPipe myMPipe, String s)
    {
        super(s);
        this.myMPipe = myMPipe;
    }

    public MPipeException(MPipe myMPipe, Throwable e)
    {
        super(e);
        this.myMPipe = myMPipe;
    }

    public MPipeException(MPipe myMPipe, String s, Throwable e)
    {
        super(s,e);
        this.myMPipe = myMPipe;
    }


}
