/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusSmtpFactory.java 194 2005-04-06 19:13:55Z rbscott $
 */
package com.metavize.tran.virus;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class VirusSmtpFactory implements TokenHandlerFactory
{
    private final VirusTransformImpl transform;

    VirusSmtpFactory(VirusTransformImpl transform)
    {
        this.transform = transform;
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusSmtpHandler(session, transform);
    }
}
