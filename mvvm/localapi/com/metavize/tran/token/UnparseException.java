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

package com.metavize.tran.token;

public class UnparseException extends Exception
{
    public UnparseException()
    {
        super();
    }

    public UnparseException(String message)
    {
        super(message);
    }

    public UnparseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UnparseException(Throwable cause)
    {
        super(cause);
    }
}
