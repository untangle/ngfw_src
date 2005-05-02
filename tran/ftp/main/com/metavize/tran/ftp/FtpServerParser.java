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
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

public class FtpServerParser extends AbstractParser
{
    private static final char SP = ' ';
    private static final char HYPHEN = '-';
    private static final char CR = '\r';
    private static final char LF = '\n';

    private static final String CRLF = "\r\n";

    private final Fitting fitting;

    private final Logger logger = Logger.getLogger(FtpServerParser.class);

    FtpServerParser(TCPSession session)
    {
        super(session, false);
        lineBuffering(true);

        Pipeline p = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
        fitting = p.getServerFitting(session.mPipe());
    }

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        if (Fitting.FTP_CTL_STREAM == fitting) {
            return parseServerCtl(buf);
        } else if (Fitting.FTP_DATA_STREAM == fitting) {
            return parseServerData(buf);
        } else {
            throw new IllegalStateException("bad input fitting: " + fitting);
        }
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (Fitting.FTP_DATA_STREAM == fitting) {
            return new ParseResult(new Token[] { EndMarker.MARKER }, null);
        } else {
            if (buf.hasRemaining()) {
                logger.warn("unread data in read buffer: " + buf.remaining());
            }
            return new ParseResult(null, null);
        }
    }

    public TokenStreamer endSession() { return null; }

    // private methods --------------------------------------------------------

    private ParseResult parseServerCtl(ByteBuffer buf) throws ParseException
    {
        ByteBuffer dup = buf.duplicate();

        if (completeLine(dup)) {
            int replyCode = replyCode(dup);

            if (-1 == replyCode) {
                throw new ParseException("expected reply code");
            }

            switch (dup.get()) {
            case SP: {
                byte[] ba = new byte[dup.remaining()];
                dup.get(ba);
                String message = new String(ba);

                FtpReply reply = new FtpReply(replyCode, message);

                return new ParseResult(new Token[] { reply }, null);
            }

            case HYPHEN: {
                int i = dup.limit() - 2;
                while (3 < --i && LF != dup.get(i));

                if (LF != dup.get(i++)) {
                    return new ParseResult(null, buf.compact());
                }

                ByteBuffer end = dup.duplicate();
                end.position(i);
                end.limit(end.limit() - 2);
                int endCode = replyCode(end);

                if (-1 == endCode || SP != end.get()) {
                    return new ParseResult(null, buf.compact());
                }

                StringBuffer sb = new StringBuffer(buf.remaining());

                while (buf.hasRemaining()) {
                    byte b;

                    for (int j = 0; j < 3; j++) {
                        if (!Character.isDigit(buf.get())) {
                            throw new ParseException("digit expected");
                        }
                    }

                    if (SP != (b = buf.get()) && HYPHEN != b) {
                        throw new ParseException("space or hyphen expected");
                    }

                    while (CR != (b = buf.get())) {
                        sb.append((char)b);
                    }

                    if (LF != (b = buf.get())) {
                        throw new ParseException("LF expected");
                    }

                    if (buf.hasRemaining()) {
                        sb.append(CRLF);
                    }
                }

                String message = sb.toString();

                FtpReply reply = new FtpReply(replyCode, message);

                return new ParseResult(new Token[] { reply }, null);
            }

            default:
                throw new ParseException("expected a space");
            }

        } else {
            return new ParseResult(null, buf.compact());
        }
    }

    private ParseResult parseServerData(ByteBuffer buf) throws ParseException
    {
        Chunk c = new Chunk(buf.duplicate());
        return new ParseResult(new Token[] { c }, null);
    }

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
     * Checks if the buffer contains a complete line.
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
