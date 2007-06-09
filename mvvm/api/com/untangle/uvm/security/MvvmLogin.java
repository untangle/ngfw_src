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

package com.untangle.mvvm.security;

import javax.security.auth.login.FailedLoginException;

import com.untangle.mvvm.client.MultipleLoginsException;
import com.untangle.mvvm.client.MvvmRemoteContext;

public interface MvvmLogin
{
    boolean isActivated();

    MvvmRemoteContext activationLogin(String key)
        throws FailedLoginException, MultipleLoginsException;

    MvvmRemoteContext interactiveLogin(String username, String password,
                                       boolean force)
        throws FailedLoginException, MultipleLoginsException;

    MvvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException;
}
