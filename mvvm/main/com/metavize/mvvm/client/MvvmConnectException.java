/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.client;

/**
 * Exception for connecting to the MVVM remotely.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class MvvmConnectException extends Exception
{
    public MvvmConnectException()
    {
        super();
    }

    public MvvmConnectException(String message)
    {
        super(message);
    }

    public MvvmConnectException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public MvvmConnectException(Throwable cause)
    {
        super(cause);
    }
}
