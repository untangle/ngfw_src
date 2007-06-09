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

package com.untangle.tran.ftp;

import com.untangle.tran.token.Casing;
import com.untangle.tran.token.CasingFactory;
import com.untangle.mvvm.tapi.TCPSession;


class FtpCasingFactory implements CasingFactory
{
    private static final Object LOCK = new Object();

    private static FtpCasingFactory FTP_CASING_FACTORY;

    private FtpCasingFactory() { }

    static FtpCasingFactory factory()
    {
        synchronized (LOCK) {
            if (null == FTP_CASING_FACTORY) {
                FTP_CASING_FACTORY = new FtpCasingFactory();
            }
        }

        return FTP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean inside)
    {
        return new FtpCasing(session, inside);
    }
}
