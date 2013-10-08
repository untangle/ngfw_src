/**
 * $Id: MIMEAccumulator.java 34539 2013-04-12 05:06:33Z dmorris $
 */
package com.untangle.node.smtp.mime;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

import com.untangle.node.smtp.MessageInfo;
import com.untangle.uvm.vnet.Pipeline;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Class used to accumulate MIME bytes. Usage is tricky and subtle, for a few reasons:
 * <ul>
 * <li>
 * "Ownership" of the underlying file is subtle. Before a complete MIME message has been parsed, the accumulator owns
 * the file. Afterwards, users should consider the file "owned" by the MIME. Once ownership of the file has been
 * transfered, using the {@link #dispose dispose} method could hose callers. Instead, use {@link #closeInput closeInput}
 * which will only close open streams.</li>
 * <li>
 * If multiple nodes are in "buffer-and-trickle" where they are accumulating MIME yet also passing it along, there is
 * confusion as-to if a given MIMEChunk should (a) be written to disk and (b) should be unparsed. To avoid this
 * confusion, all MIMEChunks are members of a given MIMEAccumulator, and have methods to determine if they should be
 * unparsed. Appending a chunk to the file is simplified via the {@link #appendChunkToFile appendChunkToFile} which
 * silently ignores duplicate calls to append the same chunk.</li>
 * </ul>
 * <br>
 * <br>
 * If a MIMEAccumulator ever reaches an unparser, the {@link #toTCPStreamer toTCPStreamer} method can be used to obtain
 * a streamer destined for the endpoint. The MIMEAccumulator then "remembers" this event, such that subsequent
 * MIMEChunks may or may not be written-out depending on when (if) they were appended to the file. If they were appended
 * <i>before</i> the unparse event, then they should not be unparsed if received. In other words, please use the
 * "shouldUnparse" method on MIMEChunk before unparsing. <br>
 * <br>
 * No one should ever see a MIMEChunk before the MIMEAccumulator to-which it belongs.
 */
public class MIMEAccumulator
{

    private final Logger m_logger = Logger.getLogger(MIMEAccumulator.class);
    private static final int CHUNK_SZ = 1024 * 4;

    private File m_file;
    private FileOutputStream m_fileOut;
    private FileChannel m_fileChannel;
    private InternetHeaders m_headers;
    private FileMIMESource m_fileMIMESource;
    private int m_headersLen;
    private boolean m_unparsed = false;
    private int m_greatestChunkAppendedAndUnparsed = -1;
    private int m_chunkIndex = 0;
    private MimeMessage m_mimeMessage;

    /**
     * Class used to represent a chunk of MIME accociated with a given MIMEAccumulator. MIMEChunks may not have useful
     * data (if for example they are simply a marker of {@link #isLast the end} of a MIME message). <br>
     * <br>
     * MIMEChunks also may or may not be written to file. To write a given chunk to a file, use the "appendChunkToFile"
     * method of the MIMEAccumulator. <br>
     * <br>
     * Before ever unparsing a MIMEChunk, please use the {@link #shouldUnparse shouldUnparse} method, which will catch
     * the case of a MIMEChunk passed <b>and</b> written to file.
     */
    public class MIMEChunk
    {
        private boolean m_isLast = false;
        private int m_index;
        private ByteBuffer m_buf;
        private boolean m_writtenToFile = false;

        private MIMEChunk(ByteBuffer buf, boolean isLast, int index) {
            m_isLast = isLast;
            m_buf = buf;
            m_index = index;
        }

        /**
         * Does this chunk represent the end of the message. Such chunks may or may not {@link #hasData have data}.
         */
        public boolean isLast()
        {
            return m_isLast;
        }

        /**
         * Does this chunk have any data. Boundary cases in parsing mayt result in blank chunks.
         */
        public boolean hasData()
        {
            return m_buf != null && m_buf.hasRemaining();
        }

        /**
         * Get the underlying data. Note that you should first check {@link #hasData hasData} to see if null may be
         * returned. <br>
         * <br>
         * <b>If unparsing, please first consult {@link #shouldUnparse shouldUnparse}</b>
         */
        public ByteBuffer getData()
        {
            return m_buf == null ? null : m_buf.duplicate();
        }

        /**
         * Length of internal buffer. May be 0 is internal buffer is null
         */
        public int length()
        {
            return m_buf == null ? 0 : m_buf.remaining();
        }

        private void writtenToFile()
        {
            m_writtenToFile = true;
        }

        private boolean isWrittenToFile()
        {
            return m_writtenToFile;
        }

        private int getIndex()
        {
            return m_index;
        }

        /**
         * Test if this chunk should be unparsed. False will be returned if this was passed t0 "appendChunkToFile" yet
         * the MIMEAccumulator has yet to be unparsed.
         */
        public boolean shouldUnparse()
        {
            return shouldUnparseImpl(this);
        }

        /**
         * Obviously only for deep debugging
         */
        public void superDebugMe(Logger logger, String prefix)
        {
            String body = getData() == null ? "<null>" : com.untangle.node.util.ASCIIUtil.bbToString(getData());
            logger.debug(prefix + " " + toString() + " \"" + body + "\"");
        }

        private boolean isSameAccumulator(MIMEAccumulator ref)
        {
            return ref == MIMEAccumulator.this;
        }

        public String toString()
        {
            return "[CID:" + getIndex() + "]";
        }
    }

    /**
     * Construct an accumulator. <br>
     * <br>
     * If exception thrown, no open files or descriptors are left.
     * 
     * @param pipeline
     *            the pipeline (for creating a temp file).
     */
    public MIMEAccumulator(Pipeline pipeline) throws IOException {
        m_logger.debug("Opening temp file to buffer MIME");
        try {
            m_file = File.createTempFile("MIMEAccumulator-", null);
            m_fileOut = new FileOutputStream(m_file);
            m_fileChannel = m_fileOut.getChannel();
        } catch (IOException ex) {
            m_logger.error("Exception creating a temp file for MIME message", ex);
            try {
                m_fileOut.close();
            } catch (Exception ignore) {
            }
            try {
                m_file.delete();
            } catch (Exception ignore) {
            }
            IOException ex2 = new IOException("Exception creating a temp file for MIME message");
            ex2.initCause(ex);
            throw ex2;
        }
    }

    /**
     * Method to associate a chunk with this MIMEAccumulator. Does not implicitly {@link #appendChunkToFile add to the
     * underlying file}.
     * 
     * @param buf
     *            the buffer (may be null)
     * @param isLast
     *            true if this is the last chunk in the MIME message
     */
    public MIMEAccumulator.MIMEChunk createChunk(ByteBuffer buf, boolean isLast)
    {
        int next = nextIndex();
        m_logger.debug("[createChunk()] Creating MIMEChunk " + next + " with "
                + (buf == null ? "0" : Integer.toString(buf.remaining())) + " bytes");
        return new MIMEChunk(buf, isLast, nextIndex());
    }

    /**
     * Add the given chunk to this accumulator. An error will occur if the chunk was not {@link createChunk created by
     * this accumulator}. <br>
     * <br>
     * The boolean is used to convey outcome (success/failure). Failures have already been logged. If error, chunk was
     * not written. If the chunk has already been written (for example, two Nodes in "buffer-and-passthru" mode), then
     * the second write is silently ignored. <br>
     * <br>
     * If an error does occur, no streams are closed.
     * 
     * @param chunk
     *            the chunk
     * @return true if successful.
     */
    public boolean appendChunkToFile(MIMEAccumulator.MIMEChunk chunk)
    {
        if (!chunk.isSameAccumulator(this)) {
            throw new RuntimeException("Chunk not for this MIME file");
        }
        if (!chunk.hasData()) {
            m_logger.debug("[appendChunkToFile()] Chunk " + chunk + " has no data.  Nothing to append");
            return true;
        }
        if (chunk.isWrittenToFile()) {
            m_logger.debug("[appendChunkToFile()] Chunk " + chunk + " already appended to this file");
            return true;
        }
        if (!appendToFile(chunk.getData())) {
            m_logger.debug("[appendChunkToFile()] Error appending chunk " + chunk);
            return false;
        }
        if (!m_unparsed) {
            m_logger.debug("[appendChunkToFile()] Assign chunk " + chunk + " greatest chunk yet unparsed");
            m_greatestChunkAppendedAndUnparsed = chunk.getIndex();
        }
        chunk.writtenToFile();
        return true;
    }

    /**
     * Add bytes to the underlying file for the header. Boolean return conveys success. <br>
     * <br>
     * Return of false means error, but things are not closed. Bytes in the buffer <b>are</b> consumed.
     * 
     * If <code>isLast</code> then the {@link #getHeadersLength headers length} is assigned as the length of the file
     * <b>after</b> this insert. Passing a null (or blank) buf with <code>isLast</code> true is the way to assign null
     * headers.
     * 
     * Note that if there are any headers (or the headers were simply a blank line) the terminating CRLF should be
     * appended.
     * 
     * @param buf
     *            the buffer (may be null)
     * @param isLast
     *            is this the last buffer for the headers
     * 
     * @return true if success.
     */
    public boolean addHeaderBytes(ByteBuffer buf, boolean isLast)
    {
        if (buf != null && buf.hasRemaining()) {
            if (!appendToFile(buf)) {
                return false;
            }
        }
        if (isLast) {
            try {
                m_fileOut.flush();
            } catch (Exception wtf) {
                m_logger.error("Error adding header bytes", wtf);
            }
            m_headersLen = (int) m_file.length();
        }
        return true;
    }

    /**
     * Returns true if the headers were parsed correctly. As a side effect, the CPMIMEAccumulator is "completed" by
     * having its MIMESource set. <br>
     * <br>
     * This method may be called more than once, although after the first call the headers are cached. <br>
     * <br>
     * If there is an error, the MIMESource is not set and the headers are blank. The MIMEAccumulator is not cleaned-up,
     * but there should be no temp streams open.
     * 
     * @return null if there was an error
     */
    public InternetHeaders parseHeaders()
    {
        if (m_headers != null) {
            return m_headers;
        }
        if (getHeadersLength() == 0) {
            m_logger.debug("Parsing headers, yet no header bytes.  Assume " + "blank headers");

            m_headers = new InternetHeaders();
            return m_headers;
        }
        MIMEParsingInputStream in = null;
        try {
            m_fileMIMESource = new FileMIMESource(m_file);
            in = m_fileMIMESource.getInputStream();
            m_headers = new InternetHeaders(in);
            m_headersLen = (int) in.position();
            in.close();
            return m_headers;
        } catch (Exception ex) {
            m_logger.error("Error parsing MIME body", ex);
            try {
                in.close();
            } catch (Exception ignore) {
            }
            m_fileMIMESource = null;
            return null;
        }
    }

    /**
     * Get the length of the headers (in bytes) including any terminator (CRLF).
     */
    public int getHeadersLength()
    {
        return m_headersLen;
    }

    /**
     * Method called when we are {@link #parseHeaders parsing headers}, and encounter an exception parsing the headers.
     * Returns any accumulated header bytes. If there are none, a blank buffer is returned. <br>
     * <br>
     * If there is an error, the MIMEAccumulator is not closed but any temp streams are. <br>
     * <br>
     * Returned buffer is flipped. <br>
     * <br>
     * return of null means you're hosed. {@link #dispose Nuke} the accumulator. There is nothing else to do.
     * 
     * @return any bytes from the header trapped in the file.
     */
    public ByteBuffer drainFileToByteBuffer()
    {
        if (m_file.length() == 0) {
            return ByteBuffer.allocate(0);
        }
        FileInputStream fIn = null;
        try {
            fIn = new FileInputStream(m_file);
            ByteBuffer buf = ByteBuffer.allocate((int) m_file.length());
            FileChannel fc = fIn.getChannel();
            while (buf.hasRemaining()) {
                fc.read(buf);
            }
            fIn.close();
            buf.flip();
            return buf;
        } catch (Exception ex) {
            try {
                fIn.close();
            } catch (Exception ignore) {
            }
            m_logger.error("Error draining headers trapped in file to buffer");
            return null;
        }
    }

    /**
     * Get the size of the underlying file (the number of bytes accumulated thus-far). This includes the header bytes
     */
    public int fileSize()
    {
        return (int) m_file.length();
    }

    /**
     * Parse the accumulated bytes into a MIME message. This method may be called more than once, yet the parsed message
     * is cached. <br>
     * <br>
     * Note that once parsed, the caller should assume the MIMEMessage is responsible for the lifecycle of the
     * underlying file. <br>
     * <br>
     * This method implicitly calls {@link #closeInput closeInput}. <br>
     * <br>
     * Null is returned if there is an error, but the caller is responsible for any cleanup.
     * 
     * @return a parsed MIMEMEssage, or null if an error occured. If there is an error, it has been logged.
     */
    public MimeMessage parseBody(MessageInfo messageInfo)
    {
        if (m_mimeMessage != null) {
            return m_mimeMessage;
        }
        if (m_fileMIMESource == null) {
            m_fileMIMESource = new FileMIMESource(m_file);
        }
        MIMEParsingInputStream mimeIn = null;
        try {
            mimeIn = m_fileMIMESource.getInputStream();
            m_mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), mimeIn);
            String contentType[] = m_mimeMessage.getHeader(HeaderNames.CONTENT_TYPE);
            String encoding[] = m_mimeMessage.getHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
            MIMEUtil.setContentForPart(m_mimeMessage);
            if (contentType != null) {
                m_mimeMessage.removeHeader(HeaderNames.CONTENT_TYPE);
                for (String s : contentType) {
                    m_mimeMessage.addHeader(HeaderNames.CONTENT_TYPE, s);
                }
            }

            if (encoding != null) {
                m_mimeMessage.removeHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
                for (String s : encoding) {
                    m_mimeMessage.addHeader(HeaderNames.CONTENT_TRANSFER_ENCODING, s);
                }
            }

            if (messageInfo != null)
                messageInfo.setTmpFile(m_file);
            mimeIn.close();
            closeInput();
            return m_mimeMessage;
        } catch (Exception ex) {
            try {
                mimeIn.close();
            } catch (Exception ignore) {
            }
            m_logger.error("Error parsing MIME body", ex);
            return null;
        }
    }

    /**
     * Get a TokenStreamer for the initial contents of this message. This method is "smart" in that it remembers that
     * the Accumulator has now been unparsed.
     * 
     * @return the TokenStreamer
     */
    public TCPStreamer toTCPStreamer()
    {
        return new PartialTCPStreamer();
    }

    /**
     * Shuts down the accumulator for input. Should be called when the last chunk has been written to the file, or when
     * a partial accumulator has been unparsed.
     */
    public void closeInput()
    {
        m_logger.debug("Closing input");
        try {
            m_fileOut.close();
        } catch (Exception ignore) {
        }
        m_fileOut = null;
        m_fileChannel = null;
    }

    /**
     * Closes the accumulator, along with the file and any MIME message
     */
    public void dispose()
    {
        m_logger.debug("Disposing of accumulator file");
        closeInput();
        try {
            m_fileMIMESource.close();
        } catch (Exception ignore) {
        }
        try {
            m_file.delete();
        } catch (Exception ignore) {
        }
        m_file = null;
        m_headers = null;
        m_mimeMessage = null;
        m_fileMIMESource = null;
    }

    /**
     * Appends the bytes to the file
     * 
     * @return false if there was an error
     */
    private boolean appendToFile(ByteBuffer buf)
    {
        try {
            while (buf.hasRemaining()) {
                m_fileChannel.write(buf);
            }
            return true;
        } catch (Exception ex) {
            m_logger.error("Error writing bytes to file", ex);
            return false;
        }
    }

    /**
     * Callback from a chunk to see if it should be unparsed.
     */
    private boolean shouldUnparseImpl(MIMEChunk chunk)
    {
        if (m_unparsed) {
            // The starting bytes have been unparsed.
            // We skip writing this out if this was within the chunks written out.
            return chunk.isWrittenToFile() ? chunk.getIndex() > m_greatestChunkAppendedAndUnparsed : true;
        } else {
            return !chunk.isWrittenToFile();
        }
    }

    private void setUnparsed()
    {
        m_logger.debug("Unparsed at chunk " + m_greatestChunkAppendedAndUnparsed);
        m_unparsed = true;
    }

    /**
     * Method which returns a new index for chunks.
     */
    private synchronized int nextIndex()
    {
        return m_chunkIndex++;
    }

    // ----------------- Inner Class -----------------------

    private class PartialTCPStreamer implements TCPStreamer
    {

        private FileInputStream m_fis;
        private FileChannel m_fileInChannel;
        private final ByteBuffer m_readBuf = ByteBuffer.allocate(CHUNK_SZ);
        private Logger m_logger = Logger.getLogger(MIMEAccumulator.PartialTCPStreamer.class);

        PartialTCPStreamer() {
            m_logger.debug("Created Partial MIME message streamer");

            try {
                m_logger.debug("File is of length: " + m_file.length());
                m_fis = new FileInputStream(m_file);
                if (m_headers == null) {
                    m_logger.debug("Headers null.  Likely a parse error.  Write-out raw bytes");
                }
                // else {
                // m_logger.debug("Advance " + m_headersLen + " bytes to get past headers");
                // long toSkip = m_headersLen;
                // while (toSkip > 0) {
                // toSkip -= m_fis.skip(toSkip);
                // }
                // }
                m_fileInChannel = m_fis.getChannel();
            } catch (Exception ex) {
                m_logger.error("Error opening streamer file", ex);
                close();
            }
        }

        private void close()
        {
            try {
                m_fis.close();
            } catch (Exception ignore) {
            }
            m_fis = null;
            m_fileInChannel = null;
        }

        public boolean closeWhenDone()
        {
            return false;
        }

        public ByteBuffer nextChunk()
        {
            m_logger.debug("Next Chunk called");
            if (m_fileInChannel == null) {
                m_logger.error("Cannot return anything.  Channel never opened");
                setUnparsed();
                return null;
            }
            try {
                m_readBuf.clear();
                int read = m_fileInChannel.read(m_readBuf);
                if (read > 0) {
                    m_readBuf.flip();
                    m_logger.debug("Read a chunk of MIME from file of size: " + read);
                    return m_readBuf;
                } else {
                    m_logger.debug("No more MIME to read");
                    close();
                    setUnparsed();
                    return null;
                }
            } catch (Exception ex) {
                m_logger.error(ex);
                close();
                setUnparsed();
                return null;
            }
        }
    }
}
