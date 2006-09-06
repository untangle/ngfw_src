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

package com.metavize.mvvm.client;

public class LoginExpiredException extends InvocationException
{
    public LoginExpiredException() { super(); }
    public LoginExpiredException(String message) { super(message); }
    public LoginExpiredException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public LoginExpiredException(Throwable cause) { super(cause); }
}
