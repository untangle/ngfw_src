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

package com.metavize.tran.ftp;

import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;
import com.metavize.mvvm.tapi.TCPSession;


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
