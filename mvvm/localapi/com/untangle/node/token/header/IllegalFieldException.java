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

package com.untangle.tran.token.header;

public class IllegalFieldException extends Exception
{
    public IllegalFieldException(String message)
    {
        super(message);
    }

    public IllegalFieldException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
