/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FtpCasingFactory.java,v 1.1 2004/12/10 23:27:47 amread Exp $
 */

package com.metavize.tran.ftp;

import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;


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

    public Casing casing(boolean inside)
    {
        return new FtpCasing(inside);
    }
}
