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
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.InvalidHeaderDataException;
import com.metavize.tran.mime.LineTooLongException;
import com.metavize.tran.mime.MIMEMessageHeaders;
import com.metavize.tran.token.AbstractParser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

public class PopServerParser extends AbstractParser
{
    private final static Logger logger = Logger.getLogger(PopServerParser.class);

    private final static File BUNNICULA_TMP = new File(System.getProperty("bunnicula.tmp.dir"));

    private final static String EODATA_TAIL = "." + PopReply.PEOLINE;
    private final static String EODATA_FULL = PopReply.PEOLINE + EODATA_TAIL;

    private final static int LINE_SZ = 1024;

    private enum State {
        REPLY,
        DATA
    };

    private final MessageBoundaryScanner zMBScanner;
    private final Pipeline pipeline;

    private State state;
    private File zMsgFile;
    private FileChannel zMsgChannel;
    private MIMEMessageT zMMessageT;
    private boolean bHdrDone;
    private boolean bBodyDone;

    // constructors -----------------------------------------------------------

    PopServerParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(false);
        state = State.REPLY;

        zMBScanner = new MessageBoundaryScanner();
        bHdrDone = false;
        bBodyDone = false;

        pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        //logger.debug("parse(" + AsciiCharBuffer.wrap(buf) + "), " + buf);
        logger.debug("parse(" + buf + ")");

        List<Token> zTokens = new LinkedList<Token>();
        boolean bDone = false;

        while (false == bDone) {
            switch (state) {
            case REPLY:
                logger.debug("REPLY state, " + buf);

                int iReplyEnd = findCRLFEnd(buf);
                if (1 < iReplyEnd) {

                    /* scan and consume reply */
                    PopReply reply = PopReply.parse(buf, iReplyEnd);
                    zTokens.add(reply);

                    if (true == reply.isMsgData()) {
                        //logger.debug("retr message reply: " + reply + ", " + buf);
                        logger.debug("retr message reply: " + buf);

                        try {
                            zMsgFile = pipeline.mktemp();
                            zMsgChannel = new FileOutputStream(zMsgFile).getChannel();
                            //logger.debug("message file: " + zMsgFile);
                        } catch (IOException exn) {
                            logger.warn("cannot create message file: ", exn);
                            zMsgChannel = null;
                            zMsgFile = null;
                            break;
                        }

                        zMMessageT = new MIMEMessageT(zMsgFile);

                        logger.debug("entering DATA state");
                        state = State.DATA;

                        if (false == buf.hasRemaining()) {
                            logger.debug("buf is empty");

                            buf = null;
                            bDone = true;
                        }
                        /* else if we have more data to parse
                         * (e.g., data is message fragment),
                         * then parse remaining data
                         */
                    } else {
                        //logger.debug("message reply: " + reply + ", " + buf);
                        logger.debug("message reply: " + buf);

                        buf = null;
                        bDone = true;
                    }
                } else {
                    logger.debug("buf does not contain CRLF");

                    /* wait for more data */
                    bDone = true;
                }

                break;

            case DATA:
                logger.debug("DATA state, " + buf);

                if (true == buf.hasRemaining()) {
                    ByteBuffer dup = buf.duplicate();

                    if (false == bHdrDone) {
                        /* casing temporarily buffers and writes header */
                        logger.debug("message header: " + dup);

                        ByteBuffer writeDup = dup.duplicate();

                        try {
                            /* scan and "consume" message frag */
                            bHdrDone = zMBScanner.processHeaders(dup, LINE_SZ);
                        } catch (LineTooLongException exn) {
                            logger.warn("cannot parse message header: " + exn);
                            handleException(zTokens, buf.duplicate());
                            break;
                        }

                        logger.debug("message header is complete: " + bHdrDone + ", " + dup);

                        /* writeDup position is already set */
                        writeDup.limit(dup.position());
                        writeFile(writeDup);

                        if (true == bHdrDone) {
                            closeMsgChannel();
                            zMsgFile = null;

                            try {
                                MIMEMessageHeaders zMMHeader = MIMEMessageHeaders.parseMMHeaders(zMMessageT.getInputStream(), zMMessageT.getFileMIMESource());
                                MessageInfo zMsgInfo = MessageInfo.fromMIMEMessage(zMMHeader, session.id(), session.serverPort());

                                zMMessageT.setMIMEMessageHeader(zMMHeader);
                                zMMessageT.setMessageInfo(zMsgInfo);

                                zTokens.add(zMMessageT);
                            } catch (IOException exn) {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, buf.duplicate());
                                break;
                            } catch (InvalidHeaderDataException exn) {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, buf.duplicate());
                                break;
                            } catch (HeaderParseException exn) {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, buf.duplicate());
                                break;
                            }
                        }
                    } else {
                        /* transform writes body */
                        logger.debug("message body: " + dup);

                        /* scan and "copy" message frag */
                        ByteBuffer chunkDup = ByteBuffer.allocate(dup.limit());
                        bBodyDone = zMBScanner.processBody(dup, chunkDup);

                        chunkDup.rewind();
                        zTokens.add(new Chunk(chunkDup));

                        if (true == bBodyDone) {
                            logger.debug("got message end: " + dup);

                            zMBScanner.reset();
                            zMMessageT = null;
                            bHdrDone = false;
                            bBodyDone = false;

                            zTokens.add(EndMarker.MARKER);

                            logger.debug("re-entering REPLY state");
                            state = State.REPLY;
                        }

                        /* stop even though buf may not be empty
                         * - remaining fragment can't be handled yet
                         */
                        bDone = true;
                    }

                    if (false == dup.hasRemaining()) {
                        logger.debug("buf is empty");

                        buf = null;
                        bDone = true;
                    } else {
                        logger.debug("compact buf: " + dup);

                        buf.clear();
                        buf.put(dup);
                        buf.flip();
                    }
                } else {
                    logger.debug("no (more) data");

                    buf = null;
                    bDone = true;
                }

                break;

            default:
                throw new IllegalStateException("unknown state: " + state);
            }
        }

        logger.debug("returning ParseResult(" + zTokens + ", " + buf + ")");

        return new ParseResult(zTokens, buf);
    }

    public ParseResult parseEnd(ByteBuffer buf) throws ParseException
    {
        if (true == buf.hasRemaining()) {
            logger.warn("data trapped in read buffer: " + AsciiCharBuffer.wrap(buf) + ", " + buf);
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

    private void writeFile(ByteBuffer zBuf)
    {
        try {
            for (; true == zBuf.hasRemaining(); ) {
                zMsgChannel.write(zBuf);
            }
        } catch (IOException exn) {
            closeMsgChannel();
            zMsgFile = null;
            logger.warn("cannot write data to message file: ", exn);
        }

        return;
    }

    private void closeMsgChannel()
    {
        logger.debug("close message channel file");

        try {
            zMsgChannel.force(true);
            zMsgChannel.close();
        } catch (IOException exn) {
            logger.warn("cannot close message file: ", exn);
        } finally {
            zMsgChannel = null;
        }

        return;
    }

    private void handleException(List<Token> zTokens, ByteBuffer zBuf) throws ParseException
    {
        PopReply reply = PopReply.parse(zBuf, zBuf.limit());
        //logger.debug("parsed reply (exception): " + reply.toString() + ", " + zBuf);
        logger.debug("parsed reply (exception): " + zBuf);
        zTokens.add(reply);

        closeMsgChannel();
        zMsgFile = null;

        state = State.REPLY;
        return;
    }
}
