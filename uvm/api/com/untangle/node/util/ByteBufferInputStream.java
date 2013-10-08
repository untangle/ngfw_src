/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/ByteBufferInputStream.java $
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
package com.untangle.node.util;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;


/**
 * Input stream which wraps a ByteBuffer
 */
public class ByteBufferInputStream
    extends InputStream {

    private final ByteBuffer m_buf;
    private int m_markReadLimit;
    private int m_markPosition;

    public ByteBufferInputStream(ByteBuffer buf) {
        m_buf = buf;
    }


    @Override
        public int read()
        throws IOException {

        return m_buf.hasRemaining()?m_buf.get():-1;
    }


    @Override
        public int read(byte[] b)
        throws IOException {

        return read(b, 0, b.length);
    }


    @Override
        public int read(byte[] b, int off, int len)
        throws IOException {

        if(!m_buf.hasRemaining()) {
            return -1;
        }
        len = Math.min(len, m_buf.remaining());
        m_buf.get(b, off, len);
        return len;
    }


    @Override
        public long skip(long n)
        throws IOException {
        if(n > Integer.MAX_VALUE) {
            n = Integer.MAX_VALUE;
        }
        int toSkip = Math.min(m_buf.remaining(), (int) n);
        m_buf.position(m_buf.position() + toSkip);
        return toSkip;
    }



    @Override
        public void mark(int readLimit) {
        m_markPosition = m_buf.position();
        m_markReadLimit = m_buf.position() + readLimit;
    }


    @Override
        public void reset()
        throws IOException {
        if(m_buf.position() > m_markReadLimit) {
            throw new IOException("Exceeded mark limit");
        }
        m_buf.position(m_markPosition);
    }


    @Override
    public boolean markSupported() {
        return true;
    }

    @Override
    public void close() {
        m_buf.position(m_buf.limit());
    }

    @Override
    public int available() {
        return m_buf.remaining();
    }

}
