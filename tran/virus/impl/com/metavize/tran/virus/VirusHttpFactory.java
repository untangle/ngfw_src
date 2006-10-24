/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.virus;


import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

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
