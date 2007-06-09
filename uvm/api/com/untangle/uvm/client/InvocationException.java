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
 * Signals problem making RPC call.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class InvocationException extends RuntimeException
{
    public InvocationException()
    {
        super();
    }

    public InvocationException(String message)
    {
        super(message);
    }

    public InvocationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationException(Throwable cause)
    {
        super(cause);
    }
}
