/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.security;

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.client.MultipleLoginsException;
import com.metavize.mvvm.client.MvvmRemoteContext;

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
