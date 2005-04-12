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

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.tran.token.AbstractTokenizer;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.TokenizerException;
import com.metavize.tran.token.TokenizerResult;
import org.apache.log4j.Logger;

public class FtpClientTokenizer extends AbstractTokenizer
{
    private static final char SP = '\r';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private final FtpCasing casing;

    private final Logger logger = Logger.getLogger(FtpClientTokenizer.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    FtpClientTokenizer(FtpCasing casing)
    {
        this.casing = casing;
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

            String arg = new String(i, ba.length - i);

            FtpCommand cmd = new FtpCommand(fn, arg);

            return new TokenizerResult(cmd, null);
        } else {
            buf.compact();
            return new TokenizerResult(null, buf);
        }
    }

    public TokenStreamer endSession() { }

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
        return buf.remaining() >= 2
            && buf.get(l - 2) == CR && buf.get(l - 1) == LF;
    }
}
