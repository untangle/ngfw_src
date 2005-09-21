/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

public class MackageException extends MvvmException
{
    public MackageException()
    {
        super();
    }

    public MackageException(String message)
    {
        super(message);
    }

    public MackageException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MackageException(Throwable cause)
    {
        super(cause);
    }
}
