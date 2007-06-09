/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.impl;

import java.nio.ByteBuffer;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.tapi.Pipeline;
import com.untangle.mvvm.tapi.TCPSession;
import com.untangle.tran.token.AbstractParser;
import com.untangle.tran.token.Chunk;
import com.untangle.tran.token.ParseException;
import com.untangle.tran.token.ParseResult;
import com.untangle.tran.token.TokenStreamer;
import org.apache.log4j.Logger;

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
        m_pipeline = MvvmContextFactory.context().
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

    public final ParseResult parse(ByteBuffer buf) {

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
    }

    /**
     * Delegated "parse" method.  Superclass takes care
     * of some housekeeping before calling this method.
     * <br><br>
     * Note that if the casing is {@link #isPassthru in passthru}
     * then this method will not be called.
     */
    protected abstract ParseResult doParse(ByteBuffer buf);

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
