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

package com.untangle.node.token;

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
