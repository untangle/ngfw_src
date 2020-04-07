/**
 * $Id$
 */
package com.untangle.app.smtp.mime;

import static com.untangle.uvm.util.AsciiUtil.bbToString;
import static com.untangle.uvm.util.AsciiUtil.isEOL;
import static com.untangle.uvm.util.AsciiUtil.isLWS;
import static com.untangle.uvm.util.Ascii.SP;
import static com.untangle.uvm.util.BufferUtil.endsWith;
import static com.untangle.uvm.util.BufferUtil.startsWith;

import java.nio.ByteBuffer;

/**
 * Class representing a raw line. Maintains the terminator character(s) from the original line. Note that there may not
 * be any terminators on a given line (i.e. if a line is the last in a stream).
 */
public class Line
{

    private final ByteBuffer m_buf;
    private final int m_termLen;

    /**
     * PRE: Although not enforced, the ByteBuffer should have a backing array
     * 
     * @param buf
     *            the Buffer <b>with</b> any terminating characters within its limit
     * @param termLen Terimination length.
     * @return Instance of Line.
     */
    public Line(ByteBuffer buf, int termLen) {

        m_buf = buf.rewind();
        m_termLen = termLen;
    }

    /**
     * Returns a new Line (with shared content) which has no terminator
     * @return Line instance.
     */
    public Line removeTerminator()
    {
        if (m_termLen == 0) {
            return this;
        }
        ByteBuffer newBuf = m_buf.slice();
        newBuf.limit(newBuf.limit() - m_termLen);
        return new Line(newBuf, 0);
    }

    /**
     * Get a ByteBuffer representing the bytes of the line. Callers may modify the limit and position as-per the rules
     * of ByteBuffer. Any changes to the contents <b>will</b> be seen by other callers of this method (i.e. there is one
     * backing array, but new slices returned by each call).
     * 
     * @param includeTermInView
     *            if true, any characters which terminated this line are also returned within the limit of the buffer.
     * 
     * @return the ByteBuffer with the line, positioned at the start of the line bytes with the limit set including the
     *         terminator character(s) if <code>includeTermInView</code>
     */
    public ByteBuffer getBuffer(boolean includeTermInView)
    {
        ByteBuffer ret = m_buf.slice();
        if (!includeTermInView) {
            ret.limit(ret.limit() - m_termLen);
        }
        return ret;
    }

    /**
     * Gets the bytes of the line as a ByteBuffer, without any line terminators within the window. Equivilant to calling
     * <code>getBuffer(false)</code>
     * 
     * @return the ByteBuffer with the line, positioned at the start of the line bytes with the limit set just before
     *         the terminator character(s)
     */
    public ByteBuffer getBuffer()
    {
        return getBuffer(false);
    }

    /**
     * The length of a buffer returned from {@link #getBuffer getBuffer(false)}.
     * @return length of buffer as integer.
     */
    public int bufferLen()
    {
        return m_buf.remaining();
    }

    /**
     * Get the number of characters used in the original line for termination. May be zero.
     * 
     * @return length of terminator character(s)
     */
    public int getTermLen()
    {
        return m_termLen;
    }

    /**
     * Determine if buffer begins with a string.
     * @param  aStr String value to check.
     * @return      true if buffer begins with string, false if not.
     */
    public boolean bufferStartsWith(String aStr)
    {
        return startsWith(getBuffer(false), aStr);
    }

    /**
     * Determine if buffer end with a string.
     * @param  aStr String value to check.
     * @return      true if buffer ends with string, false if not.
     */
    public boolean bufferEndsWith(String aStr)
    {
        return endsWith(getBuffer(false), aStr);
    }

    /**
     * Convert buffer to string.
     * @return String value of buffer.
     */
    public String bufferToString()
    {
        return bbToString(getBuffer(false));
    }

    /**
     * If <code>unfoldLines</code> is true, then this assumes the lines are RFC822 formatted header lines.
     * @param lines Array of Line.
     * @param startingAt Integer to begin ati.
     * @param unfoldLines If true, unfold the lines.
     * @return String of lines to a single string.
     */
    public static String linesToString(Line[] lines, int startingAt, boolean unfoldLines)
    {

        LineIterator li = new LineIterator(lines, unfoldLines);
        if (startingAt > 0) {
            li.skip(startingAt);
        }
        StringBuilder sb = new StringBuilder();
        int b = li.next();
        while (b != -1) {
            sb.append((char) (b & 0x00FF));
            b = li.next();
        }
        return sb.toString();
    }

