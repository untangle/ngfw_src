/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: TokenEvent.java,v 1.5 2005/01/14 00:41:20 amread Exp $
 */

package com.metavize.tran.token;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;

public class TokenEvent
{
    private TCPSessionEvent event;
    private Token token;

    public TokenEvent(TCPSessionEvent event, Token token)
    {
        this.event = event;
        this.token = token;
    }

    public TCPSessionEvent sessionEvent()
    {
        return event;
    }

    public TCPSession session()
    {
        return event.session();
    }

    public Token token()
    {
        return token;
    }
}
