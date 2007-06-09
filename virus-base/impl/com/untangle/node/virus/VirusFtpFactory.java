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

package com.untangle.node.virus;

import com.untangle.uvm.tapi.TCPNewSessionRequest;
import com.untangle.uvm.tapi.TCPSession;
import com.untangle.node.token.TokenHandler;
import com.untangle.node.token.TokenHandlerFactory;

public class VirusFtpFactory implements TokenHandlerFactory
{
    private final VirusNodeImpl node;

    VirusFtpFactory(VirusNodeImpl node)
    {
        this.node = node;
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusFtpHandler(session, node);
    }

    public void handleNewSessionRequest(TCPNewSessionRequest tsr)
    {
    }
}