    /**
     * I'm feeling too lazy to write the unfold method without this helper. It is a bit of a beating on the CPU relative
     * to its value.
     */
    private static class LineIterator
    {
        private final boolean m_unfoldLines;
        private final ByteBuffer[] m_buffers;
        private final int m_numBuffers;
        private int m_currentBuffer;

        /**
         * Intiialize instance of LineIterator
         * @param lines Array of Line.
         * @param unfold If true, unfold the lines.
         * @return Instance of LneIterator.
         */
        LineIterator(Line[] lines, boolean unfold) {
            m_buffers = new ByteBuffer[lines.length];
            int i = 0;
            for (Line line : lines) {
                m_buffers[i++] = line.getBuffer(true);
            }
            m_numBuffers = m_buffers.length;
            m_unfoldLines = unfold;
            m_currentBuffer = 0;
        }

        /**
         * Skip to position.
         * @param num Integer of position to skip it.
         */
        void skip(int num)
        {
            while (num > 0 && m_currentBuffer < m_numBuffers) {
                int toSkip = m_buffers[m_currentBuffer].remaining();
                if (toSkip <= 0) {
                    // Nothing left in this buffer. Go to the next
                    m_currentBuffer++;
                    continue;
                }
                if (toSkip > num) {
                    // We're skipping less than the current buffer's remaining
                    toSkip = num;
                }
                m_buffers[m_currentBuffer].position(m_buffers[m_currentBuffer].position() + toSkip);
                num -= toSkip;
            }
        }

        /**
         * Advanced to next line.
         * @return New position.
         */
        int next()
        {
            while (m_currentBuffer < m_numBuffers) {
                if (!m_buffers[m_currentBuffer].hasRemaining()) {
                    m_currentBuffer++;
                    continue;
                }
                byte ret = m_buffers[m_currentBuffer].get();
                if (!m_unfoldLines) {
                    return ret;
                }
                // We're unfolding
                if (isEOL(ret)) {
                    // see if next is another new line char
                    ByteBuffer currentBuf = getBuffer();
                    if (currentBuf != null) {
                        ret = currentBuf.get();
                        if (isEOL(ret)) {
                            if (eatWhitespace()) {
                                return SP;
                            } else {
                                ByteBuffer gb = getBuffer();
                                return (gb == null ? -1 : gb.get());
                            }
                        } else {
                            // Odd. CRX where "X" is not
                            // whatespace. Just return it.
                            return ret;
                        }
                    } else {
                        // Ended with a CR. Odd, but we'll declare
                        // that the end
                        return -1;
                    }
                }
                return ret;
            }
            return -1;
        }

        /**
         * Remove whitespace.
         * @return trye if found whitespace, false otherwise.
         */
        private boolean eatWhitespace()
        {
            ByteBuffer buf = getBuffer();
            if (buf == null) {
                return false;
            }
            boolean ret = false;
            while (buf != null && buf.hasRemaining()) {
                byte b = buf.get();
                if (!isLWS(b)) {
                    buf.position(buf.position() - 1);
                    return ret;
                }
                ret = true;
                buf = getBuffer();
            }
            return ret;
        }

        /**
         * Returns the current buffer. If the current buffer is empty, advances to the next buffer. If there is no
         * "next" buffer, null is returned.
         * @return Next ByteBuffer.
         */
        private ByteBuffer getBuffer()
        {
            while (m_currentBuffer < m_numBuffers) {
                if (!m_buffers[m_currentBuffer].hasRemaining()) {
                    m_currentBuffer++;
                    continue;
                }
                return m_buffers[m_currentBuffer];
            }
            return null;
        }

    }

    /************** Tests ******************/

    /**
     * Run test.
     * @param  args      String of arguments to test.
     * @return           String of result.
     * @throws Exception On test exception.
     */
    public static String runTest(String[] args) throws Exception
    {

        String s1 = "foo: goo\r\n";
        String s2 = "\tdoo\r\n";

        Line l1 = new Line(ByteBuffer.wrap(s1.getBytes()), 2);
        Line l2 = new Line(ByteBuffer.wrap(s2.getBytes()), 2);

        return Line.linesToString(new Line[] { l1, l2 }, 5, true);

    }
}
