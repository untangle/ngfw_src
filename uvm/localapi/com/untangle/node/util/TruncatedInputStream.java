/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.util;
import java.io.*;


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
