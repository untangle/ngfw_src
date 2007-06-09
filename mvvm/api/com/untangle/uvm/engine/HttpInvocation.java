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

package com.untangle.mvvm.engine;

import java.io.Serializable;

import com.untangle.mvvm.security.LoginSession;
import java.net.URL;

class HttpInvocation implements Serializable
{
    private static final long serialVersionUID = 3529724701056849106L;

    final LoginSession loginSession;
    final Integer targetId;
    final String methodSignature;
    final String definingClass;
    final URL url;
    final int timeout;

    HttpInvocation(LoginSession loginSession, Integer targetId,
                   String methodSignature, String definingClass,
                   URL url, int timeout)
    {
        this.loginSession = loginSession;
        this.targetId = targetId;
        this.methodSignature = methodSignature;
        this.definingClass = definingClass;
        this.url = url;
        this.timeout = timeout;
    }
}
