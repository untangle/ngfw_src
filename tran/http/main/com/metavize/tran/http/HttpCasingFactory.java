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

package com.metavize.tran.http;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;

class HttpCasingFactory implements CasingFactory
{
    private static final Object LOCK = new Object();

    private static HttpCasingFactory HTTP_CASING_FACTORY;

    private HttpCasingFactory() { }

    static HttpCasingFactory factory()
    {
        synchronized (LOCK) {
            if (null == HTTP_CASING_FACTORY) {
                HTTP_CASING_FACTORY = new HttpCasingFactory();
            }
        }

        return HTTP_CASING_FACTORY;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new HttpCasing(session, clientSide);
    }
}
