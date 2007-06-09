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

package com.untangle.node.httpblocker;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class HttpBlockerFactory implements TokenHandlerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final HttpBlockerImpl node;

    // constructors -----------------------------------------------------------

    HttpBlockerFactory(HttpBlockerImpl node)
    {
        this.node = node;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public boolean isTokenSession(TCPSession se)
    {
        return true;
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new HttpBlockerHandler(session, node);
    }
}
