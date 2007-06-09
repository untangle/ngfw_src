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

package com.untangle.uvm.client;

/**
 * Exception for connecting to the UVM remotely.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmConnectException extends Exception
{
    public UvmConnectException()
    {
        super();
    }

    public UvmConnectException(String message)
    {
        super(message);
    }

    public UvmConnectException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UvmConnectException(Throwable cause)
    {
        super(cause);
    }
}
