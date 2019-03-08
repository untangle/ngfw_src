/**
 * $Id$
 */
package com.untangle.uvm.util;

import java.nio.ByteBuffer;

/**
 * Class used to create a ByteBuffer incrementally.  The intended use is that
 * this builder is constructed then its {@link #toByteBuffer toByteBuffer()} method
 * called once.
 * <p>
 * Since this class maintains an itnernal array, there are two choices for
 * how the array expands.  {@link GrowthStrategy#DOUBLE DOUBLE} causes the array
 * to be doubled each time an {@link #add add} would overfill the internal array.
 * {@link GrowthStrategy#INCREMENTAL INCREMANTAL} causes the array to grow by the
 * same quantity each time.  Note that the initial size is equal to the
 * growBy value.
 */
public class ByteBufferBuilder {

    private byte[] m_bytes;
    private final int m_growBy;
    private int m_pos;


    public enum GrowthStrategy {
        DOUBLE,
        INCREMENTAL
    };

    private final GrowthStrategy m_strategy;

    private static final int DEF_GROW_BY = 1024;
    private static final GrowthStrategy DEF_GROWTH_STRATEGY = GrowthStrategy.INCREMENTAL;

    /**
     * Initialize instance of ByteBufferBuilder.
     * @return instance of ByteBufferBuilder.
     */
    public ByteBufferBuilder() {
        this(DEF_GROW_BY, DEF_GROWTH_STRATEGY);
    }
    /**
     * Initialize instance of ByteBufferBuilder.
     * @param growBy integer of bytes to increase.
     * @return instance of ByteBufferBuilder.
     */
    public ByteBufferBuilder(int growBy) {
        this(growBy, DEF_GROWTH_STRATEGY);
    }
    /**
     * Initialize instance of ByteBufferBuilder.
     * @param strategy GrowthStrategy to use when incrementing.
     * @return instance of ByteBufferBuilder.
     */
    public ByteBufferBuilder(GrowthStrategy strategy) {
        this(DEF_GROW_BY, strategy);
    }

    /**
     * Construct a ByteBufferBuilder with the
     * given growth strategy and growBy size.  Note
     * that <code>growBy</code> is the initial size,
     * so it should be provided even if the strategy
     * is to double.
     * @param growBy integer of bytes to increase.
     * @param strategy GrowthStrategy to use when incrementing.
     * @return instance of ByteBufferBuilder.
     */
    public ByteBufferBuilder(int growBy,
                             GrowthStrategy strategy) {
        m_growBy = growBy;
        m_bytes = new byte[m_growBy];
        m_strategy = strategy;
    }


    /**
     * Add one byte to the internal array
     * @param b byte to add.
     */
    public void add(byte b) {
        ensure(b);
        m_bytes[m_pos++] = b;
    }
    /**
     * Add array of bytes to the internal array
     * @param bytes Array of bytes to add.
     * @param start integer of starting position within bytes to copy.
     * @param len integer of length to copy from bytes.
     */
    public void add(byte[] bytes, int start, int len) {
        ensure(len);
        System.arraycopy(bytes, start, m_bytes, m_pos, len);
        m_pos+=len;
    }
    /**
     * Add array of bytes to the internal array
     * @param bytes Array of bytes to add.
     */
    public void add(byte[] bytes) {
        add(bytes, 0, bytes.length);
    }
    /**
     * Adds any available content from
     * the given ByteBuffer.  Note that
     * this method does <b>not</b> alter
     * the position of the buffer.
     * @param buf ByteBuffer to add.
     */
    public void add(ByteBuffer buf) {
        final int remaining = buf.remaining();
        ensure(remaining);
        buf.get(m_bytes, m_pos, remaining);
        m_pos+=remaining;
    }

    /**
     * Remove the last <code>num</code> bytes
     * from the array.
     *
     * @param num the number of bytes to remove.
     */
    public void remove(int num) {
        m_pos-=num;
        m_pos = m_pos<0?0:m_pos;
    }

    /**
     * Removes all bytes from this builder.
     * Same as <code>m_builder.remove(m_builder.size());</code>
     */
    public void clear() {
        m_pos = 0;
    }

    /**
     * Remove the last added byte from
     * the internal array
     */
    public void remove() {
        remove(1);
    }
    /**
     * @return The number of bytes added thus far.
     */
    public int size() {
        return m_pos;
    }

    /**
     * Produces a new ByteBuffer, sharing the internal array maintained by this
     * class.  It is unwise to call this method more than once (although I cannot
     * think of a way other than exceptions to prevent this).
     * <p>
     * The new ByteBuffer is ready for reading.  It is also backed by an array, so the
     * <code>array()</code> method on ByteBuffer should be valid.
     *
     * @return a new ByteBuffer, ready for reading.
     */
    public ByteBuffer toByteBuffer() {
        ByteBuffer ret = ByteBuffer.wrap(m_bytes, 0, m_pos);
        return ret;
    }

    /**
     * Verify enough space exists and if not, increase.
     * @param cap integer of capacity to verify.
     */
    private void ensure(int cap) {
        if(m_pos+cap >= m_bytes.length) {
            //Increase.  Note we handle the boundary
            //case where the increased capabity is greater than
            //a single growth occurance
            int newLen = m_pos + cap;
            switch(m_strategy) {
            case DOUBLE:
                newLen = newLen > (m_bytes.length*2)?
                    newLen:(m_bytes.length*2);
                break;
            case INCREMENTAL:
                newLen = newLen > (m_bytes.length + m_growBy)?
                    newLen:(m_bytes.length + m_growBy);

            }
            byte[] newArray = new byte[newLen];
            System.arraycopy(m_bytes, 0, newArray, 0, m_pos);
            m_bytes = newArray;
        }
    }

}
