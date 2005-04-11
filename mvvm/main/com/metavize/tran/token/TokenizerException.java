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

package com.metavize.tran.token;

public class TokenizerException extends Exception
{
    public TokenizerException()
    {
        super();
    }

    public TokenizerException(String message)
    {
        super(message);
    }

    public TokenizerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public TokenizerException(Throwable cause)
    {
        super(cause);
    }
}
