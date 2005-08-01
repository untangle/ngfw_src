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

package com.metavize.tran.mail.impl.pop;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.BufferUtil.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Pipeline;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.mail.papi.pop.PopCommand;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class PopClientParser extends AbstractParser
{
    private final static Logger logger = Logger.getLogger(PopClientParser.class);

    private final static int LINE_SZ = 1024;

    private final Pipeline pipeline;

    // constructors -----------------------------------------------------------

    public PopClientParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(false);

        pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        //logger.debug("parse(" + AsciiCharBuffer.wrap(buf) + "), " + buf);
        logger.debug("parse(" + buf + ")");

        List<Token> toks = new LinkedList<Token>();

        boolean done = false;

        while (false == done && true == buf.hasRemaining()) {
            if (0 <= findCrLf(buf)) {
                logger.debug("contains CRLF: " + buf);
                PopCommand cmd = PopCommand.parse(buf);
                //logger.debug("cmd: " + cmd.toString());
                logger.debug("parsed cmd: " + buf);
                toks.add(cmd);
            } else {
                logger.debug("does not end with CRLF");
                done = true;
            }
        }

        buf.compact();

        if (0 < buf.position() && LINE_SZ >= buf.remaining()) {
            ByteBuffer bufTmp = ByteBuffer.allocate(buf.capacity() + LINE_SZ);
            buf.flip();
            bufTmp.put(buf);
            buf = bufTmp;
        }

        buf = 0 < buf.position() ? buf : null;

        logger.debug("returning ParseResult(" + toks + ", " + buf + ")");

        return new ParseResult(toks, buf);
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (true == buf.hasRemaining()) {
            logger.warn("data trapped in read buffer: " + AsciiCharBuffer.wrap(buf));
        }

        // XXX do something?

        return new ParseResult();
    }

    // private methods --------------------------------------------------------
}
