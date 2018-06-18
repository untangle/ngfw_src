/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * File MIME source.
 */
public class FileMIMESource
{

    private File file;
    private final boolean deleteFileOnClose;
    private MIMEParsingInputStream cachedStream;
    private Set<MIMEParsingInputStream> openStreams;// Cached is always in
                                                    // OpenStreams

    /**
     * Construct a new FileMIMESource against the given file which <b>will</b> delete the file when {@link #close
     * closed}.
     * 
     * @param file
     *            the file
     */
    public FileMIMESource(File file) {
        this(file, true);
    }

    /**
     * Construct a new FileMIMESource against the given file.
     * 
     * @param file
     *            the file
     * @param deleteFileOnClose
     *            if true, the file will be implicitly deleted when this {@link #close source is closed}
     */
    public FileMIMESource(File file, boolean deleteFileOnClose) {
        this.file = file;
        this.deleteFileOnClose = deleteFileOnClose;
        this.openStreams = new HashSet<MIMEParsingInputStream>();
    }

    /**
     * Return MIME parsing input stream.
     * @return MIMEParsingInputStream of import stream.
     * @throws IOException   On problem.
     */
    public MIMEParsingInputStream getInputStream() throws IOException
    {
        return getInputStream(0);
    }

    /**
     * Return MIME parsing input stream at offset.
     * @param offset long of offset.
     * @return MIMEParsingInputStream of import stream.
     * @throws IOException   On problem.
     */
    public MIMEParsingInputStream getInputStream(long offset) throws IOException
    {

        MIMEParsingInputStream ret = null;

        if (cachedStream != null) {
            if (cachedStream.position() <= offset) {
                ret = cachedStream;
            } else {
                destroyStream(cachedStream);
                ret = createBaseStream();
            }
            cachedStream = null;// Make sure we do not give this out twice
        } else {
            ret = createBaseStream();
        }

        // Now, advance the stream
        try {
            long diff = offset - ret.position();
            while (diff > 0) {
                diff -= ret.skip(diff);
            }
        } catch (IOException ex) {
            destroyStream(ret);
            IOException ex2 = new IOException("Unable to advance stream");
            ex2.initCause(ex);
            throw ex2;
        }

        return new ReusableMIMEParsingInputStream(ret);
    }

    /**
     * Close the input stream.
     */
    public void close()
    {
        // Bug 779 - Copy contents of open streams before
        // attempting to iterate, as iteration
        // removes from the list
        MIMEParsingInputStream[] oStreams = openStreams.toArray(new MIMEParsingInputStream[openStreams.size()]);

        for (MIMEParsingInputStream s : oStreams) {
            destroyStream(s);
        }

        destroyStream(cachedStream);// Should be redundant
        cachedStream = null;
        if (deleteFileOnClose) {
            try {
                file.delete();
            } catch (Exception ignore) {
            }
            file = null;
        }
    }

    /**
     * Get the underlying file. Note that this file <b>may be deleted</b> when this source is closed, so if you want the
     * file to exist for longer you should copy the file.
     * @return Underlying file of File
     */
    public File getFile()
    {
        return file;
    }

    /**
     * Get the underlying file. Note that this file <b>may be deleted</b> when this source is closed, so if you want the
     * @return Underlying file of File
     * @throws IOException   On problem.
     */
    public File toFile() throws IOException
    {
        return getFile();
    }

    /**
     * Get the underlying file. Note that this file <b>may be deleted</b> when this source is closed, so if you want the
     * @param name String of file name.  Unused.
     * @return Underlying file of File
     * @throws IOException   On problem.
     */
    public File toFile(String name) throws IOException
    {
        return getFile();
    }

    /**
     * Create base stream for file.
     * @return MIMEParsingInputStream
     * @throws IOException   On problem.
     */
    private MIMEParsingInputStream createBaseStream() throws IOException
    {
        MIMEParsingInputStream ret = new MIMEParsingInputStream(new BufferedInputStream(new FileInputStream(file)));
        openStreams.add(ret);
        return ret;
    }

    /**
     * Close and remove stream.
     * @param stream MIMEParsingInputStream to close.
     */
    private void destroyStream(MIMEParsingInputStream stream)
    {
        if (stream == null) {
            return;
        }
        try {
            stream.close();
        } catch (Exception ignore) {
        }
        try {
            openStreams.remove(stream);
        } catch (Exception ignore) {
        }
    }

    /**
     * Get MIMEParsingInputStream stream.
     * @param stream MIMEParsingInputStream.
     */
    private void returnStream(MIMEParsingInputStream stream)
    {
        if (cachedStream != null) {
            // Test if we should replace the cached stream. We do
            // this if the new stream is at a lower position (more
            // likely to be useful).
            if (cachedStream.position() < stream.position()) {
                destroyStream(stream);
            } else {
                destroyStream(cachedStream);
                cachedStream = stream;
            }
        } else {
            cachedStream = stream;
        }
    }

    /**
     * Used since we're subclasses (rather than implementing an interface) and needed a dummy stream.
     */
    private class NOOPInputStream extends InputStream
    {
        /**
         * Dummy function.
         * @return Always return integer value of -1.
         */
        public int read()
        {
            return -1;
        }
    }

    /**
     * Wrapper which lets us keep track of underlying open streams.
     */
    private class ReusableMIMEParsingInputStream extends MIMEParsingInputStream
    {
        private final MIMEParsingInputStream wrap;
        private boolean closed = false;

