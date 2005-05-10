/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MvvmLoginException.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.security;

/**
 * Exception for login failures.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class LoginExpiredException extends MvvmLoginException
{
    public LoginExpiredException()
    {
        super();
    }

    public LoginExpiredException(String message)
    {
        super(message);
    }
}
