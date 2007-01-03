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

package com.untangle.mvvm.client;

import com.untangle.mvvm.security.LoginSession;

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
