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

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.PassThruToken;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Base class for the SmtpClient/ServerUnparser
 */
public abstract class AbstractMailUnparser
    extends AbstractUnparser {

    private final Pipeline m_pipeline;
    private final AbstractMailCasing m_parentCasing;
    private final Logger m_logger =
        Logger.getLogger(AbstractMailUnparser.class);
    private final boolean m_trace;
    private boolean m_passthru = false;
    private final String m_protocol;

    protected AbstractMailUnparser(NodeTCPSession session,
                                   AbstractMailCasing parent,
                                   boolean clientSide,
                                   String protocolName) {

        super(session, clientSide);
        m_protocol = protocolName;
        m_trace = parent.isTrace();
        m_parentCasing = parent;
        m_pipeline = UvmContextFactory.context().
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

    public UnparseResult unparse(Token token) {
        if(token instanceof PassThruToken) {
            m_logger.debug("Received PassThruToken");
            declarePassthru();//Inform the parser of this state
            return UnparseResult.NONE;
        }
        if(m_trace) {
            UnparseResult ret = doUnparse(token);
            if(ret != null) {
                if(ret.isStreamer()) {
                    ret = new UnparseResult(getParentCasing().wrapUnparseStreamerForTrace(ret.getTcpStreamer()));
                }
                else {
                    ByteBuffer[] bufs = ret.result();
                    if(bufs != null) {
                        for(ByteBuffer buf : bufs) {
                            if(buf != null) {
                                getParentCasing().traceUnparse(buf);
                            }
                        }
                    }
                }
            }
            return ret;
        }
        return doUnparse(token);
    }

    /**
     * Delegated "unparse" method.  The superclass performs
     * some housekeeping before calling this method.
     * <br><br>
     * Note that subclasses should <b>not</b> worry about
     * tracing, or receiving Passthru tokens
     */
    protected abstract UnparseResult doUnparse(Token token);

    public final TCPStreamer endSession() {
        m_logger.debug("(" +
                       m_protocol + ")(" +
                       (isClientSide()?"client":"server") + ") End Session");
        getParentCasing().endSession(isClientSide());
        return null;
    }


    @Override
    public void handleFinalized() {
        m_logger.debug("(" +
                       m_protocol + ")(" +
                       (isClientSide()?"client":"server") + ") handleFinalized");
    }

}
