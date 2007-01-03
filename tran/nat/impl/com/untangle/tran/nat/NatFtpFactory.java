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

package com.untangle.tran.nat;

import com.untangle.mvvm.tapi.TCPNewSessionRequest;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.TokenHandler;
import com.untangle.tran.token.TokenHandlerFactory;

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
