/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: Invocation.java,v 1.1.1.1 2004/12/01 23:32:21 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.Serializable;
import java.net.InetAddress;


class Invocation implements Serializable
{
    private static final long serialVersionUID = 2214647488241821642L;

    InetAddress serverAddress;
    Object cacheId;
    String methodSignature;
    String definingClass;
    Object[] args;

    Invocation(InetAddress serverAddress, Object cacheId,
               String methodSignature, String definingClass, Object[] args)
    {
        this.serverAddress = serverAddress;
        this.cacheId = cacheId;
        this.methodSignature = methodSignature;
        this.definingClass = definingClass;
        this.args = args;
    }
}
