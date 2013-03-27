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

package com.untangle.node.smtp;

import java.io.File;
import java.nio.ByteBuffer;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.node.token.Casing;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;


public abstract class AbstractMailCasing extends Casing
{
    private final boolean m_trace;
    private final CasingTracer m_tracer;

    private final Logger m_logger =
        Logger.getLogger(AbstractMailCasing.class);


    public AbstractMailCasing(NodeTCPSession session,
                              boolean clientSide,
                              String protocolString)
    {

        m_trace = Boolean.parseBoolean(System.getProperty("com.untangle.node.mail.tracing"));

        if(m_logger.isEnabledFor(Level.DEBUG)) {
            m_logger.debug("Creating " +
                           (clientSide?"client":"server") + " " + protocolString + " Casing.  Client: " +
                           session.getClientAddr() + "(" + Integer.toString(session.getClientIntf()) + "), " +
                           "Server: " +
                           session.getServerAddr() + "(" + Integer.toString(session.getServerIntf()) + ")");
        }

        if(m_trace) {
            m_tracer = new CasingTracer(
                                        new File(System.getProperty("uvm.tmp.dir"), protocolString),
                                        session.id() + "_" + protocolString,
                                        clientSide);
        }
        else {
            m_tracer = null;
        }
    }

    /**
     * Test if tracing is enabled for this casing
     */
    public final boolean isTrace() {
        return m_trace;
    }

    /**
     * Callback from either parser or unparser,
     * indicating that we are entering passthru mode.
     * This is required as the passthru token may only
     * flow in one direction.  The casing will ensure that
     * both parser and unparser enter passthru.
     */
    public final void passthru() {
        ((AbstractMailParser) parser()).passthru();
        ((AbstractMailUnparser) unparser()).passthru();
    }

    /**
     * Trace a parse message.  If tracing is not enabled,
     * this call is a noop
     */
    public final void traceParse(ByteBuffer buf) {
        if(m_trace) {
            m_tracer.traceParse(buf);
        }
    }
    /**
     * Trace an unparse message.  If tracing is not enabled,
     * this call is a noop
     */
    public final void traceUnparse(ByteBuffer buf) {
        if(m_trace) {
            m_tracer.traceUnparse(buf);
        }
    }


    /**
     * Wraps a stream for tracing.  If tracing is not enabled,
     * <code>streamer</code> is returned
     */
    public final TCPStreamer wrapUnparseStreamerForTrace(TCPStreamer streamer) {
        if(m_trace) {
            return m_tracer.wrapUnparseStreamerForTrace(streamer);
        }
        else {
            return streamer;
        }
    }

    public void endSession(boolean calledFromParser) {
        if(m_trace) {
            m_tracer.endSession(calledFromParser);
        }
    }
}
