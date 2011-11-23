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

package com.untangle.node.mail.impl.pop;

import static com.untangle.node.util.BufferUtil.findCrLf;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.untangle.node.mail.PopCasing;
import com.untangle.node.mail.papi.pop.PopCommand;
import com.untangle.node.mail.papi.pop.PopCommandMore;
import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.Release;
import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;
import com.untangle.uvm.vnet.TCPSession;

public class PopClientParser extends AbstractParser
{
    private final Logger logger = Logger.getLogger(getClass());

    private enum State {
        COMMAND,
        AUTH_LOGIN
    };

    //unused// private final Pipeline pipeline;
    private final PopCasing zCasing;

    private State state;

    // constructors -----------------------------------------------------------

    public PopClientParser(TCPSession session, PopCasing zCasing)
    {
        super(session, true);
        lineBuffering(false);

        //unused// pipeline = UvmContextFactory.context().pipelineFoundry().getPipeline(session.id());
        this.zCasing = zCasing;

        state = State.COMMAND;
    }

    // Parser methods ---------------------------------------------------------

    public ParseResult parse(ByteBuffer buf) throws ParseException
    {
        //logger.debug("parse(" + AsciiCharBuffer.wrap(buf) + "), " + buf);

        List<Token> zTokens = new LinkedList<Token>();

        boolean bDone = false;

        while (false == bDone) {
            switch (state) {
            case COMMAND:
                logger.debug("COMMAND state, " + buf);

                if (1 < findCRLFEnd(buf)) {
                    ByteBuffer dup = buf.duplicate();

                    try {
                        PopCommand cmd;

                        if (null == zCasing.getUser()) {
                            /* we only check for user once per session */
                            cmd = PopCommand.parseUser(buf);
                            if (true == cmd.isAuthLogin()) {
                                logger.debug("entering AUTH LOGIN state");
                                state = State.AUTH_LOGIN;
                            } else if (true == cmd.isUser()) {
                                zCasing.setUser(cmd.getUser());
                            } else if (true == cmd.isTLS()) {
                                // TLS usually initiates before user sign-in
                                logger.warn("TLS negotiation initiated (unable to monitor rest of session); releasing session");
                                logger.debug("returning ParseResult(Release( " + cmd + "))");
                                session.release();
                                return new ParseResult(new Release(dup));
                            }
                        } else {
                            cmd = PopCommand.parse(buf);
                            if (true == cmd.isTLS()) {
                                // in case TLS initiates after user sign-in
                                logger.warn("TLS negotiation initiated after user sign-in (unable to monitor rest of session); releasing session");
                                logger.debug("returning ParseResult(Release( " + cmd + "))");
                                session.release();
                                return new ParseResult(new Release(dup));
                            } // else do not care about command
                        }

                        zTokens.add(cmd);
                    } catch (ParseException exn) {
                        /* long command may break before CRLF sequence
                         * so if parse fails,
                         * we assume long command spans multiple buffers
                         */
                        zTokens.add(new PopCommandMore(dup));
                        logger.debug("command (more): " + dup + ", " + exn);
                        /* fall through */
                    }

                    buf = null; /* buf has been consumed */
                    bDone = true;
                } else {
                    logger.debug("buf does not contain CRLF");

                    if (buf.limit() == buf.capacity()) {
                        /* casing adapter will handle full buf for us */
                        throw new ParseException("client read buf is full and does not contain CRLF; traffic cannot be POP; releasing session: " + buf);
                    }

                    /* wait for more data */
                    bDone = true;
                }

                break;

            case AUTH_LOGIN:
                logger.debug("AUTH LOGIN state, " + buf);

                /* we only check for user once per session */
                PopCommandMore cmdMore = PopCommandMore.parseAuthUser(buf);
                zCasing.setUser(cmdMore.getUser());

                zTokens.add(cmdMore);

                logger.debug("re-entering COMMAND state");
                state = State.COMMAND;

                buf = null; /* buf has been consumed */
                bDone = true;
                break;

            default:
                throw new IllegalStateException("unknown state: " + state);
            }
        }

        if (null != buf) {
            buf.position(buf.limit());
            buf.limit(buf.capacity());

            //logger.debug("reset (compacted) buf to add more data: " + buf);
        }

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
}
