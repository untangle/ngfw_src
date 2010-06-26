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

package com.untangle.node.mail.impl.smtp;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.smtp.Response;
import com.untangle.node.mail.papi.smtp.ResponseParser;
import com.untangle.node.mail.papi.smtp.SASLExchangeToken;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.uvm.vnet.TCPSession;


/**
 * ...name says it all...
 */
class SmtpServerParser extends SmtpParser
{
    private final Logger m_logger = Logger.getLogger(SmtpServerParser.class);

    SmtpServerParser(TCPSession session, SmtpCasing parent, CasingSessionTracker tracker)
    {
        super(session, parent, tracker, false);

        m_logger.debug("Created");
        lineBuffering(false);
    }

    @Override
    @SuppressWarnings("fallthrough")
    protected ParseResult doParse(ByteBuffer buf)
    {
        List<Token> toks = new ArrayList<Token>();
        boolean done = false;

        while(buf.hasRemaining() && ! done) {

            if(isPassthru()) {
                m_logger.debug("Passthru buffer (" + buf.remaining() + " bytes )");
                toks.add(new Chunk(buf));
                return new ParseResult(toks);
            }

            if(getSmtpCasing().isInSASLLogin()) {
                m_logger.debug("In SASL Exchange");
                ByteBuffer dup = buf.duplicate();
                switch(getSmtpCasing().getSASLObserver().serverData(buf)) {
                case EXCHANGE_COMPLETE:
                    m_logger.debug("SASL Exchange complete");
                    getSmtpCasing().closeSASLExchange();
                    //fallthrough ?? XXX
                case IN_PROGRESS:
                    //There should not be any extra bytes
                    //left with "in progress", but what the hell
                    dup.limit(buf.position());
                    toks.add(new SASLExchangeToken(dup));
                    break;
                case RECOMMEND_PASSTHRU:
                    m_logger.debug("Entering passthru on advice of SASLObserver");
                    declarePassthru();
                    toks.add(PassThruToken.PASSTHRU);
                    toks.add(new Chunk(dup.slice()));
                    buf.position(buf.limit());
                    return new ParseResult(toks);
                }
                continue;
            }

            try {
                ByteBuffer dup = buf.duplicate();
                Response resp = new ResponseParser().parse(dup);
                if(resp != null) {
                    buf.position(dup.position());
                    getSessionTracker().responseReceived(resp);
                    m_logger.debug("Received response: " +
                                   resp.toDebugString());
                    toks.add(resp);
                }
                else {
                    done = true;
                    m_logger.debug("Need more bytes for response");
                }
            }
            catch(Exception ex) {
                m_logger.warn("Exception parsing server response", ex);
                declarePassthru();
                toks.add(PassThruToken.PASSTHRU);
                toks.add(new Chunk(buf));
                return new ParseResult(toks);
            }
        }

        //Compact the buffer
        buf = compactIfNotEmpty(buf, (1024*2));//TODO bscott a real value

        if(buf != null) {
            m_logger.debug("returning ParseResult with " +
                           toks.size() + " tokens and a buffer with " + buf.remaining() + " remaining");
        }
        return new ParseResult(toks, buf);
    }


}
