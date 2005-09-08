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

package com.metavize.mvvm.security;

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.client.MultipleLoginsException;
import com.metavize.mvvm.client.MvvmRemoteContext;

public interface MvvmLogin
{
    MvvmRemoteContext interactiveLogin(String username, String password,
                                       boolean force)
        throws FailedLoginException, MultipleLoginsException;

    MvvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException;
}
