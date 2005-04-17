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
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;

public class FtpClientParser extends AbstractParser
{
    private static final char SP = ' ';
    private static final char CR = '\r';
    private static final char LF = '\n';

    FtpClientParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(true);
    }

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        if (completeLine(buf)) {
            byte[] ba = new byte[buf.remaining()];
            buf.get(ba);

            int i = Character.isWhitespace((char)ba[3]) ? 3 : 4;
            String fnStr = new String(ba, 0, i);
            FtpFunction fn = FtpFunction.valueOf(fnStr.toUpperCase());
            if (null == fn) {
                throw new ParseException("Unknown FTP function: " + fnStr);
            }

            while (SP == ba[++i]);

            String arg = (ba.length - 2 <= i) ? null
                : new String(ba, i, ba.length - i - 2); // no CRLF

            FtpCommand cmd = new FtpCommand(fn, arg);

            return new ParseResult(new Token[] { cmd }, null);
        } else {
            return new ParseResult(null, buf);
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
