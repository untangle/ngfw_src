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

package com.untangle.mvvm;

/**
 * Base exception class for MVVM.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmException extends Exception
{
    public MvvmException()
    {
        super();
    }

    public MvvmException(String message)
    {
        super(message);
    }

    public MvvmException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MvvmException(Throwable cause)
    {
        super(cause);
    }
}
