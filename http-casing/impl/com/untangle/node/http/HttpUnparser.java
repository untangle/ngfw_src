/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.http;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.EndMarker;
import com.untangle.node.token.Header;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * An HTTP <code>Unparser</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class HttpUnparser extends AbstractUnparser
{
    private static final ByteBuffer[] BYTE_BUFFER_PROTO = new ByteBuffer[0];

    // XXX move to util class/interface
    private static final byte[] LAST_CHUNK = "0\r\n\r\n".getBytes();
    private static final byte[] CRLF = "\r\n".getBytes();

    private static final int CLOSE_ENCODING = 0;
    private static final int CONTENT_LENGTH_ENCODING = 1;
    private static final int CHUNKED_ENCODING = 2;

    private final Logger logger = Logger.getLogger(HttpUnparser.class);

    // used to keep request with header, IIS requires this
    private final Queue outputQueue = new LinkedList();
    private final HttpCasing httpCasing;

    private int size = 0;
    private int transferEncoding;
    private String sessStr;

    HttpUnparser(TCPSession session, boolean clientSide,
                 HttpCasing httpCasing)
    {
        super(session, clientSide);
        this.httpCasing = httpCasing;
    }

    public TCPStreamer endSession() { return null; }

    public UnparseResult unparse(Token token)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " got unparse event node: " + token);
        }

        if (token instanceof StatusLine) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + " got status line!");
            }
            transferEncoding = CLOSE_ENCODING;
            return statusLine((StatusLine)token);
        } else if (token instanceof RequestLineToken) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + " got request line!");
            }
            return requestLine((RequestLineToken)token);
        } else if (token instanceof Header) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + " got header!");
            }
            return header((Header)token);
        } else if (token instanceof Chunk) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + " got chunk!");
            }
            return chunk((Chunk)token);
        } else if (token instanceof EndMarker) {
            if (logger.isDebugEnabled()) {
                logger.debug(sessStr + " got endmarker");
            }
            return endMarker();
        } else {
            throw new IllegalArgumentException("unexpected: " + token);
        }
    }

    public UnparseResult releaseFlush()
    {
        return dequeueOutput();
    }

    private UnparseResult statusLine(StatusLine s)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " status-line");
        }

        queueOutput(s.getBytes());

        return new UnparseResult(BYTE_BUFFER_PROTO);
    }

    private UnparseResult requestLine(RequestLineToken rl)
    {
        HttpMethod method = rl.getMethod();

        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " request-line"
                         + sessStr + " Unparser got method: " + method);
        }

        httpCasing.queueRequest(rl);

        queueOutput(rl.getBytes());

        return new UnparseResult(BYTE_BUFFER_PROTO);
    }

    private UnparseResult header(Header h)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " header");
        }

        String encoding = h.getValue("transfer-encoding");
        if (null != encoding && encoding.equalsIgnoreCase("chunked")) {
            transferEncoding = CHUNKED_ENCODING;
        } else if (null != h.getValue("content-length")) {
            transferEncoding = CONTENT_LENGTH_ENCODING;
        }

        queueOutput(h.getBytes());
        if (isClientSide()) {
            return dequeueOutput();
        } else {
            return new UnparseResult(BYTE_BUFFER_PROTO);
        }
    }

    private UnparseResult chunk(Chunk c)
    {
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " chunk");
        }

        ByteBuffer cBuf = c.getBytes();

        if (CHUNKED_ENCODING == transferEncoding && 0 == cBuf.remaining()) {
            return new UnparseResult(BYTE_BUFFER_PROTO);
        }

        ByteBuffer buf;

        switch (transferEncoding) {
        case CLOSE_ENCODING:
        case CONTENT_LENGTH_ENCODING:
            buf = cBuf;
            break;
        case CHUNKED_ENCODING:
            buf = ByteBuffer.allocate(cBuf.remaining() + 32);
            String hexLen = Integer.toHexString(cBuf.remaining());
            buf.put(hexLen.getBytes());
            buf.put(CRLF);
            buf.put(cBuf);
            buf.put(CRLF);
            buf.flip();
            break;
        default:
            throw new IllegalStateException
                ("transferEncoding: " + transferEncoding);
        }

        if (outputQueue.isEmpty()) {
            return new UnparseResult(new ByteBuffer[] { buf });
        } else {
            queueOutput(buf);
            return dequeueOutput();
        }
    }

    private UnparseResult endMarker()
    {
        if (logger.isDebugEnabled()) {
            logger.debug(sessStr + " GOT END MARER!!");
        }

        ByteBuffer buf = null;

        if (CHUNKED_ENCODING == transferEncoding) {
            buf = ByteBuffer.wrap(LAST_CHUNK);
        }

        if (outputQueue.isEmpty()) {
            return new UnparseResult(null == buf ? BYTE_BUFFER_PROTO
                                     : new ByteBuffer[] { buf });
        } else {
            if (null != buf) {
                queueOutput(buf);
            }

            return dequeueOutput();
        }
    }

    private void queueOutput(ByteBuffer buf)
    {
        size += buf.remaining();

        outputQueue.add(buf);
    }

    private UnparseResult dequeueOutput()
    {
        ByteBuffer buf = ByteBuffer.allocate(size);

        for (Iterator i = outputQueue.iterator(); i.hasNext(); ) {
            ByteBuffer b = (ByteBuffer)i.next();
            buf.put(b);
        }

        buf.flip();

        size = 0;
        outputQueue.clear();

        return new UnparseResult(new ByteBuffer[] { buf });
    }
}
