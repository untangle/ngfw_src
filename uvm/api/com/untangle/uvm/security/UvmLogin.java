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

package com.untangle.uvm.security;

import javax.security.auth.login.FailedLoginException;

import com.untangle.uvm.client.MultipleLoginsException;
import com.untangle.uvm.client.UvmRemoteContext;

public interface UvmLogin
{
    boolean isActivated();

    UvmRemoteContext activationLogin(String key)
        throws FailedLoginException, MultipleLoginsException;

    UvmRemoteContext interactiveLogin(String username, String password,
                                       boolean force)
        throws FailedLoginException, MultipleLoginsException;

    UvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException;
}
