/**
 * $Id$
 */
package com.untangle.node.smtp;

import java.nio.ByteBuffer;

import com.untangle.node.smtp.mime.MIMEAccumulator;
import com.untangle.uvm.vnet.MetadataToken;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Token reprsenting the Begining of a MIME message. The {@link #getMIMEAccumulator MIMEAccumulator} member may have
 * only header bytes, or the entire message. Note, however, that receivers of a BeginMIMEToken should <b>not</b>
 * consider the message complete until receiving a {@link com.untangle.node.smtp.ContinuedMIMEToken ContinuedMIMEToken}
 * with its {@link com.untangle.node.smtp.ContinuedMIMEToken#isLast last} property set to true.
 * 
 */
public class BeginMIMEToken extends MetadataToken
{

    private MIMEAccumulator m_accumulator;
    private MessageInfo m_messageInfo;

    public BeginMIMEToken(MIMEAccumulator accumulator, MessageInfo messageInfo)
    {
        m_accumulator = accumulator;
        m_messageInfo = messageInfo;
    }

    /**
     * Accessor for the MessageInfo of the email being transmitted.
     */
    public MessageInfo getMessageInfo()
    {
        return m_messageInfo;
    }

    /**
     * Object which is gathering the MIME bytes for this email
     */
    public MIMEAccumulator getMIMEAccumulator()
    {
        return m_accumulator;
    }

    /**
     * Get a TokenStreamer for the initial contents of this message
     * 
     * @param byteStuffer
     *            the byte stuffer used for initial bytes. The stuffer will retain its state, so subsequent writes will
     *            cary-over any retained bytes.
     * 
     * @return the TCPStreamer
     */
    public TCPStreamer toStuffedTCPStreamer(ByteBufferByteStuffer byteStuffer)
    {
        return new ByteBtuffingTCPStreamer(m_accumulator.toTCPStreamer(), byteStuffer);
    }

    // ----------------- Inner Class -----------------------

    private class ByteBtuffingTCPStreamer implements TCPStreamer
    {

        private final TCPStreamer wrappedStreamer;
        private final ByteBufferByteStuffer bbbs;

        ByteBtuffingTCPStreamer(TCPStreamer wrapped, ByteBufferByteStuffer bbbs)
        {
            this.wrappedStreamer = wrapped;
            this.bbbs = bbbs;
        }

        @Override
        public boolean closeWhenDone()
        {
            return wrappedStreamer.closeWhenDone();
        }

        @Override
        public ByteBuffer nextChunk()
        {
            ByteBuffer next = (ByteBuffer) wrappedStreamer.nextChunk();
            if (next != null) {
                ByteBuffer ret = ByteBuffer.allocate(next.remaining() + (bbbs.getLeftoverCount() * 2));
                bbbs.transfer(next, ret);
                return ret;
            }
            return next;
        }
    }
}
