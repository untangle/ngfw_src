/*
 * Copyright (c) 2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpCasingFactory.java,v 1.1 2004/12/10 23:27:47 amread Exp $
 */

package com.metavize.tran.http;

import com.metavize.tran.token.Casing;
import com.metavize.tran.token.CasingFactory;
import com.metavize.tran.token.Parser;
import com.metavize.tran.token.Unparser;

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

    public Casing casing(boolean inside)
    {
        return new HttpCasing(inside);
    }
}
