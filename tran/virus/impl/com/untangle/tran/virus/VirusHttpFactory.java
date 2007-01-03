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
package com.untangle.tran.virus;


import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;

public class VirusHttpFactory implements TokenHandlerFactory
{
    private final VirusTransformImpl transform;

    VirusHttpFactory(VirusTransformImpl transform)
    {
        this.transform = transform;
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusHttpHandler(session, transform);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
