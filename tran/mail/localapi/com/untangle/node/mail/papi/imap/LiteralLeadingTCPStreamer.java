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

package com.untangle.node.mail.papi.imap;

import java.nio.ByteBuffer;

import com.untangle.uvm.tapi.event.TCPStreamer;

/**
 * Wraps a TCPStreamer, leading with an IMAP literal "{nnn}CRLF"
 */
class LiteralLeadingTCPStreamer
    implements TCPStreamer {

    private final TCPStreamer m_wrappedStreamer;
    private final int m_literalLength;
    private boolean m_wroteLiteral;

    LiteralLeadingTCPStreamer(TCPStreamer wrapped,
                              int literalLength) {

        m_wrappedStreamer = wrapped;
        m_literalLength = literalLength;
        m_wroteLiteral = false;
    }

    public boolean closeWhenDone() {
        return m_wrappedStreamer.closeWhenDone();
    }

    public ByteBuffer nextChunk() {
        if(!m_wroteLiteral) {
            m_wroteLiteral = true;
            return ByteBuffer.wrap(("{" + m_literalLength + "}\r\n").getBytes());
        }
        ByteBuffer ret = m_wrappedStreamer.nextChunk();
        if(ret == null) {
            doneStreaming();
        }
        return ret;
    }

    /**
     * Callback for subclasses to do something interesting (close files, etc)
     * when Streaming is complete.  Default implementation does nothing.
     */
    protected void doneStreaming() {
    }
}
