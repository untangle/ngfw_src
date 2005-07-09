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

public class SpamImapFactory implements TokenHandlerFactory
{
    public static final SpamImapFactory FACTORY = new SpamImapFactory();

    private static final Logger logger = Logger.getLogger(SpamImapFactory.class);

    // constructors -----------------------------------------------------------

    private SpamImapFactory() { }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new SpamImapHandler(session);
    }
}
