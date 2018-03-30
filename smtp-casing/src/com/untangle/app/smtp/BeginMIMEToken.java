/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import com.untangle.app.smtp.mime.MIMEAccumulator;
import com.untangle.uvm.vnet.MetadataToken;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Token reprsenting the Begining of a MIME message. The {@link #getMIMEAccumulator MIMEAccumulator} member may have
 * only header bytes, or the entire message. Note, however, that receivers of a BeginMIMEToken should <b>not</b>
 * consider the message complete until receiving a {@link com.untangle.app.smtp.ContinuedMIMEToken ContinuedMIMEToken}
 * with its {@link com.untangle.app.smtp.ContinuedMIMEToken#isLast last} property set to true.
 * 
 */
public class BeginMIMEToken extends MetadataToken
{

    private MIMEAccumulator m_accumulator;
    private SmtpMessageEvent m_messageInfo;

    /**
     * Intialize object of BeginMIMEToken.
     * 
     * @param  accumulator MIMEAccumulator accuumulator.
     * @param  messageInfo SmtpMessageEvent message informaiton.
     * @return             Instance of BeginMIMEToken.
     */
    public BeginMIMEToken(MIMEAccumulator accumulator, SmtpMessageEvent messageInfo)
    {
        m_accumulator = accumulator;
        m_messageInfo = messageInfo;
    }

    /**
     * Accessor for the SmtpMessageEvent of the email being transmitted.
     *
     * @return SmtpMessageEvent of accessor.
     */
    public SmtpMessageEvent getSmtpMessageEvent()
    {
        return m_messageInfo;
    }

    /**
     * Object which is gathering the MIME bytes for this email
     *
     * @return MIMEAccumulator of gathering object.
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

    /**
     * Handle TCP stream.
     */
    private class ByteBtuffingTCPStreamer implements TCPStreamer
    {

        private final TCPStreamer wrappedStreamer;
        private final ByteBufferByteStuffer bbbs;

        /**
         * Initialize TCP stream handler.
         *
         * @param wrapped TCPStreamer.
         * @param bbbs ByteBufferStuffer.
         */
        ByteBtuffingTCPStreamer(TCPStreamer wrapped, ByteBufferByteStuffer bbbs)
        {
            this.wrappedStreamer = wrapped;
            this.bbbs = bbbs;
        }

        /**
         * Close the wrapped streamer.
         *
         * @return result of closing TCPStreamer object.
         */
        @Override
        public boolean closeWhenDone()
        {
            return wrappedStreamer.closeWhenDone();
        }

        /**
         * Get the next chunk from the TCP streamer.
         * @return ByteBuffer of the next chunk.
         */
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
