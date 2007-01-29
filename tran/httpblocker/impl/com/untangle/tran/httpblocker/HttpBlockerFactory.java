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

package com.untangle.tran.httpblocker;

import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class HttpBlockerFactory implements TokenHandlerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final HttpBlockerImpl transform;

    // constructors -----------------------------------------------------------

    HttpBlockerFactory(HttpBlockerImpl transform)
    {
        this.transform = transform;
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
        return new HttpBlockerHandler(session, transform);
    }
}
