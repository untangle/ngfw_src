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

package com.metavize.tran.spyware;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

class SpywareHttpFactory implements TokenHandlerFactory
{
    private static final Logger logger = Logger.getLogger(SpywareHttpFactory.class);

    private final SpywareImpl transform;

    // constructors -----------------------------------------------------------

    SpywareHttpFactory(SpywareImpl transform)
    {
        this.transform = transform;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpywareHttpHandler(session, transform);
    }
}
