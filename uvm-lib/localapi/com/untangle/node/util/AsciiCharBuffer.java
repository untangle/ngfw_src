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

package com.untangle.node.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

/**
 * Wraps a ByteBuffer to implement <code>CharSequence</code>,
 * <code>Appendable</code>, and <code>Comparable</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public class AsciiCharBuffer
    implements CharSequence, Appendable, Comparable<AsciiCharBuffer>
{
    private ByteBuffer bb;
    private boolean readOnly;

    private AsciiCharBuffer(ByteBuffer bb, boolean readOnly)
    {
        this.bb = bb;
    }

    public static AsciiCharBuffer wrap(ByteBuffer bb)
    {
        return new AsciiCharBuffer(bb.duplicate(), false);
    }

    public static AsciiCharBuffer wrap(byte[] ba)
    {
        ByteBuffer newBuf = ByteBuffer.wrap(ba);
        return new AsciiCharBuffer(newBuf, false);
    }

    public static AsciiCharBuffer allocate(int capacity, boolean readOnly)
    {
        ByteBuffer newBuf = ByteBuffer.allocate(capacity);
        return new AsciiCharBuffer(newBuf, readOnly);
    }

    public ByteBuffer getWrappedBuffer()
    {
        return bb;
    }

    public AsciiCharBuffer asReadOnlyBuffer()
    {
        return new AsciiCharBuffer(bb.duplicate(), true);
    }

    public int capacity()
    {
        return bb.capacity();
    }

    public AsciiCharBuffer clear()
    {
        bb.clear();
        return this;
    }

    public AsciiCharBuffer compact()
    {
        bb.compact();
        return this;
    }

    public AsciiCharBuffer duplicate()
    {
        return new AsciiCharBuffer(bb.duplicate(), false);
    }

    public AsciiCharBuffer flip()
    {
        bb.flip();
        return this;
    }

    public char get()
    {
        return (char)(bb.get() & 0x00FF);
    }

    public AsciiCharBuffer get(char[] dst)
    {
        return get(dst, 0, dst.length);
    }

    public AsciiCharBuffer get(char[] dst, int off, int l)
    {
        for (int i = off; i < off + l; i++) {
            dst[i] = (char)(bb.get() & 0x00FF);
        }

        return this;
    }

    public char get(int i)
    {
        return (char)(bb.get(i) & 0x00FF);
    }

    public boolean hasArray()
    {
        return false;
    }

    public boolean hasRemaining()
    {
        return bb.hasRemaining();
    }

    public boolean isDirect()
    {
        return false;
    }

    public boolean isReadOnly()
    {
        return bb.isReadOnly();
    }

    public int limit()
    {
        return bb.limit();
    }

    public AsciiCharBuffer limit(int limit)
    {
        bb.limit(limit);
        return this;
    }

    public AsciiCharBuffer mark()
    {
        bb.mark();
        return this;
    }

    public ByteOrder order()
    {
        return bb.order();
    }

    public int position()
    {
        return bb.position();
    }

    public AsciiCharBuffer position(int np)
    {
        bb.position(np);
        return this;
    }

    public AsciiCharBuffer put(char c)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        bb.put((byte)c);
        return this;
    }

    public AsciiCharBuffer put(int i, char c)
    {
        bb.put(i, (byte)c);
        return this;
    }

    public AsciiCharBuffer put(char[] src, int off, int l)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        for (int i = off; i < off + l; i++) {
            bb.put((byte)src[i]);
        }
        return this;
    }

    public AsciiCharBuffer put(char[] src)
    {
        return put(src, 0, src.length);
    }

    public AsciiCharBuffer put(CharSequence src, int s, int e)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        for (int i = s; i < s + e; i++) {
            bb.put((byte)src.charAt(i));
        }
        return this;
    }

    public AsciiCharBuffer put(CharSequence src)
    {
        return put(src, 0, src.length());
    }

    public AsciiCharBuffer slice()
    {
        return new AsciiCharBuffer(bb.slice(), false);
    }

    public int remaining()
    {
        return bb.remaining();
    }

    public AsciiCharBuffer reset()
    {
        bb.reset();
        return this;
    }

    public AsciiCharBuffer rewind()
    {
        bb.rewind();
        return this;
    }

    // CharSequence methods ---------------------------------------------------

    public char charAt(int i)
    {
        return (char)(bb.get(bb.position() + i) & 0x00FF);
    }

    public int length()
    {
        return bb.remaining();
    }

    public CharSequence subSequence(int s, int e)
    {
        ByteBuffer dup = bb.duplicate();
        dup.position(bb.position() + s);
        dup.limit(bb.position() + e);
        return new AsciiCharBuffer(dup, false);
    }

    public String toString()
    {
        ByteBuffer dup = bb.duplicate();
        byte[] sb = new byte[dup.remaining()];
        dup.get(sb);
        return new String(sb);
    }

    // Appendable methods -----------------------------------------------------

    public AsciiCharBuffer append(char c)
    {
        return put(c);
    }

    public AsciiCharBuffer append(CharSequence csq)
    {
        return put(csq);
    }

    public AsciiCharBuffer append(CharSequence csq, int start, int end)
    {
        return put(csq, start, end);
    }

    // Comparable methods -----------------------------------------------------

    public int compareTo(AsciiCharBuffer cb)
    {
        return bb.compareTo(cb.bb);
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof AsciiCharBuffer)) {
            return false;
        }
        AsciiCharBuffer cb = (AsciiCharBuffer)o;

        return bb.equals(cb.bb);
    }

    public int hashCode()
    {
        return bb.hashCode();
    }
}
