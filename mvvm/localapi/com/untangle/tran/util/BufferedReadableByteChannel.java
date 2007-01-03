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

package com.untangle.tran.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.InvalidMarkException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;

public class BufferedReadableByteChannel extends InputStream
    implements ReadableByteChannel
{
    private static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final int MARK_LIMIT = 25;

    private ReadableByteChannel in;
    private ByteBuffer buf;
    private int mark = -1;

    public BufferedReadableByteChannel(ReadableByteChannel in)
    {
        this.in = in;
        buf = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
        buf.limit(0);
    }

    public int read() throws IOException
    {
        if (!buf.hasRemaining()) {
            int i = readMore();
            if (-1 == i) {
                return -1;
            }
        }
        return buf.get();
    }

    // reads b.length bytes
    public int read(byte[] b) throws IOException
    {
        for (int i = 0; i < b.length; i++) {
            int c = read();
            if (-1 == c) {
                return 0 == i ? -1 : c;
            }
            b[i] = (byte)c;
        }
        return b.length;
    }

    // reads at least one ?
    public int read(ByteBuffer b) throws IOException
    {
        if (!buf.hasRemaining()) {
            if (readMore() < 0) {
                return -1;
            }
        }

        ByteBuffer dup = buf.duplicate();
        int len = dup.remaining() > b.remaining()
            ? b.remaining() : dup.remaining();
        dup.limit(dup.position() + len);
        b.put(dup);
        buf.position(buf.position() + len);

        return len;
    }

    public void mark()
    {
        if ((buf.capacity() - buf.limit()) < MARK_LIMIT) {
            buf.compact().flip();
        }
        mark = buf.position();
    }

    public void reset()
    {
        if (mark < 0) {
            throw new InvalidMarkException();
        }
        buf.position(mark);
    }

    private int readMore() throws IOException
    {
        assert buf.limit() == buf.position();

        int s;

        if ((buf.capacity() - buf.limit()) < MARK_LIMIT) {
            mark = -1;
            s = 0;
            buf.clear();
        } else {
            s = buf.limit();
            buf.position(s).limit(buf.capacity());
        }

        try {
            if (0 > in.read(buf)) {
                buf.position(s).limit(s);
                return -1;
            }
        } catch (AsynchronousCloseException exn) {
            // XXX figure out what is going on here
            System.out.println(exn);
            return -1;
        } catch (ClosedChannelException exn) {
            // XXX figure out what is going on here
            System.out.println(exn);
            return -1;
        }

        buf.limit(buf.position()).position(s);

        return buf.remaining();
    }

    // ReadableByteChannel implementation -------------------------------------

    public boolean isOpen()
    {
        return in.isOpen();
    }

    // InputStream implementation ---------------------------------------------

    public int available()
    {
        return 0;
    }

    public void close() throws IOException
    {
        in.close();
    }

    public void mark(int readlimit)
    {
        if (MARK_LIMIT < readlimit) {
            System.out.println("fixed readlimit of " + MARK_LIMIT);
        }
        mark();
    }

    public boolean markSupported()
    {
        return true;
    }

    public long skip(long n) throws IOException
    {
        for (int i = 0; i < n; i++) {
            if (0 > read()) {
                return i;
            }
        }
        return n;
    }
}
