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

package com.untangle.node.mail.impl;

import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.mail.papi.FatalMailParseException;
import com.untangle.node.token.AbstractParser;
import com.untangle.node.token.Chunk;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.ParseResult;
import com.untangle.node.token.TokenStreamer;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.TCPSession;

/**
 * Base class for the mail parsers
 */
public abstract class AbstractMailParser
    extends AbstractParser {

    private final Logger m_logger =
        Logger.getLogger(AbstractMailParser.class);

    private final Pipeline m_pipeline;
    private final AbstractMailCasing m_parentCasing;
    private final boolean m_trace;
    private boolean m_passthru = false;
    private final String m_protocol;

    /**
     * @param session the session
     * @param parent the parent casing
     * @param clientSide true if this is a client-side casing
     * @param protocolName the name of the protocol (i.e. "smtp").
     */
    protected  AbstractMailParser(TCPSession session,
                                  AbstractMailCasing parent,
                                  boolean clientSide,
                                  String protocolName) {

        super(session, clientSide);
        m_trace = parent.isTrace();
        m_protocol = protocolName;
        m_parentCasing = parent;
        m_pipeline = LocalUvmContextFactory.context().
            pipelineFoundry().getPipeline(session.id());
    }

    public AbstractMailCasing getParentCasing() {
        return m_parentCasing;
    }
    public Pipeline getPipeline() {
        return m_pipeline;
    }

    /**
     * Is the casing currently in passthru mode
     */
    protected boolean isPassthru() {
        return m_passthru;
    }

    /**
     * Called by the unparser to declare that
     * we are now in passthru mode.  This is called
     * either because of a parsing error by the caller,
     * or the reciept of a passthru token.
     *
     */
    protected void declarePassthru() {
        m_passthru = true;
        m_parentCasing.passthru();
    }

    /**
     * Called by the casing to declare that this
     * instance should now be in passthru mode.
     */
    protected final void passthru() {
        m_passthru = true;
    }

    @Override
    public final TokenStreamer endSession() {
        m_logger.debug("(" +
                       m_protocol + ")(" +
                       (isClientSide()?"client":"server") + ") endSession()");
        getParentCasing().endSession(isClientSide());
        return super.endSession();
    }

    public void handleFinalized() {
        m_logger.debug("(" +
                       m_protocol + ")(" +
                       (isClientSide()?"client":"server") + ") handleFinalized()");
    }

    public final ParseResult parse(ByteBuffer buf) throws ParseException {

        try {
            if(m_trace) {
                ByteBuffer dup = buf.duplicate();
                ParseResult ret = isPassthru()?
                    new ParseResult(new Chunk(buf)):
                    doParse(buf);
                if(ret != null) {
                    ByteBuffer readBuf = ret.getReadBuffer();
                    if(readBuf != null && readBuf.position() > 0) {
                        if(readBuf.position() >= dup.remaining()) {
                            //The subclass pushed-back the whole buffer
                            dup = ByteBuffer.allocate(0);
                        }
                        else {
                            dup.limit(dup.limit()-readBuf.position());
                        }
                    }
                }
                getParentCasing().traceParse(dup);
                return ret;
            }
            return isPassthru()?
                new ParseResult(new Chunk(buf)):
                doParse(buf);
        } catch (FatalMailParseException exn) {
            getSession().shutdownClient();
            getSession().shutdownServer();
            return new ParseResult();
        }
    }

    /**
     * Delegated "parse" method.  Superclass takes care
     * of some housekeeping before calling this method.
     * <br><br>
     * Note that if the casing is {@link #isPassthru in passthru}
     * then this method will not be called.
     */
    protected abstract ParseResult doParse(ByteBuffer buf)
        throws FatalMailParseException;

    public final ParseResult parseEnd(ByteBuffer buf)
        throws ParseException {
        if (buf.hasRemaining()) {
            m_logger.debug("(" +
                           m_protocol + ")(" +
                           (isClientSide()?"client":"server") + ") Passing final chunk of size: " + buf.remaining());
            return new ParseResult(new Chunk(buf));
        }
        return new ParseResult();
    }
}
