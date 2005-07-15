/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class SpamPopFactory implements TokenHandlerFactory
{
    private static final Logger logger = Logger.getLogger(SpamPopFactory.class);

    private final SpamImpl transform;

    // constructors -----------------------------------------------------------

    SpamPopFactory(SpamImpl transform) {
        this.transform = transform;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamPopHandler(session, transform);
    }
}
