/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusPopFactory.java 194 2005-04-06 19:13:55Z rbscott $
 */
package com.metavize.tran.virus;


import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.TokenHandler;
import com.metavize.tran.token.TokenHandlerFactory;

public class VirusPopFactory implements TokenHandlerFactory
{
    private final VirusTransformImpl transform;

    VirusPopFactory(VirusTransformImpl transform)
    {
        this.transform = transform;
    }

    public TokenHandler tokenHandler(TCPSession session)
    {
        return new VirusPopHandler(session, transform);
    }
}
