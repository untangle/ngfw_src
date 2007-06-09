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

package com.untangle.node.router;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class RouterFtpFactory implements TokenHandlerFactory
{
    private final RouterImpl node;

    RouterFtpFactory( RouterImpl node )
    {
        this.node = node;
    }

    public TokenHandler tokenHandler( TCPSession session )
    {
        return new RouterFtpHandler( session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
