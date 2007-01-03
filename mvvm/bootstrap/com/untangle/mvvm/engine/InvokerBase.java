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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public abstract class InvokerBase
{
    public static boolean GZIP_RESPONSE = true;

    protected InvokerBase() { }

    protected abstract void handleStream(InputStream is, OutputStream os,
                                         boolean isLocal,
                                         InetAddress remoteAddr);

    public void handle(InputStream is, OutputStream os, boolean isLocal,
                       InetAddress remoteAddr)
    {
        handleStream(is, os, isLocal, remoteAddr);
    }
}
