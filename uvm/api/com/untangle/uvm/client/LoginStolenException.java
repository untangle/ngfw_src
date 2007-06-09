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

import com.untangle.uvm.security.LoginSession;

/**
 * Signals that this login session has been terminated because another
 * user has logged in. To allow multiple simultaneous logins, set the
 * system property <code>mvvm.login.multiuser</code> to <code>true</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
