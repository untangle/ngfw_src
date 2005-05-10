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

package com.metavize.mvvm.engine;

import java.io.Serializable;

import com.metavize.mvvm.security.LoginSession;

class HttpInvocation implements Serializable
{
    private static final long serialVersionUID = 3529724701056849106L;

    final LoginSession loginSession;
    final Integer targetId;
    final String methodSignature;
    final String definingClass;

    HttpInvocation(LoginSession loginSession, Integer targetId,
                   String methodSignature, String definingClass)
    {
        this.loginSession = loginSession;
        this.targetId = targetId;
        this.methodSignature = methodSignature;
        this.definingClass = definingClass;
    }
}
