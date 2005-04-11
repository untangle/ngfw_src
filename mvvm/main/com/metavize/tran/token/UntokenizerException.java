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

public class UntokenizerException extends Exception
{
    public UntokenizerException()
    {
        super();
    }

    public UntokenizerException(String message)
    {
        super(message);
    }

    public UntokenizerException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public UntokenizerException(Throwable cause)
    {
        super(cause);
    }
}
