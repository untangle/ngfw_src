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

package com.metavize.tran.mail.impl.imap;

import java.nio.ByteBuffer;

import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.mvvm.tapi.event.TCPStreamer;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.Chunk;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;
import org.apache.log4j.Logger;

class ImapUnparser extends AbstractUnparser
{
    private final Logger logger = Logger.getLogger(ImapUnparser.class);

    public ImapUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        Chunk c = (Chunk)token;
        ByteBuffer buf = c.getBytes();

        logger.debug(this + "unparsing: " + buf);

        return new UnparseResult(buf);
    }

    public TCPStreamer endSession() { return null; }
}
