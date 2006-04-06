/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import com.metavize.mvvm.tapi.TCPNewSessionRequest;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class NatFtpFactory implements TokenHandlerFactory
{
    private final NatImpl transform;

    NatFtpFactory( NatImpl transform )
    {
        this.transform = transform;
    }

    public TokenHandler tokenHandler( TCPSession session )
    {
        return new NatFtpHandler( session, transform);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
