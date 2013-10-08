/**
 * $Id: AsciiCharBuffer.java 35447 2013-07-29 17:24:43Z dmorris $
 */
package com.untangle.node.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

/**
 * Wraps a ByteBuffer to implement <code>CharSequence</code>,
 * <code>Appendable</code>, and <code>Comparable</code>.
 *
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
