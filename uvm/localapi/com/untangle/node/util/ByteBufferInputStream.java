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
import java.nio.*;


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
