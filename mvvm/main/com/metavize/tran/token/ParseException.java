/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ParseException.java,v 1.1.1.1 2004/12/01 23:32:23 amread Exp $
 */

package com.metavize.tran.token;

public class ParseException extends Exception
{
    public ParseException()
    {
        super();
    }

    public ParseException(String message)
    {
        super(message);
    }

    public ParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public ParseException(Throwable cause)
    {
        super(cause);
    }
}
