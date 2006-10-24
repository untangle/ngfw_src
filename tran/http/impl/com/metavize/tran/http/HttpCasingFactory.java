/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
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
