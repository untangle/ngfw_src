/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: InvokerBase.java,v 1.1 2005/01/14 07:59:45 amread Exp $
 */

package com.metavize.mvvm.engine;

import java.io.InputStream;
import java.io.OutputStream;

public abstract class InvokerBase
{
    protected InvokerBase() { }

    protected abstract void handleStream(InputStream is, OutputStream os,
                                         boolean isLocal);

    public void handle(InputStream is, OutputStream os, boolean isLocal)
    {
        handleStream(is, os, isLocal);
    }
}
