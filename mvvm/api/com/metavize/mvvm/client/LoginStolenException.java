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

import com.metavize.mvvm.security.LoginSession;

public class LoginStolenException extends InvocationException
{
    private final LoginSession thief;

    public LoginStolenException(LoginSession thief)
    {
        this.thief = thief;
    }

    public LoginSession getThief()
    {
        return thief;
    }
}
