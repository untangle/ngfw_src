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

package com.metavize.mvvm.tran;

import com.metavize.mvvm.MvvmException;

public class TransformException extends MvvmException
{
    public TransformException()
    {
        super();
    }

    public TransformException(String message)
    {
        super(message);
    }

    public TransformException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TransformException(Throwable cause)
    {
        super(cause);
    }
}

