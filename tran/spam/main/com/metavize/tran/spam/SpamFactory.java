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

public class SpamFactory implements TokenHandlerFactory
{
    private static final Logger logger = Logger.getLogger(SpamFactory.class);

    // constructors -----------------------------------------------------------

    SpamFactory() { }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamSmtpHandler(session);
    }
}
