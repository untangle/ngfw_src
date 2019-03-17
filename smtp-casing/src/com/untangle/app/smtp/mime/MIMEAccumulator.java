/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

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

import com.untangle.app.smtp.SmtpMessageEvent;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Class used to accumulate MIME bytes. Usage is tricky and subtle, for a few reasons:
 * <ul>
 * <li>
 * "Ownership" of the underlying file is subtle. Before a complete MIME message has been parsed, the accumulator owns
 * the file. Afterwards, users should consider the file "owned" by the MIME. Once ownership of the file has been
 * transfered, using the {@link #dispose dispose} method could hose callers. Instead, use {@link #closeInput closeInput}
 * which will only close open streams.</li>
 * <li>
 * If multiple apps are in "buffer-and-trickle" where they are accumulating MIME yet also passing it along, there is
 * confusion as-to if a given MIMEChunkToken should (a) be written to disk and (b) should be unparsed. To avoid this
 * confusion, all MIMEChunkTokens are members of a given MIMEAccumulator, and have methods to determine if they should be
 * unparsed. Appending a chunk to the file is simplified via the {@link #appendChunkTokenToFile appendChunkTokenToFile} which
 * silently ignores duplicate calls to append the same chunk.</li>
 * </ul>
 * <br>
 * <br>
 * If a MIMEAccumulator ever reaches an unparser, the {@link #toTCPStreamer toTCPStreamer} method can be used to obtain
 * a streamer destined for the endpoint. The MIMEAccumulator then "remembers" this event, such that subsequent
 * MIMEChunkTokens may or may not be written-out depending on when (if) they were appended to the file. If they were appended
 * <i>before</i> the unparse event, then they should not be unparsed if received. In other words, please use the
 * "shouldUnparse" method on MIMEChunkToken before unparsing. <br>
 * <br>
 * No one should ever see a MIMEChunkToken before the MIMEAccumulator to-which it belongs.
 */
public class MIMEAccumulator
{

    private final Logger logger = Logger.getLogger(MIMEAccumulator.class);
    private static final int CHUNK_SZ = 1024 * 4;
    private static final int MAX_FILE_SIZE = 100 * 1024 * 1024; /* 100M */
    private File file;
    private FileOutputStream fileOut;
    private FileChannel fileChannel;
    private InternetHeaders headers;
    private FileMIMESource fileMIMESource;
    private int headersLen;
    private boolean unparsed = false;
    private int greatestChunkTokenAppendedAndUnparsed = -1;
    private int chunkIndex = 0;
    private MimeMessage mimeMessage;

    /**
     * Class used to represent a chunk of MIME accociated with a given MIMEAccumulator. MIMEChunkTokens may not have useful
     * data (if for example they are simply a marker of {@link #isLast the end} of a MIME message). <br>
     * <br>
     * MIMEChunkTokens also may or may not be written to file. To write a given chunk to a file, use the "appendChunkTokenToFile"
     * method of the MIMEAccumulator. <br>
     * <br>
     * Before ever unparsing a MIMEChunkToken, please use the {@link #shouldUnparse shouldUnparse} method, which will catch
     * the case of a MIMEChunkToken passed <b>and</b> written to file.
     */
    public class MIMEChunkToken
    {
        private boolean isLast = false;
        private int index;
        private ByteBuffer buf;
        private boolean writtenToFile = false;

        /**
         * Initialzie instance of MIMEChunkToken.
         * @param  buf    ByteBuffer to initialize with.
         * @param  isLast If true, this is the last chunk.
         * @param  index  Index o this chunk.
         * @return        Instance of MIMEChunkToken.
         */
        private MIMEChunkToken(ByteBuffer buf, boolean isLast, int index)
        {
            this.isLast = isLast;
            this.buf = buf;
            this.index = index;
        }

        /**
         * Does this chunk represent the end of the message. Such chunks may or may not {@link #hasData have data}.
         * @return true if this is the last chunk, false otherwise.
         */
        public boolean isLast()
        {
            return this.isLast;
        }

        /**
         * Does this chunk have any data. Boundary cases in parsing mayt result in blank chunks.
         * @return true if this chunk still has data, false otherwise.
         */
        public boolean hasData()
        {
            return this.buf != null && this.buf.hasRemaining();
        }

        /**
         * Get the underlying data. Note that you should first check {@link #hasData hasData} to see if null may be
         * returned. <br>
         * <br>
         * <b>If unparsing, please first consult {@link #shouldUnparse shouldUnparse}</b>
         * @return ByteBuffer of data.
         */
        public ByteBuffer getData()
        {
            return this.buf == null ? null : this.buf.duplicate();
        }

        /**
         * Length of internal buffer. May be 0 is internal buffer is null
         * @return length of buffer.
         */
        public int length()
        {
            return this.buf == null ? 0 : this.buf.remaining();
        }

        /**
         * Specify that this buffer was written to a file.
         */
        private void writtenToFile()
        {
            this.writtenToFile = true;
        }

        /**
         * Return if buffer was written to file.
         * @return If true, was written to file, otherwise false.
         */
        private boolean isWrittenToFile()
        {
            return this.writtenToFile;
        }

        /**
         * Return index of buffer.
         * @return Index of buffer.
         */
        private int getIndex()
        {
            return this.index;
        }

        /**
         * Test if this chunk should be unparsed. False will be returned if this was passed t0 "appendChunkTokenToFile" yet
         * the MIMEAccumulator has yet to be unparsed.
         * @return if true, chunk should be unparsed, false otherwise.
         */
        public boolean shouldUnparse()
        {
            return shouldUnparseImpl(this);
        }

        /**
         * Obviously only for deep debugging
         * @param logger Logger to use.
         * @param prefix Log message prefix.
         */
        public void superDebugMe(Logger logger, String prefix)
        {
            String body = getData() == null ? "<null>" : com.untangle.uvm.util.AsciiUtil.bbToString(getData());
            logger.debug(prefix + " " + toString() + " \"" + body + "\"");
        }

        /**
         * Compare accumulators.
         * @param  ref MIMEAccumulator to check.
         * @return     If true, same accumulator, false otherwise.
         */
        private boolean isSameAccumulator(MIMEAccumulator ref)
        {
            return ref == MIMEAccumulator.this;
        }

        /**
         * Get string of index identifier.
         * @return String of index.
         */
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
     * @param session AppTCPSession to use.
     * @throws IOException if unable to create temp file.
     */
    public MIMEAccumulator( AppTCPSession session ) throws IOException
    {
        this.logger.debug("Opening temp file to buffer MIME");
        try {
            this.file = File.createTempFile("MIMEAccumulator-", null);
            if (this.file != null)
                session.attachTempFile(this.file.getAbsolutePath());
            this.fileOut = new FileOutputStream(this.file);
            this.fileChannel = this.fileOut.getChannel();
        } catch (IOException ex) {
            this.logger.error("Exception creating a temp file for MIME message", ex);
            try {
                this.fileOut.close();
            } catch (Exception ignore) {
            }
            if(this.file != null){
                try {
                    this.file.delete();
                } catch (Exception ignore) {
                }
            }
            IOException ex2 = new IOException("Exception creating a temp file for MIME message");
            ex2.initCause(ex);
            throw ex2;
        }
    }

    /**
     * Method to associate a chunk with this MIMEAccumulator. Does not implicitly {@link #appendChunkTokenToFile add to the
     * underlying file}.
     * 
     * @param buf
     *            the buffer (may be null)
     * @param isLast
     *            true if this is the last chunk in the MIME message
     * @return MIMEChunkToken instance.
     */
    public MIMEAccumulator.MIMEChunkToken createChunkToken(ByteBuffer buf, boolean isLast)
    {
        int next = nextIndex();
        this.logger.debug("[createChunkToken()] Creating MIMEChunkToken " + next + " with "
                + (buf == null ? "0" : Integer.toString(buf.remaining())) + " bytes");
        return new MIMEChunkToken(buf, isLast, nextIndex());
    }

    /**
     * Add the given chunk to this accumulator. An error will occur if the chunk was not {@link createChunkToken created by
     * this accumulator}. <br>
     * <br>
     * The boolean is used to convey outcome (success/failure). Failures have already been logged. If error, chunk was
     * not written. If the chunk has already been written (for example, two Apps in "buffer-and-passthru" mode), then
     * the second write is silently ignored. <br>
     * <br>
     * If an error does occur, no streams are closed.
     * 
     * @param chunk
     *            the chunk
     * @return true if successful.
     */
    public boolean appendChunkTokenToFile(MIMEAccumulator.MIMEChunkToken chunk)
    {
        if (!chunk.isSameAccumulator(this)) {
            throw new RuntimeException("ChunkToken not for this MIME file");
        }
        if (!chunk.hasData()) {
            this.logger.debug("[appendChunkTokenToFile()] ChunkToken " + chunk + " has no data.  Nothing to append");
            return true;
        }
        if (chunk.isWrittenToFile()) {
            this.logger.debug("[appendChunkTokenToFile()] ChunkToken " + chunk + " already appended to this file");
            return true;
        }
        if (!appendToFile(chunk.getData())) {
            this.logger.debug("[appendChunkTokenToFile()] Error appending chunk " + chunk);
            return false;
        }
        if (!this.unparsed) {
            this.logger.debug("[appendChunkTokenToFile()] Assign chunk " + chunk + " greatest chunk yet unparsed");
            this.greatestChunkTokenAppendedAndUnparsed = chunk.getIndex();
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
                this.fileOut.flush();
            } catch (Exception wtf) {
                this.logger.error("Error adding header bytes", wtf);
            }
            this.headersLen = (int) this.file.length();
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
        if (this.headers != null) {
            return this.headers;
        }
        if (getHeadersLength() == 0) {
            this.logger.debug("Parsing headers, yet no header bytes.  Assume " + "blank headers");

            this.headers = new InternetHeaders();
            return this.headers;
        }
        MIMEParsingInputStream in = null;
        try {
            this.fileMIMESource = new FileMIMESource(this.file);
            in = this.fileMIMESource.getInputStream();
            this.headers = new InternetHeaders(in);
            this.headersLen = (int) in.position();
            return this.headers;
        } catch (Exception ex) {
            this.logger.error("Error parsing MIME body", ex);
            this.fileMIMESource = null;
            return null;
        }finally{
            if(in != null){
                try {
                    in.close();
                } catch (Exception ignore) {
                }
            }
        }
    }

    /**
     * Get the length of the headers (in bytes) including any terminator (CRLF).
     * @return length of headers.
     */
    public int getHeadersLength()
    {
        return this.headersLen;
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
        if (this.file.length() == 0) {
            return ByteBuffer.allocate(0);
        }
        try(
            FileInputStream fIn = new FileInputStream(this.file);
        ){
            ByteBuffer buf = ByteBuffer.allocate((int) this.file.length());
            FileChannel fc = fIn.getChannel();
            while (buf.hasRemaining()) {
                fc.read(buf);
            }
            buf.flip();
            return buf;
        } catch (Exception ex) {
            this.logger.error("Error draining headers trapped in file to buffer");
            return null;
        }
    }

    /**
     * Get the size of the underlying file (the number of bytes accumulated thus-far). This includes the header bytes
     * @return Size of file.
     */
    public int fileSize()
    {
        return (int) this.file.length();
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
     * @param messageInfo SmtpMessageEvent to process.
     * @return a parsed MIMEMEssage, or null if an error occured. If there is an error, it has been logged.
     */
    public MimeMessage parseBody(SmtpMessageEvent messageInfo)
    {
        if (this.mimeMessage != null) {
            return this.mimeMessage;
        }
        if (this.fileMIMESource == null) {
            this.fileMIMESource = new FileMIMESource(this.file);
        }
        MIMEParsingInputStream mimeIn = null;
        try {
            mimeIn = this.fileMIMESource.getInputStream();
            this.mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()), mimeIn);
            String contentType[] = this.mimeMessage.getHeader(HeaderNames.CONTENT_TYPE);
            String encoding[] = this.mimeMessage.getHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
            MIMEUtil.setContentForPart(this.mimeMessage);
            if (contentType != null) {
                this.mimeMessage.removeHeader(HeaderNames.CONTENT_TYPE);
                for (String s : contentType) {
                    this.mimeMessage.addHeader(HeaderNames.CONTENT_TYPE, s);
                }
            }

            if (encoding != null) {
                this.mimeMessage.removeHeader(HeaderNames.CONTENT_TRANSFER_ENCODING);
                for (String s : encoding) {
                    this.mimeMessage.addHeader(HeaderNames.CONTENT_TRANSFER_ENCODING, s);
                }
            }

            if (messageInfo != null)
                messageInfo.setTmpFile(this.file);
            closeInput();
            return this.mimeMessage;
        } catch (Exception ex) {
            this.logger.error("Error parsing MIME body", ex);
            return null;
        }finally{
            if(mimeIn != null){
                try {
                    mimeIn.close();
                } catch (Exception ignore) {
                }
            }
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
        this.logger.debug("Closing input");
        try {
            this.fileOut.close();
        } catch (Exception ignore) {
        }
        this.fileOut = null;
        this.fileChannel = null;
    }

    /**
     * Closes the accumulator, along with the file and any MIME message
     */
    public void dispose()
    {
        this.logger.debug("Disposing of accumulator file");
        closeInput();
        try {
            this.fileMIMESource.close();
        } catch (Exception ignore) {
        }
        try {
            this.file.delete();
        } catch (Exception ignore) {
        }
        this.file = null;
        this.headers = null;
        this.mimeMessage = null;
        this.fileMIMESource = null;
    }

    /**
     * Appends the bytes to the file
     *
     * @param buf ByteBuffer to append.
     * @return false if there was an error
     */
    private boolean appendToFile(ByteBuffer buf)
    {
        if ( this.file.length() > MAX_FILE_SIZE )
            return false;
        try {
            while (buf.hasRemaining()) {
                this.fileChannel.write(buf);
            }
            return true;
        } catch (Exception ex) {
            this.logger.error("Error writing bytes to file", ex);
            return false;
        }
    }

    /**
     * Callback from a chunk to see if it should be unparsed.
     * @param chunk MIMEChunkToken to parse.
     * @return true if chunk hasn't been written to file, false ohtherwise.
     */
    private boolean shouldUnparseImpl(MIMEChunkToken chunk)
    {
        if (this.unparsed) {
            // The starting bytes have been unparsed.
            // We skip writing this out if this was within the chunks written out.
            return chunk.isWrittenToFile() ? chunk.getIndex() > this.greatestChunkTokenAppendedAndUnparsed : true;
        } else {
            return !chunk.isWrittenToFile();
        }
    }

    /**
     * Set unparsed to true.
     */
    private void setUnparsed()
    {
        this.logger.debug("Unparsed at chunk " + this.greatestChunkTokenAppendedAndUnparsed);
        this.unparsed = true;
    }

    /**
     * Method which returns a new index for chunks.
     * @return The next index.
     */
    private synchronized int nextIndex()
    {
        return this.chunkIndex++;
    }

    // ----------------- Inner Class -----------------------

    /**
     * Partial TCP streamer.
     */
    private class PartialTCPStreamer implements TCPStreamer
    {

        private FileInputStream fis;
        private FileChannel fileInChannel;
        private final ByteBuffer readBuf = ByteBuffer.allocate(CHUNK_SZ);
        private Logger logger = Logger.getLogger(MIMEAccumulator.PartialTCPStreamer.class);

        /**
         * Initialize instance of PartialTCPStreamer.
         * @return PartialTCPStreamer.
         */
        PartialTCPStreamer() {
            this.logger.debug("Created Partial MIME message streamer");

            try {
                this.logger.debug("File is of length: " + file.length());
                this.fis = new FileInputStream(file);
                if ( headers == null ) {
                    this.logger.debug("Headers null.  Likely a parse error.  Write-out raw bytes");
                }
                // else {
                // this.logger.debug("Advance " + this.headersLen + " bytes to get past headers");
                // long toSkip = this.headersLen;
                // while (toSkip > 0) {
                // toSkip -= this.fis.skip(toSkip);
                // }
                // }
                this.fileInChannel = this.fis.getChannel();
            } catch (Exception ex) {
                this.logger.error("Error opening streamer file", ex);
                close();
            }
        }

        /**
         * Close the accumulator.
         */
        private void close()
        {
            try {
                this.fis.close();
            } catch (Exception ignore) {
            }
            this.fis = null;
            this.fileInChannel = null;
        }

        /**
         * Close when done.
         * @return Always return false.
         */
        public boolean closeWhenDone()
        {
            return false;
        }

        /**
         * Return the next chunk.
         * @return ByteBuffer of next chunk.
         */
        public ByteBuffer nextChunk()
        {
            this.logger.debug("Next ChunkToken called");
            if (this.fileInChannel == null) {
                this.logger.error("Cannot return anything.  Channel never opened");
                setUnparsed();
                return null;
            }
            try {
                this.readBuf.clear();
                int read = this.fileInChannel.read(this.readBuf);
                if (read > 0) {
                    this.readBuf.flip();
                    this.logger.debug("Read a chunk of MIME from file of size: " + read);
                    return this.readBuf;
                } else {
                    this.logger.debug("No more MIME to read");
                    close();
                    setUnparsed();
                    return null;
                }
            } catch (Exception ex) {
                this.logger.error(ex);
                close();
                setUnparsed();
                return null;
            }
        }
    }
}
