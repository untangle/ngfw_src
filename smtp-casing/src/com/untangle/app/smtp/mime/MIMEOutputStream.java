/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

import static com.untangle.uvm.util.Ascii.CRLF_BA;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * OutputStream which adds some conveniences for doing MIME stuff. <br>
 * This class is single-threaded.
 */
public class MIMEOutputStream extends FilterOutputStream
{

    private byte[] m_transferBuf;

    /**
     * Initiaize instance of MIMEOutputStream.
     * @param  target OutputStream to use.
     * @return        Instance of MIMEOutputStream.
     */
    public MIMEOutputStream(OutputStream target) {
        super(target);
    }

    /**
     * Write multiple lines to output stream.
     * @param  lines       Array of Lines.
     * @return             Long of amount written.
     * @throws IOException On write error.
     */
    public long write(Line[] lines) throws IOException
    {
        long ret = 0;
        for (Line line : lines) {
            ret += write(line);
        }
        return ret;
    }

    /**
     * Write single line to output stream.
     * @param  line        Line to write.
     * @return             Long of amount written.
     * @throws IOException On write error.
     */
    public int write(Line line) throws IOException
    {
        return write(line.getBuffer(true));
    }

    /**
     * The buffer is not reset to its original position after this method concludes.
     * @param  buf         ByteBuffer to write.
     * @return             Long of amount written.
     * @throws IOException On write error.
     */
    public int write(ByteBuffer buf) throws IOException
    {
        int ret = buf.remaining();

        if (buf.hasArray()) {
            write(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
        } else {
            ensureTransferBuf();
            while (buf.hasRemaining()) {
                int transfer = buf.remaining() > m_transferBuf.length ? m_transferBuf.length : buf.remaining();
                buf.get(m_transferBuf, 0, transfer);
                write(m_transferBuf, 0, transfer);
            }
        }
        return ret;
    }

    /**
     * Pipes the contents of the input stream to this output stream, until EOF is reached.
     * @param in InputStream to connect via pipe.
     * @return long of pipe value.
     * @throws IOException if cannot pipe/
     */
    public long pipe(InputStream in) throws IOException
    {
        return pipe(in, Long.MAX_VALUE);
    }

    /**
     * Pipes the contents of the input stream to this output stream, until EOF is reached.
     * @param in InputStream to connect via pipe.
     * @param maxTransfer long of max transfer length.
     * @return long of pipe value.
     * @throws IOException if cannot pipe/
     */
    public long pipe(final InputStream in, final long maxTransfer) throws IOException
    {
        long total = 0;
        int reqAmt = 0;
        int read = 0;

        ensureTransferBuf();

        while (total < maxTransfer) {

            // Figure out how much to ask for in our read.
            reqAmt = (maxTransfer - total) > m_transferBuf.length ? m_transferBuf.length : (int) (maxTransfer - total);

            // Perform the read
            read = in.read(m_transferBuf, 0, reqAmt);
            if (read == -1) {
                break;
            }
            // Perform the write
            write(m_transferBuf, 0, read);
            total += read;
        }
        return total;
    }

    /**
     * Note that this String should be in the US-ASCII charset!
     * @param aString String to write.
     * @throws IOException on write error.
     */
    public void write(String aString) throws IOException
    {
        write(aString.getBytes());
    }

    /**
     * Writes the given line, terminating with a CRLF
     * @param line String to write.
     * @throws IOException on write error.
     */
    public void writeLine(String line) throws IOException
    {
        write(line);
        writeLine();
    }

    /**
     * Writes a proper line terminator (CRLF).
     * @throws IOException on write error.
     */
    public void writeLine() throws IOException
    {
        write(CRLF_BA);
    }

    /**
     * Create transfer buffer if it doesn't exist.
     */
    private void ensureTransferBuf()
    {
        if (m_transferBuf == null) {
            m_transferBuf = new byte[8192];
        }
    }

}
