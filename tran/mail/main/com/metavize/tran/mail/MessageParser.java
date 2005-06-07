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

import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.ParseResult;
import com.metavize.tran.token.Token;
import org.apache.log4j.Logger;

class MessageParser
{
    private enum ParserState {
        START_MESSAGE,
        MESSAGE_HEADER,
        RFC822_MESSAGE,
        PREAMBLE,
        MIME_HEADER,
        MIME_BODY,
        EPILOGUE,
        NON_MIME_BODY
    };

    private final Logger logger = Logger.getLogger(MessageParser.class);

    private ParserState state = ParserState.START_MESSAGE;
    private String delimiter;
    private ByteDecoder decoder = null;

    private MessageParser messageParser = null;

    // constructors -----------------------------------------------------------

    MessageParser() { }

    MessageParser(String delimiter)
    {
        this.state = ParserState.PREAMBLE;
        this.delimiter = delimiter;
        this.decoder = null;
    }

    // package protected methods ----------------------------------------------

    ParseResult parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parsing message");

        List<Token> tokens = new LinkedList<Token>();

        while (buf.hasRemaining()) {
            switch (state) {
            case START_MESSAGE: {
                logger.debug("START_MESSAGE");
                state = ParserState.MESSAGE_HEADER;
                break;
            }

            case MESSAGE_HEADER: {
                logger.debug("MESSAGE_HEADER");
                if (completeHeader(buf)) {
                    logger.debug("got complete header");
                    Rfc822Header header = consumeHeader(buf);
                    tokens.add(header);

                    String mimeVersion = header.getMimeVersion();

                    if (null == mimeVersion) {
                        state = ParserState.NON_MIME_BODY;
                    } else {
                        if (!mimeVersion.equals("1.0")) {
                            logger.warn("unknown MIME-Version: " + mimeVersion);
                        }

                        String te = header.getContentTransferEncoding();
                        // XXX add quoted printable XXX
                        if (null != null && te.equalsIgnoreCase("base64")) {
                            decoder = new Base64Decoder();
                        }

                        ContentType contentType = header.getContentType();
                        if (null == contentType) {
                            logger.warn("MIME-Version without Content-Type");
                            state = ParserState.NON_MIME_BODY;
                        } else {
                            String primaryType = contentType.getPrimaryType();
                            String subType = contentType.getSubType();
                            if (primaryType.equals("multipart")) {
                                if (null != decoder) {
                                    logger.warn("encoded multipart?");
                                }

                                delimiter = contentType.getParameter("boundary");
                                if (null == delimiter) {
                                    // XXX make setting to be strict and reject message
                                    logger.warn("multipart without a boundary: " + contentType);
                                    state = ParserState.NON_MIME_BODY;
                                } else {
                                    state = ParserState.PREAMBLE;
                                }
                            } else if (primaryType.equals("message")
                                       && subType.equals("rfc822")) {
                                if (null != decoder) {
                                    logger.warn("encoded message/rfc822?");
                                }

                                state = ParserState.RFC822_MESSAGE;
                                messageParser = new MessageParser();
                            } else {
                                state = ParserState.NON_MIME_BODY;
                            }
                        }
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
                int i = findDelimiter(buf);
                logger.debug("found delmiter at: " + i + " in : " + buf);
                if (0 <= i) {
                    logger.debug("found delimiter");

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
                    MimeBoundary boundary = consumeDelimiter(buf);
                    logger.debug("after consume: " + buf);
                    tokens.add(boundary);

                    if (boundary.isLast()) {
                        throw new ParseException("no parts");
                    }

                    state = ParserState.MIME_HEADER;
                } else {
                    ByteBuffer b = ByteBuffer.allocate(buf.capacity());
                    b.put(buf);
                    return new ParseResult(tokens, b);
                }
                break;
            }

            case MIME_HEADER: {
                logger.debug("MIME_HEADER: " + buf);
                if (completeHeader(buf)) {
                    Rfc822Header header = consumeHeader(buf);
                    tokens.add(header);
                    ContentType ct = header.getContentType();

                    if (null == ct) {
                        // XXX rfc 2045 section 4 says:

                        // Note that the MIME-Version header field is
                        // required at the top level of a message.  It
                        // is not required for each body part of a
                        // multipart entity.  It is required for the
                        // embedded headers of a body of type
                        // "message/rfc822" or "message/partial" if
                        // and only if the embedded message is itself
                        // claimed to be MIME-conformant.

                        // throw new ParseException("no content type");
                    }

                    String te = header.getContentTransferEncoding();

                    // XXX add quoted-printable XXX
                    if (null != te && te.equalsIgnoreCase("base64")) {
                        decoder = new Base64Decoder();
                    } else {
                        decoder = null;
                    }

                    if (null != ct) {
                        String primaryType = ct.getPrimaryType();
                        String subType = ct.getSubType();
                        if (primaryType.equals("multipart")) {
                            String delim = ct.getParameter("boundary");
                            if (null == delim) {
                                throw new ParseException("no delimiter");
                            }
                            messageParser = new MessageParser(delim);
                            state = ParserState.MIME_BODY;
                        } else if (primaryType.equals("message")
                                   && subType.equals("rfc822")) {
                            messageParser = new MessageParser();
                            state = ParserState.MIME_BODY;
                        } else {
                            messageParser = null;
                            state = ParserState.MIME_BODY;
                        }
                    } else {
                        messageParser = null;
                        state = ParserState.MIME_BODY;
                    }
                } else {
                    ByteBuffer b = ByteBuffer.allocate(buf.capacity());
                    b.put(buf);
                    return new ParseResult(tokens, b);
                }
                break;
            }

            case MIME_BODY: {
                logger.debug("MIME_BODY");
                int i = findDelimiter(buf);

                if (0 > i) {
                    logger.debug("partial body");
                    if (null == messageParser) {
                        logger.debug("consuming raw body");
                        tokens.addAll(consumeChunk(buf));
                        return new ParseResult(tokens);
                    } else {
                        logger.debug("parsing nested body");
                        ParseResult pr = messageParser.parse(buf);
                        tokens.addAll(pr.getResults());
                        return new ParseResult(tokens, pr.getReadBuffer());
                    }
                } else {
                    logger.debug("whole body");
                    ByteBuffer dup = buf.duplicate();
                    dup.limit(i);
                    buf.position(i);

                    if (null == messageParser) {
                        tokens.addAll(consumeChunk(dup));
                    } else {
                        ParseResult pr = messageParser.parse(dup);
                        tokens.addAll(pr.getResults());
                        ByteBuffer rb = pr.getReadBuffer();
                        if (null != rb && 0 != rb.position()) {
                            throw new ParseException("read buffer not empty: " + rb);
                        }

                        state = ParserState.MIME_HEADER;
                    }

                    MimeBoundary boundary = consumeDelimiter(buf);
                    tokens.add(boundary);

                    if (boundary.isLast()) {
                        state = ParserState.EPILOGUE;
                    } else {
                        state = ParserState.MIME_HEADER;
                    }
                }
                break;
            }

            case EPILOGUE:
                decoder = null;
            case NON_MIME_BODY: {
                logger.debug("EPILOGUE|NON_MIME_BODY");
                tokens.addAll(consumeChunk(buf));
                return new ParseResult(tokens, null);
            }

            case RFC822_MESSAGE:
                logger.debug("RFC822_MESSAGE");
                return messageParser.parse(buf);
            }
        }

        return new ParseResult(tokens, null);
    }

    // private methods --------------------------------------------------------

    private boolean completeHeader(ByteBuffer buf)
    {
        ByteBuffer dup = buf.duplicate();

        while (dup.hasRemaining()) {
            int i = findCrLf(dup);

            if (0 <= i) {
                i += 2;
                dup.position(i);
                if (startsWith(dup, CRLF)) {
                    return true;
                }
            } else {
                return false;
            }
        }

        return false;
    }

    private int findDelimiter(ByteBuffer buf)
    {
        logger.debug("findDelimiter: " + buf);

        String delimStr = "--" + delimiter;

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

    // buf must be at start of delimiter
    private MimeBoundary consumeDelimiter(ByteBuffer buf)
    {
        logger.debug("consuming delim: " + buf);
        buf.position(buf.position() + delimiter.length() + 2);
        logger.debug("pos + delimiter.length + 2: " + buf);
        boolean endDelimiter = startsWith(buf, "--");
        logger.debug("after startsWith: " + buf);
        logger.debug("endDelimiter: " + endDelimiter);

        int i = findCrLf(buf);
        logger.debug("i: " + i + " buf: " + buf);
        if (0 <= i) {
            buf.position(i + 2);
            logger.debug("buf.position = i + 2: " + buf);
        } else {
            throw new IllegalStateException("no CRLF after delimiter");
        }

        return new MimeBoundary(delimiter, endDelimiter);
    }

    private List<Chunk> consumeChunk(ByteBuffer buf)
    {
        List<Chunk> l = new LinkedList<Chunk>();

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

        return l;
    }

    private Rfc822Header consumeHeader(ByteBuffer buf) throws ParseException
    {
        Rfc822Header header = Rfc822Header.parse(buf);

        // consume CRLF
        if (buf.remaining() < 2 || CR != buf.get() || LF != buf.get()) {
            throw new ParseException("expected CRLF");
        }

        return header;
    }
}
