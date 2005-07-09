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

public class PopServerParser extends AbstractParser
{
    private final static Logger logger = Logger.getLogger(PopServerParser.class);

    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String SZVAL = DIGVAL + " octets";
    private final static String OK = "+OK ";
    private final static String OKREPLY = "^\\" + OK;
    private final static String CRLF = "\r\n";
    private final static String EOLINE = CRLF;
    private final static String PEOLINE = EOLINE + "$"; /* protocol EOLINE */

    private final static String DATAOK = OKREPLY + SZVAL + "[^" + PEOLINE + "]*" + PEOLINE;
    private final static String EODATA_TAIL = "." + PEOLINE;
    private final static String EODATA_FULL = PEOLINE + EODATA_TAIL;

    private final static int LINE_SZ = 1024;

    private enum State { REPLY,
                         DATA
                       };

    private final Pipeline pipeline;

    private MimeParser mimeParser = null;
    private State state;
    private File msgFile;
    private FileChannel msgChannel;

    // constructors -----------------------------------------------------------

    PopServerParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(false);
        state = State.REPLY;

        pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse(" + buf + ")");

        List<Token> toks = new LinkedList<Token>();
        boolean done = false;

        while (false == done && true == buf.hasRemaining()) {
            switch (state) {
            case REPLY:
                logger.debug("REPLY state");
                if (0 <= findCrLf(buf)) {
                    logger.debug("contains CRLF: " + buf);
                    PopReply reply = PopReply.parse(buf);
                    //logger.debug("reply: " + reply.toString());
                    logger.debug("parsed reply: " + buf);
                    toks.add(reply);

                    if (true == reply.toString().matches(DATAOK)) {
                        logger.debug("entering DATA state");
                        state = State.DATA;
                        try {
                            msgFile = pipeline.mktemp();
                            msgChannel = new FileOutputStream(msgFile).getChannel();
                        } catch (IOException exn) {
                            logger.warn("cannot create message file: ", exn);
                            msgFile = null;
                            msgChannel = null;
                        }
                        mimeParser = new MimeParser();
                    }
                } else {
                    logger.debug("does not end with CRLF");
                    done = true;
                }

                break;

            case DATA:
                logger.debug("DATA state");

                int iLimit = dataCutoff(buf);
                logger.debug("cutoff at: " + iLimit);

                ByteBuffer dup = buf.duplicate();
                dup.limit(iLimit);
                buf.position(iLimit);

                try {
                    for (ByteBuffer bout = dup.duplicate();
                         null != msgChannel && true == bout.hasRemaining();
                         msgChannel.write(bout)) ;
                } catch (IOException exn) {
                    logger.warn("cannot write message: ", exn);
                    msgFile = null;
                    msgChannel = null;
                }

                if (true == dup.hasRemaining()) {
                    logger.debug("parsing message: " + dup);
                    ParseResult pr = mimeParser.parse(dup);
                    logger.debug("parsed message: " + dup);
                    dup = pr.getReadBuffer();
                    toks.addAll(pr.getResults());
                } else {
                    logger.debug("not enough data to parse message");
                }

                if (true == buf.hasRemaining() && true == startsWith(buf, EODATA_TAIL)) {
                    if (null != dup && 0 != dup.position()) {
                        dup.flip();
                        logger.debug("message not fully consumed: '" +
                                     AsciiCharBuffer.wrap(dup) + "'");
                        throw new ParseException("message not fully consumed");
                    }

                    logger.debug("got message end");
                    if (null != msgChannel) {
                        try {
                            msgChannel.close();
                        } catch (IOException exn) {
                            logger.warn("cannot close message file: ", exn);
                        }
                    }
                    toks.add(new MessageFile(msgFile));
                    toks.add(EndMarker.MARKER);
                    // XXX check if .[[:space:]]*CRLF is allowed
                    buf.position(buf.position() + 3); // XXX consume .CRLF

                    logger.debug("entering REPLY state");
                    state = State.REPLY;
                } else {
                    if (null != dup && 0 != dup.position()) {
                        dup.flip();
                        if (true == buf.hasRemaining()) {
                            logger.debug("copying into new buffer");
                            int len = dup.remaining() + buf.remaining();
                            logger.debug("size of dup + buf: " + len);
                            len += LINE_SZ;
                            ByteBuffer bufTmp = ByteBuffer.allocate(len);
                            bufTmp.put(dup);
                            bufTmp.put(buf);
                            bufTmp.flip();
                            buf = bufTmp;
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
                throw new IllegalStateException("unknown state: " + state);
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
        if (true == startsWith(buf, EODATA_TAIL)) {
            logger.debug("buffer starts with .CRLF");
            return 0;
        } else {
            int i = findString(buf, EODATA_FULL);
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