        /**
         * Initialize instance of ReusableMIMEParsingInputStream.
         * @param  wrap MIMEParsingInputStream to process.
         * @return      Instance of ReusableMIMEParsingInputStream
         */
        public ReusableMIMEParsingInputStream(MIMEParsingInputStream wrap) {
            super(new NOOPInputStream());
            this.wrap = wrap;
        }

        /**
         * Return wrap position.
         * @return long of position.
         */
        @Override
        public long position()
        {
            checkClosedRE();
            return wrap.position();
        }

        /**
         * Unread wrap.
         * @param  b           Size to unread.
         * @throws IOException On problem.
         */
        @Override
        public void unread(int b) throws IOException
        {
            checkClosed();
            wrap.unread(b);
        }

        /**
         * Unread wrap.
         * @param  b           Array of bytes to unread.
         * @throws IOException On problem.
         */
        @Override
        public void unread(byte[] b) throws IOException
        {
            checkClosed();
            wrap.unread(b);
        }

        /**
         * Unread wrap.
         * @param  b           Array of bytes to unread.
         * @param off          Offset of integer.
         * @param len          Length of integer.
         * @throws IOException On problem.
         */
        @Override
        public void unread(byte[] b, int off, int len) throws IOException
        {
            checkClosed();
            wrap.unread(b, off, len);
        }

        /**
         * Read from wrap.
         * @return Length read.
         * @throws IOException On problem.
         */
        @Override
        public int read() throws IOException
        {
            checkClosed();
            return wrap.read();
        }

        /**
         * Read from wrap.
         * @param b Array of byte to read into.
         * @return Value from wrap.
         * @throws IOException On problem.
         */
        @Override
        public int read(byte[] b) throws IOException
        {
            checkClosed();
            return wrap.read(b);
        }

        /**
         * Read from wrap.
         * @param b Array of byte to read into.
         * @param off Offset of integer to read.
         * @param len Length of integer to read.
         * @return Value from wrap.
         * @throws IOException On problem.
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            checkClosed();
            return wrap.read(b, off, len);
        }

        /**
         * Read line of max length.
         * @param  maxLen               Maximum length of line to read.
         * @return                      Line read.
         * @throws IOException          If there is a problem reading.
         * @throws LineTooLongException If line exceeds maxclen.
         */
        @Override
        public Line readLine(int maxLen) throws IOException, LineTooLongException
        {
            checkClosed();
            return wrap.readLine(maxLen);
        }

        /**
         * Read line of max length.
         * @return                      Line read.
         * @throws IOException          If there is a problem reading.
         * @throws LineTooLongException If line exceeds default length.
         */
        @Override
        public Line readLine() throws IOException, LineTooLongException
        {
            checkClosed();
            return wrap.readLine();
        }

        /**
         * Unread line.
         * @param  line        Line to stuff back into wrap.
         * @throws IOException If there is a prolem unreading.
         */
        @Override
        public void unreadLine(Line line) throws IOException
        {
            checkClosed();
            wrap.unreadLine(line);
        }

        /**
         * Move wrap to boundry.
         * @param  boundaryStr   Bounty to search.
         * @param  leaveBoundary If true, leave the boundry, false otherwise.
         * @return               BoundaryResult.
         * @throws IOException   If unable to skip.
         */
        @Override
        public BoundaryResult skipToBoundary(String boundaryStr, final boolean leaveBoundary) throws IOException
        {
            checkClosed();
            return wrap.skipToBoundary(boundaryStr, leaveBoundary);
        }

        /**
         * Move to next line.
         * @throws IOException If read error.
         */
        @Override
        public void advanceToNextLine() throws IOException
        {
            checkClosed();
            wrap.advanceToNextLine();
        }

        /**
         * Move to EOF.
         * @throws IOException If read error.
         */
        @Override
        public void advanceToEOF() throws IOException
        {
            checkClosed();
            wrap.advanceToEOF();
        }

        /**
         * Move from current position to new position.
         * @param  n           Amount ot move.
         * @return             Current position.
         * @throws IOException If unable to skip.
         */
        @Override
        public long skip(long n) throws IOException
        {
            checkClosed();
            return wrap.skip(n);
        }

        /**
         * Determine how much is available in wrao.
         * @return Remmaining length in wrap.
         * @throws IOException   If unable to check.
         */
        @Override
        public int available() throws IOException
        {
            checkClosed();
            return wrap.available();
        }

        /**
         * Close the wrap.
         * @throws IOException If unable to close.
         */
        @Override
        public void close() throws IOException
        {
            if (closed) {
                return;
            }
            closed = true;
            returnStream(wrap);
        }

        /**
         * Mark at readLimit.
         * @param readlimit integer value to mark.
         */
        @Override
        public void mark(int readlimit)
        {
            checkClosedRE();
            wrap.mark(readlimit);
        }

        /**
         * Reset stream.
         * @throws IOException If unable to reset.
         */
        @Override
        public void reset() throws IOException
        {
            checkClosed();
            wrap.reset();
        }

        /**
         * Determine if mark is supported.
         * @return true if mark is supported, false otherwise.
         */
        @Override
        public boolean markSupported()
        {
            checkClosedRE();
            return wrap.markSupported();
        }

        /**
         * Determine if closed.
         * @throws IOException If already closed.
         */
        private void checkClosed() throws IOException
        {
            if (closed) {
                throw new IOException("Stream already closed");
            }
        }

        /**
         * Determine if closed with a runtime exception.
         * @throws RuntimeException If already closed.
         */
        private void checkClosedRE() throws RuntimeException
        {
            if (closed) {
                throw new RuntimeException("Stream already closed");
            }
        }
    }

}
