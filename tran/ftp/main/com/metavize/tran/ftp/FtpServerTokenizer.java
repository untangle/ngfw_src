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

public class FtpServerTokenizer extends AbstractTokenizer
{
    private static final char SP = ' ';
    private static final char HYPHEN = '-';
    private static final char CR = '\r';
    private static final char LF = '\n';

    FtpServerTokenizer(TCPSession session)
    {
        super(session, false);
        lineBuffering(true);
    }

    public TokenizerResult tokenize(ByteBuffer buf) throws TokenizerException
    {
        ByteBuffer dup = buf.duplicate();

        if (completeReply(dup)) {
            int replyCode = replyCode(dup);

            if (-1 == replyCode) {
                throw new TokenizerException("expected reply code");
            }

            switch (dup.get()) {
            case SP: {
                byte[] ba = new byte[dup.remaining()];
                dup.get(ba);
                String message = new String(ba);

                FtpReply reply = new FtpReply(replyCode, message);
                return new TokenizerResult(new Token[] { reply }, null);
            }

            case HYPHEN: {
                int i = dup.limit() - 2;
                while (3 < --i && LF != dup.get(i));

                if (LF != dup.get(i++)) {
                    return new TokenizerResult(null, buf);
                }

                ByteBuffer end = dup.duplicate();
                end.position(i);
                end.limit(end.limit() - 2);
                int endCode = replyCode(end);

                if (-1 == endCode || HYPHEN != end.get()) {
                    return new TokenizerResult(null, buf);
                }

                byte[] mb = new byte[dup.remaining() + end.remaining()];

                dup.get(mb);
                end.get(mb, dup.remaining(), end.remaining());
                String message = new String(mb);

                FtpReply reply = new FtpReply(replyCode, message);
                return new TokenizerResult(new Token[] { reply }, null);
            }

            default:
                throw new TokenizerException("expected a space");
            }

        } else {
            return new TokenizerResult(null, buf);
        }
    }

    public TokenStreamer endSession() { return null; }

    // private methods --------------------------------------------------------

    private int replyCode(ByteBuffer buf)
    {
        int i = 0;

        byte c = buf.get();
        if (48 <= c && 57 >= c) {
            i = (c - 48) * 100;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48) * 10;
        } else {
            return -1;
        }

        c = buf.get();
        if (48 <= c && 57 >= c) {
            i += (c - 48);
        } else {
            return -1;
        }

        return i;
    }

    /**
     * Checks if the buffer contains a complete reply.
     *
     * @param buf to check.
     * @return true if a complete line.
     */
    private boolean completeReply(ByteBuffer buf)
    {
        int l = buf.limit();
        return buf.remaining() >= 2 && buf.get(l - 2) == CR
            && buf.get(l - 1) == LF;
    }
}
