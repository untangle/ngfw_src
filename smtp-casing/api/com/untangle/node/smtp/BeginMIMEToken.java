/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.smtp;

import java.nio.ByteBuffer;

import com.untangle.node.token.MetadataToken;
import com.untangle.uvm.vnet.event.TCPStreamer;


/**
 * Token reprsenting the Begining
 * of a MIME message.  The {@link #getMIMEAccumulator MIMEAccumulator}
 * member may have only header bytes, or the entire message.  Note, however,
 * that receivers of a BeginMIMEToken should <b>not</b> consider the message
 * complete until receiving a {@link com.untangle.node.smtp.ContinuedMIMEToken ContinuedMIMEToken}
 * with its {@link com.untangle.node.smtp.ContinuedMIMEToken#isLast last}
 * property set to true.
 *
 */
public class BeginMIMEToken
    extends MetadataToken {

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
