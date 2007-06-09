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

package com.untangle.uvm;

/**
 * Base exception class for UVM.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class UvmException extends Exception
{
    public UvmException()
    {
        super();
    }

    public UvmException(String message)
    {
        super(message);
    }

    public UvmException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UvmException(Throwable cause)
    {
        super(cause);
    }
}
