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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

public abstract class InvokerBase
{
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
