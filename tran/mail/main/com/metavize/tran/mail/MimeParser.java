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
import java.util.LinkedList;
import java.util.List;
import javax.mail.internet.ContentType;

import com.metavize.tran.mail.MimeStateMachine.State;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.header.HeaderParser;
import org.apache.log4j.Logger;

class MimeParser
{
    private enum ParserState {
        RFC822_MESSAGE,
        NON_MULTIPART_BODY
    };

    private final Logger logger = Logger.getLogger(MimeParser.class);

    private State state = State.START;
    private String boundary;
    private ByteDecoder decoder = null;

    private HeaderParser<Rfc822Header> headerParser = null;
    private MimeParser mimeParser = null;

    // constructors -----------------------------------------------------------

    MimeParser() { }

    MimeParser(String boundary)
    {
        this.state = State.PREAMBLE;
        this.boundary = boundary;
        this.decoder = null;
    }

    // package protected methods ----------------------------------------------

    ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parsing message");

        List<Token> tokens = new LinkedList<Token>();

        while (buf.hasRemaining()) {
            switch (state) {
            case START: {
                logger.debug("START");
                state = State.MESSAGE_HEADER;
                break;
            }

            case BODY: {
                logger.debug("BODY");
                if (null == mimeParser) {
                    consumeChunk(buf, tokens);
                    return new ParseResult(tokens, null);
                } else {
                    ParseResult pr = mimeParser.parse(buf);
                    tokens.addAll(pr.getResults());
                    return new ParseResult(tokens, pr.getReadBuffer());
                }
            }

            case MESSAGE_HEADER: {
                logger.debug("MESSAGE_HEADER");
                if (null == headerParser) {
                    headerParser = new HeaderParser(new Rfc822Header());
                }

                if (headerParser.isCompleteHeader(buf)) {
                    logger.debug("got complete header");
                    Rfc822Header header = headerParser.parse(buf);

                    headerParser = null;
                    tokens.add(header);

                    String te = header.getContentTransferEncoding();
                    // XXX add quoted printable XXX
                    if (null != null && te.equalsIgnoreCase("base64")) {
                        decoder = new Base64Decoder();
                    }

                    switch (header.getMessageType()) {
                    case RFC822:
                        mimeParser = new MimeParser();
                        state = State.BODY;
                        break;

                    case MULTIPART:
                        ContentType ct = header.getContentType();
                        boundary = null == ct ? null : ct.getParameter("boundary");
                        state = State.PREAMBLE;
                        break;

                    case BLOB:
                        state = State.BODY;
                        break;
                    }
                } else {
                    ByteBuffer b = ByteBuffer.allocate(buf.capacity());
                    b.put(buf);
                    return new ParseResult(tokens, b);
                }
                break;
            }

            case PREAMBLE: {
                logger.debug("PREAMBLE");
                int i = findBoundary(buf);
                logger.debug("found delmiter at: " + i + " in : " + buf);
                if (0 <= i) {
                    logger.debug("found boundary");

                    if (0 < i) { // there is content before boundary
                        ByteBuffer dup = buf.duplicate();
                        dup.limit(i - 1);
                        ByteBuffer cbuf = ByteBuffer.allocate(dup.remaining());
                        cbuf.put(dup);
                        cbuf.flip();
                        Chunk c = new Chunk(cbuf);
                        tokens.add(c);
                    } else {
                        tokens.add(Chunk.EMPTY);
                    }

                    buf.position(i);
                    MimeBoundary boundary = consumeBoundary(buf);
                    logger.debug("after consume: " + buf);
                    tokens.add(boundary);

                    if (boundary.isLast()) {
                        throw new ParseException("no parts");
                    }

                    state = State.MULTIPART_HEADER;
                } else {
                    ByteBuffer b = ByteBuffer.allocate(buf.capacity());
                    b.put(buf);
                    return new ParseResult(tokens, b);
                }
                break;
            }

            case MULTIPART_HEADER: {
                logger.debug("MULTIPART_HEADER: " + buf);
                if (null == headerParser) {
                    headerParser = new HeaderParser(new Rfc822Header());
                }
                if (headerParser.isCompleteHeader(buf)) {
                    Rfc822Header header = headerParser.parse(buf);
                    headerParser = null;
                    tokens.add(header);

                    String te = header.getContentTransferEncoding();

                    // XXX add quoted-printable XXX
                    if (null != te && te.equalsIgnoreCase("base64")) {
                        decoder = new Base64Decoder();
                    } else {
                        decoder = null;
                    }

                    switch (header.getMessageType()) {
                    case RFC822:
                        logger.debug("mimeParser()");
                        mimeParser = new MimeParser();
                        break;

                    case MULTIPART:
                        logger.debug("MimeParser(b)");
                        ContentType ct = header.getContentType();
                        String b = null == ct ? null : ct.getParameter("boundary");
                        mimeParser = new MimeParser(b);
                        break;

                    case BLOB:
                        logger.debug("mimeParser = null");
                        mimeParser = null;
                        break;
                    }
                    state = State.MULTIPART_BODY;
                } else {
                    ByteBuffer b = ByteBuffer.allocate(buf.capacity());
                    b.put(buf);
                    return new ParseResult(tokens, b);
                }
                break;
            }

            case MULTIPART_BODY: {
                logger.debug("MULTIPART_BODY");
                int i = findBoundary(buf);

                if (0 > i) {
                    logger.debug("partial body");
                    if (null == mimeParser) {
                        logger.debug("consuming raw body");
                        consumeChunk(buf, tokens);
                        return new ParseResult(tokens);
                    } else {
                        logger.debug("parsing nested body");
                        ParseResult pr = mimeParser.parse(buf);
                        tokens.addAll(pr.getResults());
                        return new ParseResult(tokens, pr.getReadBuffer());
                    }
                } else {
                    logger.debug("whole body");
                    ByteBuffer dup = buf.duplicate();
                    dup.limit(i);
                    buf.position(i);

                    if (null == mimeParser) {
                        consumeChunk(dup, tokens);
                    } else {
                        ParseResult pr = mimeParser.parse(dup);
                        tokens.addAll(pr.getResults());
                        ByteBuffer rb = pr.getReadBuffer();
                        if (null != rb && 0 != rb.position()) {
                            throw new ParseException("read buffer not empty: " + rb);
                        }

                        state = State.MULTIPART_HEADER;
                    }

                    MimeBoundary boundary = consumeBoundary(buf);
                    tokens.add(boundary);

                    if (boundary.isLast()) {
                        state = State.EPILOGUE;
                    } else {
                        state = State.MULTIPART_HEADER;
                    }
                }
                break;
            }

            case EPILOGUE:
                logger.debug("EPILOGUE");
                consumeChunk(buf, tokens);
                return new ParseResult(tokens, null);
            }
        }

