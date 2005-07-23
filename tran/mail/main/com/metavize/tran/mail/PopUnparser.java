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

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.EndMarker;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;
import org.apache.log4j.Logger;

class PopUnparser extends AbstractUnparser
{
    private final static Logger logger = Logger.getLogger(PopUnparser.class);

    private final static String DATA_END = "." + CRLF;

    private ByteEncoder encoder;

    public PopUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        logger.debug("unparser got: " + token.getClass());

        List<ByteBuffer> bufs = new LinkedList<ByteBuffer>();

        if (null != encoder && !(token instanceof Chunk)) {
            bufs.add(encoder.endEncoding());
            encoder = null;
        }

        if (token instanceof EndMarker) {
            bufs.add(ByteBuffer.wrap(DATA_END.getBytes()));
        } else if (token instanceof Rfc822Header) {
            Rfc822Header header = (Rfc822Header)token;
            String te = header.getContentTransferEncoding();
            // XXX quoted printable XXX
            if (null != te && te.equalsIgnoreCase("base64")) {
                encoder = new Base64Encoder();
            }

            bufs.add(header.getBytes());
        } else if (token instanceof Chunk) {
            Chunk chunk = (Chunk)token;
            ByteBuffer bytes = chunk.getBytes();
            if (null == encoder) {
                bufs.add(bytes);
            } else {
                bufs.addAll(encoder.encode(bytes));
            }
        } else {
            bufs.add(token.getBytes());
        }

        return new UnparseResult(bufs.toArray(new ByteBuffer[bufs.size()]));
    }

    public TCPStreamer endSession() { return null; }
}
