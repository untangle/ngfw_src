/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: UnparseException.java,v 1.1.1.1 2004/12/01 23:32:23 amread Exp $
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
