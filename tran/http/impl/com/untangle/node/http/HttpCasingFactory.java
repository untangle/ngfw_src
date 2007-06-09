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

package com.untangle.tran.http;

import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.Casing;
import com.untangle.tran.token.CasingFactory;

class HttpCasingFactory implements CasingFactory
{
    private final HttpTransformImpl transform;


    public HttpCasingFactory(HttpTransformImpl transform)
    {
        this.transform = transform;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new HttpCasing(session, clientSide, transform);
    }
}
