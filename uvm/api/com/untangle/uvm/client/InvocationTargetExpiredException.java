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
 * Signals that remote target no longer exists.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class InvocationTargetExpiredException extends InvocationException
{
    public InvocationTargetExpiredException()
    {
        super();
    }

    public InvocationTargetExpiredException(String message)
    {
        super(message);
    }

    public InvocationTargetExpiredException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public InvocationTargetExpiredException(Throwable cause)
    {
        super(cause);
    }
}
