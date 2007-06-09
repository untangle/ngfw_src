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

package com.untangle.tran.mail.papi;

import java.nio.ByteBuffer;

import com.untangle.mvvm.tapi.event.TCPStreamer;
import com.untangle.tran.token.MetadataToken;
import org.apache.log4j.Logger;


/**
 * Token reprsenting the Begining
 * of a MIME message.  The {@link #getMIMEAccumulator MIMEAccumulator}
 * member may have only header bytes, or the entire message.  Note, however,
 * that receivers of a BeginMIMEToken should <b>not</b> consider the message
 * complete until receiving a {@link com.untangle.tran.mail.papi.ContinuedMIMEToken ContinuedMIMEToken}
 * with its {@link com.untangle.tran.mail.papi.ContinuedMIMEToken#isLast last}
 * property set to true.
 *
 */
public class BeginMIMEToken
    extends MetadataToken {

    private final Logger m_logger =
        Logger.getLogger(BeginMIMEToken.class);

    private MIMEAccumulator m_accumulator;
    private MessageInfo m_messageInfo;


    public BeginMIMEToken(MIMEAccumulator accumulator,
                          MessageInfo messageInfo) {
        m_accumulator = accumulator;
        m_messageInfo = messageInfo;
    }

    /**
     * Accessor for the MessageInfo of the email being
     * transmitted.
     */
    public MessageInfo getMessageInfo() {
        return m_messageInfo;
    }

    /**
     * Object which is gathering the MIME bytes for this email
     */
    public MIMEAccumulator getMIMEAccumulator() {
        return m_accumulator;
    }

    /**
     * Get a TCPStreamer for the initial contents
     * of this message, without byte stuffing.
     *
     * @return the TCPStreamer
     */
    public TCPStreamer toUnstuffedTCPStreamer() {
        return m_accumulator.toTCPStreamer();
    }

    /**
     * Get a TokenStreamer for the initial
     * contents of this message
     *
     * @param byteStuffer the byte stuffer used for initial bytes.  The
     *        stuffer will retain its state, so subsequent writes will
     *        cary-over any retained bytes.
     *
     * @return the TCPStreamer
     */
    public TCPStreamer toStuffedTCPStreamer(ByteBufferByteStuffer byteStuffer) {
        return new ByteBtuffingTCPStreamer(m_accumulator.toTCPStreamer(),
                                           byteStuffer);
    }

    //----------------- Inner Class -----------------------

    private class ByteBtuffingTCPStreamer
        implements TCPStreamer {

        private final TCPStreamer m_wrappedStreamer;
        private final ByteBufferByteStuffer m_bbbs;

        ByteBtuffingTCPStreamer(TCPStreamer wrapped,
                                ByteBufferByteStuffer bbbs) {
            m_wrappedStreamer = wrapped;
            m_bbbs = bbbs;
        }

        public boolean closeWhenDone() {
            return m_wrappedStreamer.closeWhenDone();
        }

        public ByteBuffer nextChunk() {
            ByteBuffer next = m_wrappedStreamer.nextChunk();
            if(next != null) {
                ByteBuffer ret = ByteBuffer.allocate(next.remaining() +
                                                     (m_bbbs.getLeftoverCount()*2));
                m_bbbs.transfer(next, ret);
                return ret;
            }
            return next;
        }
    }
}
