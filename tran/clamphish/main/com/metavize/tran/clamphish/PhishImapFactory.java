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

package com.metavize.tran.clamphish;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

public class PhishImapFactory implements TokenHandlerFactory
{
    private static final Logger logger = Logger.getLogger(PhishImapFactory.class);

    private final ClamPhishTransform transform;

    // constructors -----------------------------------------------------------

    PhishImapFactory(ClamPhishTransform transform)
    {
        this.transform = transform;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new PhishImapHandler(session, transform);
    }
}
