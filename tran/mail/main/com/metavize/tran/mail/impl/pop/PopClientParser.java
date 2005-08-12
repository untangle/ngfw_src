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
import com.metavize.tran.mail.PopCasing;
import com.metavize.tran.mail.papi.pop.PopCommand;
import com.metavize.tran.mail.papi.pop.PopCommandMore;
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

    private final Pipeline pipeline;
    private final PopCasing zCasing;

    // constructors -----------------------------------------------------------

    public PopClientParser(TCPSession session, PopCasing zCasing)
    {
        super(session, true);
        lineBuffering(false);

        pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
        this.zCasing = zCasing;
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        //logger.debug("parse(" + AsciiCharBuffer.wrap(buf) + "), " + buf);
        logger.debug("parse(" + buf + ")");

        List<Token> zTokens = new LinkedList<Token>();

        boolean bDone = false;

        while (false == bDone) {
            int iCmdEnd = findCRLFEnd(buf);
            if (1 < iCmdEnd) {
                ByteBuffer dup = buf.duplicate();

                try {
                    PopCommand cmd;

                    if (null == zCasing.getUser()) {
                        /* we only check for user once per session */
                        cmd = PopCommand.parseUser(buf);
                    } else {
                        cmd = PopCommand.parse(buf);
                    }

                    zTokens.add(cmd);

                    //logger.debug("command: " + cmd + ", " + buf);
                    logger.debug("command: " + buf);
                } catch (ParseException exn) {
                    /* long command may break before CRLF sequence
                     * so if parse fails,
                     * we assume long command spans multiple buffers
                     */
                    zTokens.add(new PopCommandMore(dup));
                    logger.debug("command (more): " + dup + ", " + exn);
                    /* fall through */
                }

                buf = null;
                bDone = true;
            } else {
                logger.debug("buf does not contain CRLF");

                /* wait for more data */
                bDone = true;
            }
        }

        logger.debug("returning ParseResult(" + zTokens + ", " + buf + ")");

        return new ParseResult(zTokens, buf);
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

    private int findCRLFEnd(ByteBuffer zBuf)
    {
        /* returns 1 (if no CRLF) or greater (if CRLF found)
         * - findCrLf returns -1 if buffer contains no CRLF pair
         * - findCrLf returns absolute index of end of CRLF pair in buffer
         */
        return findCrLf(zBuf) + (1 + 1);
    }
}
