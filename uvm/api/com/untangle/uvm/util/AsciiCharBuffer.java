/**
 * $Id$
 */
package com.untangle.uvm.util;

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

    /**
     * Initialize instance of AsciiCharBuffer.
     * @param  bb       ByteBuffer to set.
     * @param  readOnly Unused boolean.
     * @return          Instance of AsciiCharBuffer.
     */
    private AsciiCharBuffer(ByteBuffer bb, boolean readOnly)
    {
        this.bb = bb;
    }

    /**
     * Return instance of AsciiCharBuffer from copied buffer.
     * @param  bb ByteBuffer to copy.
     * @return    Instance of AsciiCharBuffer.
     */
    public static AsciiCharBuffer wrap(ByteBuffer bb)
    {
        return new AsciiCharBuffer(bb.duplicate(), false);
    }

    /**
     * Return instance of AsciiCharBuffer from array of bytes.
     * @param  ba Array of bytes to copy.
     * @return    Instance of AsciiCharBuffer.
     */
    public static AsciiCharBuffer wrap(byte[] ba)
    {
        ByteBuffer newBuf = ByteBuffer.wrap(ba);
        return new AsciiCharBuffer(newBuf, false);
    }

    /**
     * Return instance of AsciiCharBuffer of a specified size.
     * @param  capacity integer size of bytes.
     * @param  readOnly boolean if true, buffer is read only, otherwise writable.
     * @return    Instance of AsciiCharBuffer.
     */
    public static AsciiCharBuffer allocate(int capacity, boolean readOnly)
    {
        ByteBuffer newBuf = ByteBuffer.allocate(capacity);
        return new AsciiCharBuffer(newBuf, readOnly);
    }

    /**
     * Return buffer.
     * @return ByteBuffer.
     */
    public ByteBuffer getWrappedBuffer()
    {
        return bb;
    }

    /**
     * Return copy as read-only.
     * @return AsciiCharBuffer as read-only.
     */
    public AsciiCharBuffer asReadOnlyBuffer()
    {
        return new AsciiCharBuffer(bb.duplicate(), true);
    }

    /**
     * Return size of buffer.
     * @return integer of size.
     */
    public int capacity()
    {
        return bb.capacity();
    }

    /**
     * Empty buffer.
     * @return Current AsciiCharBuffer instance.
     */
    public AsciiCharBuffer clear()
    {
        bb.clear();
        return this;
    }

    /**
     * Perform compaction.
     * @return Current AsciiCharBuffer instance.
     */
    public AsciiCharBuffer compact()
    {
        bb.compact();
        return this;
    }

    /**
     * Return copy of this buffer, read-write able.
     * @return Copy of current AsciiCharBuffer instance.
     */
    public AsciiCharBuffer duplicate()
    {
        return new AsciiCharBuffer(bb.duplicate(), false);
    }

    /**
     * Switch this buffer from read-only to read/write.
     * @return Current AsciiCharBuffer instance.
     */
    public AsciiCharBuffer flip()
    {
        bb.flip();
        return this;
    }

    /**
     * Return current character at current position.
     * @return char in ASCII.
     */
    public char get()
    {
        return (char)(bb.get() & 0x00FF);
    }

    /**
     * Copy array of characters into buffer.
     * @param  dst array of char to write into.
     * @return     Instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer get(char[] dst)
    {
        return get(dst, 0, dst.length);
    }

    /**
     * Get from buffer into array
     * @param  dst Array of char buffer to write into.
     * @param  off integer offset into dst to begin writing.
     * @param  l   integer of length to write
     * @return     Current instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer get(char[] dst, int off, int l)
    {
        for (int i = off; i < off + l; i++) {
            dst[i] = (char)(bb.get() & 0x00FF);
        }

        return this;
    }

    /**
     * Return  character at specified position.
     * @param i integer of position to get.
     * @return char in ASCII.
     */
    public char get(int i)
    {
        return (char)(bb.get(i) & 0x00FF);
    }

    /**
     * Determine if buffer has array.
     * @return Always return boolean of false.
     */
    public boolean hasArray()
    {
        return false;
    }

    /**
     * Return if remaining values in buffer.
     * @return boolean true if remaining characters, false if none remaining.
     */
    public boolean hasRemaining()
    {
        return bb.hasRemaining();
    }

    /**
     * Return if direct.
     * @return Always return boolean false.
     */
    public boolean isDirect()
    {
        return false;
    }

    /**
     * Return if read-only.
     * @return boolean true if readonly, otherwise false for read-write.
     */
    public boolean isReadOnly()
    {
        return bb.isReadOnly();
    }

    /**
     * Return limit of buffer.
     * @return integer limit of buffer.
     */
    public int limit()
    {
        return bb.limit();
    }

    /**
     * Specify the limit of the buffer.
     * @param  limit limit to set.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer limit(int limit)
    {
        bb.limit(limit);
        return this;
    }

    /**
     * Set this position.
     * @return [description]
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer mark()
    {
        bb.mark();
        return this;
    }

    /**
     * Return ByteOrder for buffer.
     * @return ByteOrder for buffer.
     */
    public ByteOrder order()
    {
        return bb.order();
    }

    /**
     * Return current position in buffer.
     * @return integer of current position in buffer.
     */
    public int position()
    {
        return bb.position();
    }

    /**
     * Specify new position in buffer.
     * @param  np integer of new position.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer position(int np)
    {
        bb.position(np);
        return this;
    }

    /**
     * Place character at current position.
     * @param  c char to place
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(char c)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        bb.put((byte)c);
        return this;
    }

    /**
     * Place character at specified position.
     * @param  i position
     * @param  c char to place
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(int i, char c)
    {
        bb.put(i, (byte)c);
        return this;
    }

    /**
     * Place subset of character at current position.
     * @param src array of char to place.
     * @param off integer starting offset in src.
     * @param l integer of length to write
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(char[] src, int off, int l)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        for (int i = off; i < off + l; i++) {
            bb.put((byte)src[i]);
        }
        return this;
    }

    /**
     * Place array of char at specified position.
     * @param src array of char to place.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(char[] src)
    {
        return put(src, 0, src.length);
    }

    /**
     * Place CharSequence t current position.
     * @param src CharSequence to place.
     * @param s integer starting offset in src.
     * @param e integer of length to write
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(CharSequence src, int s, int e)
    {
        if (readOnly) { new ReadOnlyBufferException(); }
        for (int i = s; i < s + e; i++) {
            bb.put((byte)src.charAt(i));
        }
        return this;
    }

    /**
     * Place CharSequence current position.
     * @param src CharSequence to place.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer put(CharSequence src)
    {
        return put(src, 0, src.length());
    }

    /**
     * Extract new piece of buffer at current position.
     * @return New instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer slice()
    {
        return new AsciiCharBuffer(bb.slice(), false);
    }

    /**
     * Return count of remaining chars in buffer.
     * @return integer of count of remaining chars in buffer.
     */
    public int remaining()
    {
        return bb.remaining();
    }

    /**
     * Clear the buffer.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer reset()
    {
        bb.reset();
        return this;
    }

    /**
     * Set the buffer to position 0.
     * @return       this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer rewind()
    {
        bb.rewind();
        return this;
    }

    // CharSequence methods ---------------------------------------------------
    /**
     * Return character at ppsition.
     * @param  i integer of position to return.
     * @return   char at position.
     */
    public char charAt(int i)
    {
        return (char)(bb.get(bb.position() + i) & 0x00FF);
    }

    /**
     * Return length of buffer.
     * @return integer length of buffer.
     */
    public int length()
    {
        return bb.remaining();
    }

    /**
     * Rerturn subsquence of buffer.
     * @param  s integer start position.
     * @param  e integer end of position.
     * @return   New instance of AsciiCharBuffer cast as CharSequence.
     */
    public CharSequence subSequence(int s, int e)
    {
        ByteBuffer dup = bb.duplicate();
        dup.position(bb.position() + s);
        dup.limit(bb.position() + e);
        return new AsciiCharBuffer(dup, false);
    }

    /**
     * Return buffer converted to string.
     * @return String of buffer.
     */
    public String toString()
    {
        ByteBuffer dup = bb.duplicate();
        byte[] sb = new byte[dup.remaining()];
        dup.get(sb);
        return new String(sb);
    }

    // Appendable methods -----------------------------------------------------

    /**
     * Add character to buffer.
     * @param  c char to add.
     * @return   this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer append(char c)
    {
        return put(c);
    }

    /**
     * Add character sequence to buffer.
     * @param  csq CharSequence to add.
     * @return   this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer append(CharSequence csq)
    {
        return put(csq);
    }

    /**
     * Add subsequence of character sequence  to buffer.
     * @param  csq CharSequence to add.
     * @param start integer of start position
     * @param end integer of end position.
     * @return   this instance of AsciiCharBuffer.
     */
    public AsciiCharBuffer append(CharSequence csq, int start, int end)
    {
        return put(csq, start, end);
    }

    // Comparable methods -----------------------------------------------------

    /**
     * Compare this AsciiCharBuffer to another.
     * @param  cb AsciiCharBuffer to compare.
     * @return    integer result of compare.
     */
    public int compareTo(AsciiCharBuffer cb)
    {
        return bb.compareTo(cb.bb);
    }

    // Object methods ---------------------------------------------------------

    /**
     * Determine if this AsciiCharBuffer is the same as another.
     * @param  o Target AsciiCharBuffer to compare.
     * @return   boolean where if true, equals, otherwise not. 
     */
    public boolean equals(Object o)
    {
        if (!(o instanceof AsciiCharBuffer)) {
            return false;
        }
        AsciiCharBuffer cb = (AsciiCharBuffer)o;

        return bb.equals(cb.bb);
    }

    /**
     * Return hashcode for sequence.
     * @return integer hashcode.
     */
    public int hashCode()
    {
        return bb.hashCode();
    }
}
