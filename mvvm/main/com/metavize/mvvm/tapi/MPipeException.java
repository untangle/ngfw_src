/*
 * Copyright (c) 2003-2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id: MPipeException.java,v 1.2 2005/01/30 09:20:31 amread Exp $
 */

package com.metavize.mvvm.tapi;

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
