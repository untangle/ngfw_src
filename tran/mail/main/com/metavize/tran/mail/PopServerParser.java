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
import com.metavize.tran.mime.EmailAddress;
import com.metavize.tran.mime.EmailAddressWithRcptType;
import com.metavize.tran.mime.HeaderParseException;
import com.metavize.tran.mime.InvalidHeaderDataException;
import com.metavize.tran.mime.LineTooLongException;
import com.metavize.tran.mime.RcptType;
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
    private MIMEMessageHolderT zMMHolderT;
    private boolean bSendAll;

    // constructors -----------------------------------------------------------

    PopServerParser(TCPSession session)
    {
        super(session, true);
        lineBuffering(false);
        state = State.REPLY;

        zMBScanner = new MessageBoundaryScanner();
        bSendAll = false;

        pipeline = MvvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse(" + buf + ")");

        List<Token> zTokens = new LinkedList<Token>(); /* send asap */
        boolean done = false;

        int iReplyEnd;

        while (false == done && true == buf.hasRemaining()) {
            switch (state) {
            case REPLY:
                logger.debug("REPLY state");
                iReplyEnd = findCRLFEnd(buf);
                if (1 < iReplyEnd) {
                    logger.debug("contains CRLF: " + buf);
                    PopReply reply = PopReply.parse(buf, iReplyEnd);
                    //logger.debug("reply: " + reply.toString());
                    logger.debug("parsed reply: " + buf);
                    zTokens.add(reply);

                    if (true == reply.isMsgData()) {
                        logger.debug("entering DATA state");
                        state = State.DATA;
                        try {
                            zMsgFile = pipeline.mktemp(); //XXXX
                            zMsgChannel = new FileOutputStream(zMsgFile, true).getChannel();
                        } catch (IOException exn) {
                            logger.warn("cannot create message file: ", exn);
                            zMsgChannel = null;
                            zMsgFile = null;
                            break;
                        }

                        zMMHolderT = new MIMEMessageHolderT(zMsgFile);
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
                buf.position(iLimit);
                dup.limit(iLimit);

                if (true == dup.hasRemaining()) {
                    if (false == bSendAll)
                    {
                        /* this casing temporarily buffers and writes header */

                        try
                        {
                            bSendAll = zMBScanner.processHeaders(dup.duplicate(), LINE_SZ);
                        }
                        catch (LineTooLongException exn)
                        {
                            logger.warn("cannot parse message header: " + exn);
                            handleException(zTokens, dup.duplicate());
                            break;
                        }

                        logger.debug("message header is complete: " + bSendAll);

                        writeFile(dup.duplicate());

                        if (true == bSendAll)
                        {
                            closeMsgChannel();
                            zMsgFile = null;

                            try
                            {
                                MIMEMessageHeaders zMMHeader = MIMEMessageHeaders.parseMMHeaders(zMMHolderT.getInputStream(), zMMHolderT.getFileMIMESource());
                                MessageInfo zMsgInfo = createMsgInfo(zMMHeader);
                                zMMHolderT.setMIMEMessageHeader(zMMHeader);
                                zMMHolderT.setMessageInfo(zMsgInfo);
                                zTokens.add(zMMHolderT);
                            }
                            catch (IOException exn)
                            {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, dup.duplicate());
                                break;
                            }
                            catch (InvalidHeaderDataException exn)
                            {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, dup.duplicate());
                                break;
                            }
                            catch (HeaderParseException exn)
                            {
                                logger.warn("cannot parse message header: " + exn);
                                handleException(zTokens, dup.duplicate());
                                break;
                            }

                            dup.position(iLimit); /* consume message */
                        }
                    }
                    else
                    {
                        logger.debug("message body: " + dup);

                        /* transform will buffer and write remaining data */
                        zTokens.add(consumeChunk(dup));
                    }
                } else {
                    logger.debug("no data");
                }

                if (true == buf.hasRemaining() && true == startsWith(buf, EODATA_TAIL)) {
                    logger.debug("got message end: " + dup);

                    if (true == dup.hasRemaining()) {
                        dup.rewind();
                        throw new ParseException("message not fully consumed: '" + AsciiCharBuffer.wrap(dup) + "'");
                    }

                    bSendAll = false;
                    zMBScanner.reset();
                    zMMHolderT = null;

                    zTokens.add(EndMarker.MARKER);
                    // XXX check if .[[:space:]]*CRLF is allowed
                    buf.position(buf.position() + 3); // XXX consume .CRLF

                    logger.debug("entering REPLY state");
                    state = State.REPLY;
                } else {
                    if (true == dup.hasRemaining()) {
                        /* append data fragment (e.g., all remaining data) */
                        dup.rewind();
                        if (true == buf.hasRemaining()) {
                            logger.debug("copying into new buffer");
                            int len = dup.remaining() + buf.remaining();
                            logger.debug("size of dup + buf: " + len);
                            len += LINE_SZ;

                            ByteBuffer bufTmp = ByteBuffer.allocate(len);
                            bufTmp.put(dup);
                            bufTmp.put(buf);
                            bufTmp.rewind();
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
            buf.rewind();
            bufTmp.put(buf);
            buf = bufTmp;
        }

        buf = 0 < buf.position() ? buf : null;

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

    private Chunk consumeChunk(ByteBuffer zBuf)
    {
        ByteBuffer zBufTmp = ByteBuffer.allocate(zBuf.remaining());
        zBufTmp.put(zBuf);
        zBufTmp.rewind();

        return new Chunk(zBufTmp);
    }

    private void writeFile(ByteBuffer zBuf)
    {
        try
        {
            for (; true == zBuf.hasRemaining(); )
            {
                zMsgChannel.write(zBuf);
            }
        }
        catch (IOException exn)
        {
            closeMsgChannel();
            zMsgFile = null;
            logger.warn("cannot write data to message file: ", exn);
        }

        return;
    }

    private void closeMsgChannel()
    {
        logger.debug("close message channel file");

        try
        {
            zMsgChannel.force(true);
            zMsgChannel.close();
        }
        catch (IOException exn)
        {
            logger.warn("cannot close message file: ", exn);
        }
        finally
        {
            zMsgChannel = null;
        }

        return;
    }

    private MessageInfo createMsgInfo(MIMEMessageHeaders zMMHeader)
    {
        MessageInfo zMsgInfo = new MessageInfo(session.id(), session.serverPort(), zMMHeader.getSubject());
        EmailAddress zFrom = zMMHeader.getFrom();
        zMsgInfo.addAddress(AddressKind.FROM, zFrom.getAddress(), zFrom.getPersonal()); /* from address will never be null */

        EmailAddress zRcpt;
        AddressKind zKind;

        List<EmailAddressWithRcptType> zRcptTypes = zMMHeader.getAllRecipients();
        for (EmailAddressWithRcptType zRcptType : zRcptTypes)
        {
            zRcpt = zRcptType.address;
            if (false == zRcpt.isNullAddress())
            {
                switch(zRcptType.type)
                {
                case TO:
                    zKind = AddressKind.TO;
                    break;

                case CC:
                    zKind = AddressKind.CC;
                    break;

                default:
                case BCC:
                    continue; /* skip BCC */
                }

                zMsgInfo.addAddress(zKind, zRcpt.getAddress(), zRcpt.getPersonal());
            }
        }

        return zMsgInfo;
    }

    private void handleException(List<Token> zTokens, ByteBuffer zBuf) throws ParseException
    {
        PopReply reply = PopReply.parse(zBuf, zBuf.limit());
        //logger.debug("reply: " + reply.toString());
        logger.debug("parsed reply (exception): " + zBuf);
        zTokens.add(reply);

        closeMsgChannel();
        zMsgFile = null;

        state = State.REPLY;
        return;
    }
}
