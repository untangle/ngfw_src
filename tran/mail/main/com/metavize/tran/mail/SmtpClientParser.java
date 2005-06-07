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
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class SmtpClientParser extends AbstractParser
{
    private enum State { COMMAND, DATA };

    private final Pipeline pipeline;

    private final Logger logger = Logger.getLogger(SmtpClientParser.class);

    private State state = State.COMMAND;
    private MessageParser messageParser = null;
    private File msgFile;
    private FileChannel msgChannel;

    // constructors -----------------------------------------------------------

    SmtpClientParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(false);
        state = State.COMMAND;

        pipeline = MvvmContextFactory.context().pipelineFoundry()
            .getPipeline(session.id());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse(" + buf + ")");

        List<Token> toks = new LinkedList<Token>();

        boolean done = false;

        while (!done && buf.hasRemaining()) {
            switch (state) {
            case COMMAND:
                logger.debug("COMMAND state");
                if (0 <= findCrLf(buf)) {
                    logger.debug("contains CRLF: " + buf);
                    SmtpCommand cmd = SmtpCommand.parse(buf);
                    logger.debug("Parsed cmd: " + buf);
                    toks.add(cmd);

                    if (cmd.getCommand().equals("DATA")) {
                        logger.debug("entering DATA state");
                        state = State.DATA;
                        try {
                            msgFile = pipeline.mktemp();
                            msgChannel = new FileOutputStream(msgFile).getChannel();
                        } catch (IOException exn) {
                            logger.warn("could not make message file", exn);
                            msgFile = null;
                            msgChannel = null;
                        }
                        messageParser = new MessageParser();
                    }
                } else {
                    logger.debug("does not end with CRLF");
                    done = true;
                }

                break;

            case DATA:
                logger.debug("DATA state");

                int i = dataCutoff(buf);
                logger.debug("cutoff at: " + i);

                ByteBuffer dup = buf.duplicate();
                dup.limit(i);
                buf.position(i);

                try {
                    for (ByteBuffer bout = dup.duplicate();
                         null != msgChannel && bout.hasRemaining();
                         msgChannel.write(bout));
                } catch (IOException exn) {
                    logger.warn("exception while writing message", exn);
                    msgFile = null;
                    msgChannel = null;
                }

                if (dup.hasRemaining()) {
                    logger.debug("parsing message: " + dup);
                    ParseResult pr = messageParser.parse(dup);
                    logger.debug("parsed message: " + dup);
                    dup = pr.getReadBuffer();
                    toks.addAll(pr.getResults());
                } else {
                    logger.debug("not enough data, not parsing message");
                }

                if (buf.hasRemaining() && startsWith(buf, ".\r\n")) {
                    if (null != dup && 0 != dup.position()) {
                        throw new ParseException("message not fully consumed");
                    }

                    logger.debug("got message end");
                    if (null != msgChannel) {
                        try {
                            msgChannel.close();
                        } catch (IOException exn) {
                            logger.warn("could not close message file", exn);
                        }
                    }
                    toks.add(new MessageFile(msgFile));
                    toks.add(EndMarker.MARKER);
                    // XXX check if .[[:space:]]*CRLF is allowed
                    buf.position(buf.position() + 3); // consume .CRLF

                    logger.debug("entering COMMAND state");
                    state = State.COMMAND;
                } else {
                    if (null != dup && 0 != dup.position()) {
                        dup.flip();
                        if (buf.hasRemaining()) {
                            logger.debug("copying into new buffer");
                            int l = dup.remaining() + buf.remaining();
                            logger.debug("size of dup + buf: " + l);
                            l += 1024;
                            ByteBuffer b = ByteBuffer.allocate(l);
                            b.put(dup);
                            b.put(buf);
                            b.flip();
                            buf = b;
                        } else {
                            logger.debug("dup is now buf");
                            buf = dup;
                        }
                    } else {
                        logger.debug("buf is still buf");
                    }

                    done = true;
                }

                break;

            default:
                throw new IllegalStateException("illegal state: " + state);
            }
        }

        buf.compact();

        if (0 < buf.position() && 1024 >= buf.remaining()) {
            ByteBuffer b = ByteBuffer.allocate(buf.capacity() + 1024);
            buf.flip();
            b.put(buf);
            buf = b;
        }

        buf = 0 < buf.position() ? buf : null;

        logger.debug("returning ParseResult(" + toks + ", " + buf + ")");

        return new ParseResult(toks, buf);
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (buf.hasRemaining()) {
            logger.warn("data trapped in read buffer: " + AsciiCharBuffer.wrap(buf));
        }

        // XXX do something?

        return new ParseResult();
    }

    // private methods --------------------------------------------------------

    /**
     * Select a cutoff for message data, either before the . or at the
     * beginning of the last incomplete line.
     *
     * @param buf buffer to search.
     * @return the absolute index of the cutoff.
     */
    private int dataCutoff(ByteBuffer buf)
    {
        logger.debug("dataCutoff(" + buf + ")");
        if (startsWith(buf, ".\r\n")) {
            logger.debug("buffer starts with .CRLF");
            return 0;
        } else {
            int i = findString(buf, "\r\n.\r\n");
            if (0 <= i) {
                logger.debug("found CRLF.CRLF");
                return i + 2;
            } else {
                // return through last complete line
                int j = findLastString(buf, CRLF);

                if (0 <= j) {
                    logger.debug("cutoff at last CRLF");
                    return j + 2;
                } else {
                    logger.debug("no CRLF, cutoff at position");
                    return buf.position();
                }
            }
        }
    }
}
