/**
 * $Id$
 */
package com.untangle.app.smtp;

import java.nio.ByteBuffer;

import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.app.smtp.mime.MIMETCPStreamer;
import com.untangle.uvm.vnet.MetadataToken;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Class representing a Complete MIME message. This will be issued if an upstream App has buffered a complete message.
 */
public class CompleteMIMEToken extends MetadataToken
{

    private static final int CHUNK_SZ = 1024 * 4;

    private final Logger m_logger = Logger.getLogger(CompleteMIMEToken.class);

    private final MimeMessage m_msg;
    private final SmtpMessageEvent m_msgInfo;

    /**
     * Initialize CompleteMIMEToken instance.
     *
     * @param  msg     MimeMessage to use.
     * @param  msgInfo SmtpMessageEvent message information.
     * @return         New CompleteMIMEToken instance.
     */
    public CompleteMIMEToken(MimeMessage msg, SmtpMessageEvent msgInfo) {
        m_msg = msg;
        m_msgInfo = msgInfo;
    }

    /**
     * Get the MIMEMessage member of this token
     *
     * @return The MimeMessage
     */
    public MimeMessage getMessage()
    {
        return m_msg;
    }

    /**
     * Get the SmtpMessageEvent associated with this email
     *
     * @return The SmtpMessageEvent.
     */
    public SmtpMessageEvent getSmtpMessageEvent()
    {
        return m_msgInfo;
    }

    /**
     * Get a TokenStreamer for the contents of this MIME Message (unstuffed)
     *
     * @param disposeWhenComplete If true, dispose when completed, false otherwise.
     * @param session AppTcpSession
     * @return the TokenStreamer
     */
    public TCPStreamer toUnstuffedTCPStreamer( boolean disposeWhenComplete, AppTCPSession session )
    {
        m_logger.debug("About to return a new MIMETCPStreamer");
        return createMIMETCPStreamer( disposeWhenComplete, session );
    }

    /**
     * Get a TokenStreamer for the contents of this MIME Message
     * 
     * @param disposeWhenComplete If true, dispose when completed, false otherwise.
     * @param session AppTcpSession
     * @return the TokenStreamer
     */
    public TCPStreamer toStuffedTCPStreamer( boolean disposeWhenComplete, AppTCPSession session )
    {
        m_logger.debug("About to return a new StuffingMIMETCPStreamer");
        return new StuffingMIMETCPStreamer(getMessage(), m_msgInfo, disposeWhenComplete, session);
    }

    /**
     * Method for subclasses to create a streamer.
     * @param disposeWhenComplete If true, dispose when completed, false otherwise.
     * @param session AppTcpSession
     * @return A new MIMETCPStreamer intance.
     */
    protected MIMETCPStreamer createMIMETCPStreamer( boolean disposeWhenComplete, AppTCPSession session )
    {
        return new MIMETCPStreamer(getMessage(), m_msgInfo, CHUNK_SZ, disposeWhenComplete, session);
    }

    /**
     * Perform buffer management on MIMETCPStreamer
     */
    private class StuffingMIMETCPStreamer extends MIMETCPStreamer
    {

        private ByteBufferByteStuffer m_bbbs = new ByteBufferByteStuffer();
        private final Logger m_logger = Logger.getLogger(CompleteMIMEToken.StuffingMIMETCPStreamer.class);
        private ByteBuffer m_readBuf = ByteBuffer.allocate(CHUNK_SZ);
        private boolean m_readLast = false;

        /**
         * Initialize StuffingMIMETCPStreamer intance.
         *
         * @param msg MimeMessage object.
         * @param messageInfo SmtpMessageEvent object.
         * @param disposeWhenComplete If true, dispose when completed, false otherwise.
         * @param session AppTcpSession
         */
        StuffingMIMETCPStreamer(MimeMessage msg, SmtpMessageEvent messageInfo, boolean disposeWhenComplete, AppTCPSession session)
        {
            super(msg, messageInfo, 0, disposeWhenComplete, session);
            m_logger.debug("Created Complete MIME message streamer (Stuffing)");
        }

        /**
         * Create a read buffer.
         *
         * @return An initalized ByteBuffer.
         */
        @Override
        protected ByteBuffer createReadBuf()
        {
            return m_readBuf.clear();
        }

        /**
         * Pull the next section ino the buffer.
         * @return ByteBuffer containing the next chunk.
         */
        @Override
        public ByteBuffer nextChunk()
        {
            m_logger.debug("Next ChunkToken called");

            ByteBuffer superRet = super.nextChunk();// This is actualy our "m_readBuf"
            if (superRet != null) {
                ByteBuffer sinkBuf = ByteBuffer.allocate(CHUNK_SZ);
                m_bbbs.transfer(superRet, sinkBuf);
                m_logger.debug("Returning a ByteBuffer of size: " + sinkBuf.remaining());
                return sinkBuf;
            } else {
                if (m_readLast) {
                    return null;
                } else {
                    m_readLast = true;
                    m_logger.debug("No more MIME to read");
                    ByteBuffer toWrap = m_bbbs.getLast(true);
                    m_logger.debug("Final wrapped buffer of size: " + toWrap.remaining());
                    return toWrap;
                }
            }
        }
    }

    /**
     * Erase temporary file if defined.
     */
    public void cleanupTempFile()
    {
        if (m_msgInfo != null && m_msgInfo.getTmpFile() != null) {
            try {
                m_msgInfo.getTmpFile().delete();
            } catch (Exception e) {
                // ignore
            }
        }
    }
}
