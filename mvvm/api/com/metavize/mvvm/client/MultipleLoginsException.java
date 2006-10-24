/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.client;

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.security.LoginSession;

public class MultipleLoginsException extends FailedLoginException
{
    private final LoginSession otherLogin;

    public MultipleLoginsException(LoginSession otherLogin)
    {
        this.otherLogin = otherLogin;
    }

    public LoginSession getOtherLogin()
    {
        return otherLogin;
    }
}
