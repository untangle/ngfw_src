/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: MvvmLogin.java,v 1.1.1.1 2004/12/01 23:32:22 amread Exp $
 */

package com.metavize.mvvm.security;

import javax.security.auth.login.FailedLoginException;

import com.metavize.mvvm.MvvmContext;

public interface MvvmLogin
{
    public MvvmContext login(String username, String password)
        throws FailedLoginException;
}
