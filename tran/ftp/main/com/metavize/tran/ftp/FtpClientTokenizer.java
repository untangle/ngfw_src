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
import com.metavize.tran.token.AbstractTokenizer;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.TokenizerException;
import com.metavize.tran.token.TokenizerResult;

public class FtpClientTokenizer extends AbstractTokenizer
{
    private static final char SP = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    FtpClientTokenizer(TCPSession session)
    {
        super(session, true);
        lineBuffering(true);
    }

    public TokenizerResult tokenize(ByteBuffer buf) throws TokenizerException
    {
        if (completeLine(buf)) {
            byte[] ba = new byte[buf.remaining()];
            buf.get(ba);

            int i = SP == ba[3] ? 3 : 4;
            String fnStr = new String(ba, 0, i);
            FtpFunction fn = FtpFunction.getInstance(fnStr);
            if (null == fn) {
                throw new TokenizerException("Unknown FTP function: " + fnStr);
            }

            while (SP == ba[++i]);

            String arg = new String(ba, i, ba.length - i - 2); // no CRLF

            FtpCommand cmd = new FtpCommand(fn, arg);

            return new TokenizerResult(new Token[] { cmd }, null);
        } else {
            return new TokenizerResult(null, buf);
        }
    }

    public TokenStreamer endSession() { return null; }

    // private methods --------------------------------------------------------

    /**
     * True the buffer contains a complete line <CRLF>. Obsolete line
     * terminators are not accepted.
     *
     * @param buf to check.
     * @return true if a complete line.
     */
    private boolean completeLine(ByteBuffer buf)
    {
        int l = buf.limit();

        return buf.remaining() >= 2 && buf.get(l - 2) == CR
            && buf.get(l - 1) == LF;
    }
}
