/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpInvocation.java,v 1.3 2005/02/25 02:45:29 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.Serializable;
import java.net.URL;

import com.metavize.mvvm.security.LoginSession;


class HttpInvocation implements Serializable
{
    private static final long serialVersionUID = -192623323581233139L;

    LoginSession loginSession;
    URL url;
    Object cacheId;
    String methodSignature;
    String definingClass;

    HttpInvocation(LoginSession loginSession, URL url, Object cacheId,
                   String methodSignature, String definingClass)
    {
        this.loginSession = loginSession;
        this.url = url;
        this.cacheId = cacheId;
        this.methodSignature = methodSignature;
        this.definingClass = definingClass;
    }
}
