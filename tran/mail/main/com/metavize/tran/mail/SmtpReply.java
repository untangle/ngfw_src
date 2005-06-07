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

package com.metavize.tran.mail;

import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

/**
 * SMTP server reply to a command.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 2821, Section 4.2"
 */
public class SmtpReply implements Token
{
    private static final Pattern LINE_SPLITTER = Pattern.compile("\r\n");

    private final int replyCode;
    private final String message;

    private final Logger logger = Logger.getLogger(SmtpReply.class);

    public SmtpReply(int replyCode, String message)
    {
        this.replyCode = replyCode;
        this.message = message;
    }

    // static factories ------------------------------------------------------

    public static SmtpReply parse(ByteBuffer buf) throws ParseException
    {
        Logger logger = Logger.getLogger(SmtpReply.class);

        StringBuilder sb = new StringBuilder();
        int replyCode = -1;

        for (boolean parsingReply = true; parsingReply; ) {
            logger.debug("getting replyCode");
            int rc = replyCode(buf);

            if (0 > replyCode) { replyCode = rc; }

            if (-1 == replyCode) {
                throw new ParseException("expected reply code");
            } else if (rc != replyCode) {
                throw new ParseException("reply code mismatch");
            }

            switch (buf.get()) {
            case SP:
                logger.debug("got space");
                parsingReply = false;
                break;

            case '-':
                logger.debug("got dash");
                parsingReply = true;
                break;

            default:
                throw new ParseException("SP or '-' expected");
            }

            logger.debug("reading line");
            for (boolean readingLine = true; readingLine; ) {
                if (!buf.hasRemaining()) {
                    throw new ParseException("does not end in CRLF");
                }
                char c = (char)buf.get();
                switch (c) {
                case CR:
                    if (LF != buf.get()) {
                        throw new ParseException("CR without LF");
                    } else {
                        sb.append(CRLF);
                        readingLine = false;
                    }
                    break;

                case LF:
                    throw new ParseException("LF without CR");

                default:
                    sb.append(c);
                    break;
                }
            }
        }

        return new SmtpReply(replyCode, sb.toString());
    }

    // bean methods -----------------------------------------------------------

    public int getReplyCode()
    {
        return replyCode;
    }

    public String getMessage()
    {
        return message;
    }

    // Token methods ---------------------------------------------------------

    /**
     * Includes final CRLF.
     *
     * @return the ftp reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        String[] lines = LINE_SPLITTER.split(message);
        byte[] rcBytes = Integer.toString(replyCode).getBytes();

        int l = message.length() + 4 * lines.length;
        ByteBuffer buf = ByteBuffer.allocate(l);

        for (int i = 0; i < lines.length; i++) {
            buf.put(rcBytes);
            buf.put((byte)(i == lines.length - 1 ? SP : DASH));
            buf.put(lines[i].getBytes());
            buf.put((byte)CR);
            buf.put((byte)LF);
        }

        buf.flip();

        return buf;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }

    // private methods --------------------------------------------------------

    private static int replyCode(ByteBuffer buf)
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

}