        return new ParseResult(tokens, null);
    }

    // private methods --------------------------------------------------------

    private int findBoundary(ByteBuffer buf)
    {
        logger.debug("findBoundary: " + buf);

        String delimStr = "--" + boundary;

        logger.debug("searching for delimStr: '" + delimStr + "'");

        if (startsWith(buf, delimStr)) {
            logger.debug("startsWith: " + buf);
            logger.debug("found delimStr at 0");
            return buf.position();
        } else {
            logger.debug("!startsWith: " + buf);
            logger.debug("searching buffer: '" + CRLF + delimStr + "'");
            int i = findString(buf, CRLF + delimStr);
            return 0 > i ? i : i + 2;
        }
    }

    // buf must be at start of boundary
    private MimeBoundary consumeBoundary(ByteBuffer buf)
    {
        logger.debug("consuming delim: " + buf);
        buf.position(buf.position() + boundary.length() + 2);
        logger.debug("pos + boundary.length + 2: " + buf);
        boolean endBoundary = startsWith(buf, "--");
        logger.debug("after startsWith: " + buf);
        logger.debug("endBoundary: " + endBoundary);

        int i = findCrLf(buf);
        logger.debug("i: " + i + " buf: " + buf);
        if (0 <= i) {
            buf.position(i + 2);
            logger.debug("buf.position = i + 2: " + buf);
        } else {
            throw new IllegalStateException("no CRLF after boundary");
        }

        return new MimeBoundary(boundary, endBoundary);
    }

    private void consumeChunk(ByteBuffer buf, List<Token> l)
    {
        if (null != decoder) {
            List<ByteBuffer> bufs = decoder.decode(buf);
            for (ByteBuffer b : bufs) {
                l.add(new Chunk(b));
            }
        } else {
            ByteBuffer b = ByteBuffer.allocate(buf.remaining());
            b.put(buf);
            b.flip();

            Chunk c = new Chunk(b);
            l.add(new Chunk(b));
        }
    }
}
