/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: HttpBlockerFactory.java,v 1.2 2005/01/28 10:27:31 amread Exp $
 */

package com.metavize.tran.httpblocker;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class HttpBlockerFactory implements TokenHandlerFactory
{
    private static final Logger logger = Logger
        .getLogger(HttpBlockerFactory.class);

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

    public TokenHandler tokenHandler(TCPSession session)
    {

        return new HttpBlockerHandler(session, transform);
    }
}
