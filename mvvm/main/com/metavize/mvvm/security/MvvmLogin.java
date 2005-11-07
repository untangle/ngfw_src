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
    boolean isActivated();

    /**
     * Activates the EdgeGuard using the given key. 
     *
     * @param key a <code>String</code> giving the key to be activated under
     * @return a <code>MvvmRemoteContext</code> value
     * @exception FailedLoginException if the key isn't kosher or the product has already been activated
     */
    MvvmRemoteContext activationLogin(String key)
        throws FailedLoginException, MultipleLoginsException;

    MvvmRemoteContext interactiveLogin(String username, String password,
                                       boolean force)
        throws FailedLoginException, MultipleLoginsException;

    MvvmRemoteContext systemLogin(String username, String password)
        throws FailedLoginException;
}
