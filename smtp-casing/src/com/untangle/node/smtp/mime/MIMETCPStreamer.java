/**
 * $Id: MIMETCPStreamer.java 34539 2013-04-12 05:06:33Z dmorris $
 */
package com.untangle.node.smtp.mime;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MessageInfo;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Class which acts as a TCPStreamer, using the contents of a MIMEMessage. Users should not directly have to use this
 * (it is public only because of subclassing/packaging reasons). <br>
 * <br>
 * Note that the MIMEMessage is first converted to a file during the constructor, so {@link #getFileLength
 * getFileLength} will return the total number of bytes to be returned. If an error is encountered converting the
 * MIMEMessage to a file, the length of bytes returned is 0 (and the error is logged).
 */
public class MIMETCPStreamer implements TCPStreamer
{
    private final int m_chunkSz;
    private FileInputStream m_fos;
    private FileChannel m_channel;
    private final Logger m_logger = Logger.getLogger(MIMETCPStreamer.class);
    private final boolean m_disposeWhenComplete;
    private boolean m_closed = false;
    private long m_fileLength;
    private final MimeMessage m_msg;
    private final MessageInfo messageInfo;
    File file;

    /**
     * Construct a new MIMETCPStreamer
     * 
     * @param msg
     *            the MIMEMessage
     * @param pipeline
     *            the pipeline (for creating temp files)
     * @param readChunkSz
     *            the size of the read buffer for the file
     * @param disposeWhenComplete
     *            if true, the MIMEMessage's {@link com.untangle.node.smtp.mime.MIMEPart#dispose dispose} method will be
     *            called when streaming is complete or an error is encountered.
     */
    public MIMETCPStreamer(MimeMessage msg, MessageInfo messageInfo, final Pipeline pipeline, int readChunkSz,
            boolean disposeWhenComplete) {

        m_msg = msg;
        m_chunkSz = readChunkSz;
        m_disposeWhenComplete = disposeWhenComplete;
        this.messageInfo = messageInfo;
        try {
            writeToFile();
            m_logger.debug("File is of length: " + file.length());
            m_fos = new FileInputStream(file);
            m_channel = m_fos.getChannel();
            m_fileLength = file.length();
        } catch (Exception ex) {
            m_fileLength = 0;
            m_logger.error(ex);
            close();
        }
    }

    private void writeToFile() throws IOException
    {
        FileOutputStream fOut = null;
        try {
            try {
                if (file != null)
                    file.delete();
            } catch (Exception ignore) {
            }
            file = File.createTempFile("MIMEMessage-", null);
            fOut = new FileOutputStream(file);
            BufferedOutputStream bufOut = new BufferedOutputStream(fOut);
            MIMEOutputStream mimeOut = new MIMEOutputStream(bufOut);
            m_msg.writeTo(mimeOut);
            mimeOut.flush();
            bufOut.flush();
            fOut.flush();
            fOut.close();
        } catch (MessagingException ex) {
            try {
                fOut.close();
            } catch (Exception ignore) {
            }
            IOException ex2 = new IOException();
            ex2.initCause(ex);
            throw ex2;
        } catch (IOException ex) {
            try {
                fOut.close();
            } catch (Exception ignore) {
            }
            IOException ex2 = new IOException();
            ex2.initCause(ex);
            throw ex;
        }
    }

    /**
     * Get the length of the file.
     */
    public long getFileLength()
    {
        return m_fileLength;
    }

    private void close()
    {
        if (m_closed) {
            return;
        }
        m_closed = true;
        try {
            m_fos.close();
        } catch (Exception ignore) {
        }
        m_fos = null;
        m_channel = null;
        if (m_disposeWhenComplete) {
            try {
                messageInfo.getTmpFile().delete();
                if (file != null)
                    file.delete();
            } catch (Exception e) {
                // ignore
            }
            m_logger.debug("Disposing of MIME file");
        }
    }

    public boolean closeWhenDone()
    {
        return false;
    }

    /**
     * Method creates a new read buffer. By default, a new buffer of length "readChunkSz" is allocated and used. This
     * method may be overidden by subclasses, which do not intend to return this buffer to the ultimate caller of
     * {@link #nextChunk nextChunk}. In such a case, a read buffer may be re-used (of the contents of the read are going
     * to be modified anyway.
     */
    protected ByteBuffer createReadBuf()
    {
        return ByteBuffer.allocate(m_chunkSz);
    }

    public ByteBuffer nextChunk()
    {
        m_logger.debug("Next Chunk called");
        if (m_channel == null) {
            return null;
        }
        try {
            ByteBuffer ret = createReadBuf();
            if (m_channel.read(ret) < 0) {
                m_logger.debug("Reached end-of-file");
                close();
                return null;
            } else {
                ret.flip();
                return ret;
            }
        } catch (Exception ex) {
            m_logger.error(ex);
            close();
            return null;
        }
    }
}
