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

public class TokenException extends Exception
{
    public TokenException()
    {
        super();
    }

    public TokenException(String message)
    {
        super(message);
    }

    public TokenException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TokenException(Throwable cause)
    {
        super(cause);
    }
}
