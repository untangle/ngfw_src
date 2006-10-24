/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.token;

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
