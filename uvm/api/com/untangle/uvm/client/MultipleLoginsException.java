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

package com.untangle.uvm.client;

import javax.security.auth.login.FailedLoginException;

import com.untangle.uvm.security.LoginSession;

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
