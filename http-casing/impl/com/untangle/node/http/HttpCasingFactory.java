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

package com.untangle.node.http;

import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.Casing;
import com.untangle.node.token.CasingFactory;

class HttpCasingFactory implements CasingFactory
{
    private final HttpNodeImpl node;


    public HttpCasingFactory(HttpNodeImpl node)
    {
        this.node = node;
    }

    public Casing casing(TCPSession session, boolean clientSide)
    {
        return new HttpCasing(session, clientSide, node);
    }
}
