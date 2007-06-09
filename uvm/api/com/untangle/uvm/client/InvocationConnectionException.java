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
 * Signals problem connecting to server.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class InvocationConnectionException extends InvocationException
{
    public InvocationConnectionException()
    {
        super();
    }

    public InvocationConnectionException(String message)
    {
        super(message);
    }

    public InvocationConnectionException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationConnectionException(Throwable cause)
    {
        super(cause);
    }
}
