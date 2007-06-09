/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: PhishHttpFactory.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.node.clamphish;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;
import org.apache.log4j.Logger;

class PhishHttpFactory implements TokenHandlerFactory
{
    private final Logger logger = Logger.getLogger(getClass());

    private final ClamPhishNode node;

    // constructors -----------------------------------------------------------

    PhishHttpFactory(ClamPhishNode node)
    {
        this.node = node;
    }

    // TokenHandlerFactory methods --------------------------------------------

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new PhishHttpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
