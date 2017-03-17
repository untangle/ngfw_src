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

    // -------------------------
    // See doc on MIMESource
    // -------------------------
    public MIMEParsingInputStream getInputStream() throws IOException
    {
        return getInputStream(0);
    }

    // -------------------------
    // See doc on MIMESource
    // -------------------------
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

    // -------------------------
    // See doc on MIMESource
    // -------------------------
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
     */
    public File getFile()
    {
        return file;
    }

    // -------------------------
    // See doc on MIMESource
    // -------------------------
    public File toFile() throws IOException
    {
        return getFile();
    }

    // -------------------------
    // See doc on MIMESource
    // -------------------------
    public File toFile(String name) throws IOException
    {
        return getFile();
    }

    private MIMEParsingInputStream createBaseStream() throws IOException
    {
        MIMEParsingInputStream ret = new MIMEParsingInputStream(new BufferedInputStream(new FileInputStream(file)));
        openStreams.add(ret);
        return ret;
    }

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

        public ReusableMIMEParsingInputStream(MIMEParsingInputStream wrap) {
            super(new NOOPInputStream());
            this.wrap = wrap;
        }

        @Override
        public long position()
        {
            checkClosedRE();
            return wrap.position();
        }

        @Override
        public void unread(int b) throws IOException
        {
            checkClosed();
            wrap.unread(b);
        }

        @Override
        public void unread(byte[] b) throws IOException
        {
            checkClosed();
            wrap.unread(b);
        }

        @Override
        public void unread(byte[] b, int off, int len) throws IOException
        {
            checkClosed();
            wrap.unread(b, off, len);
        }

        @Override
        public int read() throws IOException
        {
            checkClosed();
            return wrap.read();
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            checkClosed();
            return wrap.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            checkClosed();
            return wrap.read(b, off, len);
        }

        @Override
        public Line readLine(int maxLen) throws IOException, LineTooLongException
        {
            checkClosed();
            return wrap.readLine(maxLen);
        }

        @Override
        public Line readLine() throws IOException, LineTooLongException
        {
            checkClosed();
            return wrap.readLine();
        }

        @Override
        public void unreadLine(Line line) throws IOException
        {
            checkClosed();
            wrap.unreadLine(line);
        }

        @Override
        public BoundaryResult skipToBoundary(String boundaryStr, final boolean leaveBoundary) throws IOException
        {
            checkClosed();
            return wrap.skipToBoundary(boundaryStr, leaveBoundary);
        }

        @Override
        public void advanceToNextLine() throws IOException
        {
            checkClosed();
            wrap.advanceToNextLine();
        }

        @Override
        public void advanceToEOF() throws IOException
        {
            checkClosed();
            wrap.advanceToEOF();
        }

        @Override
        public long skip(long n) throws IOException
        {
            checkClosed();
            return wrap.skip(n);
        }

        @Override
        public int available() throws IOException
        {
            checkClosed();
            return wrap.available();
        }

        @Override
        public void close() throws IOException
        {
            if (closed) {
                return;
            }
            closed = true;
            returnStream(wrap);
        }

        @Override
        public void mark(int readlimit)
        {
            checkClosedRE();
            wrap.mark(readlimit);
        }

        @Override
        public void reset() throws IOException
        {
            checkClosed();
            wrap.reset();
        }

        @Override
        public boolean markSupported()
        {
            checkClosedRE();
            return wrap.markSupported();
        }

        private void checkClosed() throws IOException
        {
            if (closed) {
                throw new IOException("Stream already closed");
            }
        }

        private void checkClosedRE() throws RuntimeException
        {
            if (closed) {
                throw new RuntimeException("Stream already closed");
            }
        }
    }

}
