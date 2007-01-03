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

package com.untangle.tran.token;

import com.untangle.mvvm.tapi.Session;
import com.untangle.mvvm.tran.MutateTStats;
import org.apache.log4j.Logger;

import static com.untangle.tran.token.CasingAdaptor.TOKEN_SIZE;

class TokenStreamerWrapper implements TokenStreamer
{
    private final Logger logger = Logger.getLogger(getClass());

    private final TokenStreamer tokenStreamer;
    private final Session session;
    private final int direction;

    TokenStreamerWrapper(TokenStreamer tokenStreamer, Session session, int direction)
    {
        this.tokenStreamer = tokenStreamer;
        this.session = session;
        this.direction = direction;
    }

    public Token nextToken()
    {
        Token token = tokenStreamer.nextToken();

        if (null != token) {
            try {
                MutateTStats.rewroteData(direction, session,
                                         token.getEstimatedSize() - TOKEN_SIZE);
            } catch (Exception exn) {
                logger.warn("could not estimate size", exn);
            }
        }

        return token;
    }

    public boolean closeWhenDone()
    {
        return tokenStreamer.closeWhenDone();
    }
}
