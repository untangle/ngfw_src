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

import static com.metavize.tran.util.BufferUtil.*;
import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

class SmtpServerParser extends AbstractParser
{
    private final Logger logger = Logger.getLogger(SmtpServerParser.class);

    private int bufPos = 0;

    // constructors -----------------------------------------------------------

    SmtpServerParser(TCPSession session)
    {
        super(session, false);
        lineBuffering(true);
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse(ByteBuffer)");

        if (endsWithCrLf(buf) && bufPos + 3 < buf.limit()) {
            logger.debug("ends with CRLF");
            if (SP == buf.get(bufPos + 3)) { /* last line */
                logger.debug("got whole reply");
                SmtpReply reply = SmtpReply.parse(buf);
                bufPos = 0;
                logger.debug("returning reply");
                return new ParseResult(reply);
            } else {
                logger.debug("accumulating reply");
                bufPos = buf.limit();
                logger.debug("set bufPos to: " + bufPos);
                return new ParseResult(buf.compact());
            }
        } else {
            return new ParseResult(buf.compact());
        }
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (buf.hasRemaining()) {
            logger.warn("data trapped in read buffer: " + buf.remaining());
        }

        // XXX do something?

        return new ParseResult();
    }

    public TokenStreamer endSession() { return null; }
}
