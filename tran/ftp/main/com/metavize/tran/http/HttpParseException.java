/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpParseException.java,v 1.1 2004/12/10 23:27:47 amread Exp $
 */

package com.metavize.tran.http;

import com.metavize.tran.token.ParseException;

public class HttpParseException extends ParseException
{
    public HttpParseException()
    {
        super();
    }

    public HttpParseException(String message)
    {
        super(message);
    }

    public HttpParseException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public HttpParseException(Throwable cause)
    {
        super(cause);
    }
}
