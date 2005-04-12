/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractUntokenizer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.UntokenizerResult;

class FtpUntokenizer extends AbstractUntokenizer
{
    private final byte[] CRLF = new byte[] { 13, 10 };

    public FtpUntokenizer(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UntokenizerResult untokenize(Token token)
    {
        return new UntokenizerResult(new ByteBuffer[] { token.getBytes() });
    }

    public TokenStreamer endSession() { return null; }
}
