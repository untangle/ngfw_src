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

package com.untangle.node.smtp.mime;

import static com.untangle.node.util.Ascii.CRLF_BA;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * OutputStream which adds some convienences
 * for doing MIME stuff.
 * <br>
 * This class is single-threaded.
 */
public class MIMEOutputStream
    extends FilterOutputStream {

    private byte[] m_transferBuf;

    public MIMEOutputStream(OutputStream target) {
        super(target);
    }

    public long write(Line[] lines)
        throws IOException {
        long ret = 0;
        for(Line line : lines) {
            ret+=write(line);
        }
        return ret;
    }

    public int write(Line line)
        throws IOException {
        return write(line.getBuffer(true));
    }

    /**
     * The buffer is not reset to its original
     * position after this method concludes.
     */
    public int write(ByteBuffer buf)
        throws IOException {

        //Technically sloppy, but assume we will write this
        //much
        int ret = buf.remaining();

        if(buf.hasArray()) {
            write(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
        }
        else {
            ensureTransferBuf();
            while(buf.hasRemaining()) {
                int transfer = buf.remaining() > m_transferBuf.length?
                    m_transferBuf.length:
                    buf.remaining();
                buf.get(m_transferBuf, 0, transfer);
                write(m_transferBuf, 0, transfer);
            }
        }
        return ret;
    }

    /**
     * Pipes the contents of the input stream
     * to this output stream, until EOF is
     * reached.
     */
    public long pipe(InputStream in)
        throws IOException {

        return pipe(in, Long.MAX_VALUE);
    }

    public long pipe(final InputStream in,
                     final long maxTransfer)
        throws IOException {

        long total = 0;
        int reqAmt = 0;
        int read = 0;

        ensureTransferBuf();

        while(total < maxTransfer) {

            //Figure out how much to ask for in our read.
            reqAmt = (maxTransfer - total) > m_transferBuf.length?
                m_transferBuf.length:
                (int) (maxTransfer - total);

            //Perform the read
            read = in.read(m_transferBuf, 0, reqAmt);
            if(read == -1) {
                break;
            }
            //Perform the write
            write(m_transferBuf, 0, read);
            total+=read;
        }
        return total;
    }

    /**
     * Note that this String should be in the US-ASCII charset!
     */
    public void write(String aString)
        throws IOException {
        write(aString.getBytes());
    }

    /**
     * Writes the given line, terminating with a CRLF
     */
    public void writeLine(String line)
        throws IOException {
        write(line);
        writeLine();
    }

    /**
     * Writes a proper line terminator (CRLF).
     */
    public void writeLine()
        throws IOException {
        write(CRLF_BA);
    }

    public void write(MIMESourceRecord record)
        throws IOException {
        //TODO bscott There has to be a better way to handle this
        MIMEParsingInputStream inStream = record.source.getInputStream(record.start);
        pipe(inStream, record.len);
        inStream.close();
    }


    private void ensureTransferBuf() {
        if(m_transferBuf == null) {
            m_transferBuf = new byte[8192];
        }
    }

}
