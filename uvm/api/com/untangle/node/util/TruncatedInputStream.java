/*
 * $HeadURL: svn://chef/work/src/uvm/api/com/untangle/node/util/TruncatedInputStream.java $
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
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;


/**
 * Input stream which wraps another, but returns -1 (EOF)
 * at a point before the wrapped stream.
 */
public class TruncatedInputStream
    extends FilterInputStream {

    private final long m_maxRead;

    private long m_read = 0;

    public TruncatedInputStream(InputStream wrap,
                                long maxRead) {
        super(wrap);
        m_maxRead = maxRead;
    }



    @Override
        public int read()
        throws IOException {

        if(m_read >= m_maxRead) {
            return -1;
        }
        m_read++;
        return in.read();
    }


    @Override
        public int read(byte[] b)
        throws IOException {

        return read(b, 0, b.length);
    }


    @Override
        public int read(byte[] b, int off, int len)
        throws IOException {

        int max = (int) max(len);
        if(max == 0) {
            return -1;
        }
        int ret = in.read(b, off, max);
        m_read+=ret;
        return ret;
    }


    @Override
        public long skip(long n)
        throws IOException {

        long toSkip = max(n);

        long ret = toSkip ==0?
            0:
            in.skip(toSkip);
        m_read+=ret;
        return ret;
    }



    /**
     * Mark is not supported, so this method does nothing
     *
     * @exception IOException from the backing stream
     */
    @Override
        public void mark(int readlimit) {
        //Do nothing
    }



    /**
     * Since marks are not supported, this always throws
     * an exception
     *
     * @exception IOException (always)
     */
    @Override
        public void reset()
        throws IOException {
        throw new IOException("mark not supported");
    }


    /**
     * Always returns false
     *
     * @exception IOException from the backing stream
     */
    @Override
        public boolean markSupported() {
        return false;
    }

    private long max(long requested) {
        long remaining = m_maxRead - m_read;
        return remaining <= 0?
            0:
            (requested > remaining)?
            remaining:
        requested;
    }

}
