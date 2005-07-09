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

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

class ImapServerParser extends AbstractParser
{
    private final Logger logger = Logger.getLogger(ImapServerParser.class);

    // constructors -----------------------------------------------------------

    ImapServerParser(TCPSession session)
    {
        super(session, false);
        logger.debug(this + " new ImapServerParser");

        lineBuffering(true); // XXX line buffering?
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        Chunk c = new Chunk(buf);

        logger.debug(this + " passing chunk of size: " + buf.remaining());
        return new ParseResult(c);
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        Chunk c = new Chunk(buf);

        logger.debug(this + " passing chunk of size: " + buf.remaining());
        return new ParseResult(c);
    }

    public TokenStreamer endSession() { return null; }
}
