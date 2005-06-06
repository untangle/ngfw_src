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

package com.metavize.tran.nat;

import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;
import com.metavize.mvvm.tapi.TCPSession;

import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenException;
import com.metavize.tran.token.TokenResult;

import com.metavize.tran.ftp.FtpCommand;
import com.metavize.tran.ftp.FtpFunction;
import com.metavize.tran.ftp.FtpCommand;
import com.metavize.tran.ftp.FtpReply;
import com.metavize.tran.ftp.FtpStateMachine;


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
}
