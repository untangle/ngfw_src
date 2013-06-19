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

package com.untangle.node.smtp.mime;

import static com.untangle.node.util.ASCIIUtil.bbToString;
import static com.untangle.node.util.ASCIIUtil.isEOL;
import static com.untangle.node.util.ASCIIUtil.isLWS;
import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.HTAB;
import static com.untangle.node.util.Ascii.LF;
import static com.untangle.node.util.Ascii.SP;
import static com.untangle.node.util.BufferUtil.endsWith;
import static com.untangle.node.util.BufferUtil.startsWith;

import java.nio.ByteBuffer;

/**
 * Class representing a raw line.  Maintains the terminator
 * character(s) from the original line.  Note that there
 * may not be any terminators on a given line (i.e. if a line
 * is the last in a stream).
 */
public class Line {

    private final ByteBuffer m_buf;
    private final int m_termLen;


    /**
     * PRE: Although not enforced, the ByteBuffer should
     *      have a backing array
     *
     * @param buf the Buffer <b>with</b> any terminating characters within its limit
     */
    public Line(ByteBuffer buf,
                int termLen) {

        m_buf = (ByteBuffer) buf.rewind();
        m_termLen = termLen;
    }

    /**
     * Returns a new Line (with shared content)
     * which has no terminator
     */
    public Line removeTerminator() {
        if(m_termLen == 0) {
            return this;
        }
        ByteBuffer newBuf = m_buf.slice();
        newBuf.limit(newBuf.limit() - m_termLen);
        return new Line(newBuf, 0);
    }

    /**
     * Get a ByteBuffer representing the bytes of the line.  Callers may modify the
     * limit and position as-per the rules of ByteBuffer.  Any changes
     * to the contents <b>will</b> be seen by other callers of this method (i.e.
     * there is one backing array, but new slices returned by each call).
     *
     * @param includeTermInView if true, any characters which terminated this
     *        line are also returned within the limit of the buffer.
     *
     * @return the ByteBuffer with the line, positioned at the start
     *         of the line bytes with the limit set including the
     *         terminator character(s) if <code>includeTermInView</code>
     */
    public ByteBuffer getBuffer(boolean includeTermInView) {
        ByteBuffer ret = m_buf.slice();
        if(!includeTermInView) {
            ret.limit(ret.limit() - m_termLen);
        }
        return ret;
    }
    /**
     * Gets the bytes of the line as a ByteBuffer, without
     * any line terminators within the window.  Equivilant
     * to calling <code>getBuffer(false)</code>
     *
     * @return the ByteBuffer with the line, positioned at the start
     *         of the line bytes with the limit set just before the
     *         terminator character(s)
     */
    public ByteBuffer getBuffer() {
        return getBuffer(false);
    }

    /**
     * The length of a buffer returned
     * from {@link #getBuffer getBuffer(false)}.
     */
    public int bufferLen() {
        return m_buf.remaining();
    }

    /**
     * Get the number of characters used in the original line
     * for termination.  May be zero.
     *
     * @return length of terminator character(s)
     */
    public int getTermLen() {
        return m_termLen;
    }

    public boolean bufferStartsWith(String aStr) {
        return startsWith(getBuffer(false), aStr);
    }

    public boolean bufferEndsWith(String aStr) {
        return endsWith(getBuffer(false), aStr);
    }

    public String bufferToString() {
        return bbToString(getBuffer(false));
    }


    /**
     * If <code>unfoldLines</code> is true, then
     * this assumes the lines are RFC822 formatted
     * header lines.
     */
    public static String linesToString(Line[] lines,
                                       int startingAt,
                                       boolean unfoldLines) {

        LineIterator li = new LineIterator(lines, unfoldLines);
        if(startingAt > 0) {
            li.skip(startingAt);
        }
        StringBuilder sb = new StringBuilder();
        int b = li.next();
        while(b != -1) {
            sb.append((char)(b & 0x00FF));
            b = li.next();
        }
        //    String ret = sb.toString();
        //    System.out.println("***DEBUG*** [linesToString] returning \"" + ret + "\"");
        return sb.toString();

        //    return linesToString(lines, startingAt, Integer.MAX_VALUE, unfoldLines);
    }
    /**
     * Helper method (since I didn't think there was enough of a reason
     * to create a "LineList" or something).
     * <p>
     * This method does assist in changing the
     * position of the first Line.
     * <p>
     * <code>len</code> includes any folding for InternetHeader lines.
     * However, they are not returned in the returned String
     */
    @SuppressWarnings("unused")
    private static String linesToString(Line[] lines,
                                        int startingAt,
                                        int len,
                                        boolean unfoldLines) {

        //==============================================
        // TODO bscott This (goofy) implementation
        //      not only looks bad, but also adds
        //      an extra space to the end of headers
        //==============================================

        StringBuilder sb = new StringBuilder();
        int xFered = 0;
        char c;

        ByteBuffer bb;

        for(int i = 0; i<lines.length; i++) {
            bb = lines[i].getBuffer(true);
            if(i == 0) {
                bb.position(bb.position() + startingAt);
            }
            while(xFered < len && bb.hasRemaining()) {
                c = (char) (bb.get() & 0x00FF);
                xFered++;
                if(unfoldLines && (c == CR || c == LF)) {
                    xFered+=eatWhitespace(bb, len - xFered);
                    //Nasty hack.  Replace "c" with
                    //a SP, and let the append below pick it up
                    c = SP;
                    if(xFered >= len) {
                        sb.append(SP);
                        return sb.toString();
                    }
                }
                sb.append(c);
            }
            if(xFered >= len) {
                return sb.toString();
            }
        }
        return sb.toString();

    }

    private static int eatWhitespace(ByteBuffer buf,
                                     int maxToConsume) {
        int count = 0;
        while (buf.hasRemaining() && (count < maxToConsume)) {
            byte b = buf.get();
            if(!(
                 b == CR ||
                 b == LF ||
                 b == HTAB ||
                 b == SP)
               ) {
                buf.position(buf.position()-1);
                break;
            }
            count++;
        }
        return count;
    }

    public static void main(String[] args)
        throws Exception {

        String s1 = "foo: goo\r\n";
        String s2 = "\tdoo\r\n";

        Line l1 = new Line(ByteBuffer.wrap(s1.getBytes()), 2);
        Line l2 = new Line(ByteBuffer.wrap(s2.getBytes()), 2);

        System.out.println(linesToString(new Line[] {l1, l2},
                                         5,
                                         true));

    }

    /**
     * I'm feeling too lazy to write the
     * unfold method without this helper.  It
     * is a bit of a beating on the CPU relative
     * to its value.
     */
    private static class LineIterator {
        private final boolean m_unfoldLines;
        private final ByteBuffer[] m_buffers;
        private final int m_numBuffers;
        private int m_currentBuffer;

        LineIterator(Line[] lines,
                     boolean unfold) {
            m_buffers = new ByteBuffer[lines.length];
            int i = 0;
            for(Line line : lines) {
                m_buffers[i++] = line.getBuffer(true);
            }
            m_numBuffers = m_buffers.length;
            m_unfoldLines = unfold;
            m_currentBuffer = 0;
        }
        void skip(int num) {
            while(num > 0 && m_currentBuffer < m_numBuffers) {
                int toSkip = m_buffers[m_currentBuffer].remaining();
                if(toSkip <= 0) {
                    //Nothing left in this buffer.  Go to the next
                    m_currentBuffer++;
                    continue;
                }
                if(toSkip > num) {
                    //We're skipping less than the current buffer's remaining
                    toSkip = num;
                }
                m_buffers[m_currentBuffer].position(m_buffers[m_currentBuffer].position() + toSkip);
                num-=toSkip;
            }
        }

        int next() {
            while(m_currentBuffer < m_numBuffers) {
                if(!m_buffers[m_currentBuffer].hasRemaining()) {
                    m_currentBuffer++;
                    continue;
                }
                byte ret = m_buffers[m_currentBuffer].get();
                if(!m_unfoldLines) {
                    return ret;
                }
                //We're unfolding
                if(isEOL(ret)) {
                    //see if next is another new line char
                    ByteBuffer currentBuf = getBuffer();
                    if(currentBuf != null) {
                        ret = currentBuf.get();
                        if(isEOL(ret)) {
                            if(eatWhitespace()) {
                                return SP;
                            }
                            else {
                                return getBuffer() == null?
                                    -1:
                                    getBuffer().get();
                            }
                        }
                        else {
                            //Odd.  CRX where "X" is not
                            //whatespace.  Just return it.
                            return ret;
                        }
                    }
                    else {
                        //Ended with a CR.  Odd, but we'll declare
                        //that the end
                        return -1;
                    }
                }
                return ret;
            }
            return -1;
        }

        private boolean eatWhitespace() {
            ByteBuffer buf = getBuffer();
            if(buf == null) {
                return false;
            }
            boolean ret = false;
            while(buf != null && buf.hasRemaining()) {
                byte b = buf.get();
                if(!isLWS(b)) {
                    buf.position(buf.position() - 1);
                    return ret;
                }
                ret = true;
                buf = getBuffer();
            }
            return ret;
        }

        /**
         * Returns the current buffer.  If the current
         * buffer is empty, advances to the next buffer.  If
         * there is no "next" buffer, null is returned.
         */
        private ByteBuffer getBuffer() {
            while(m_currentBuffer < m_numBuffers) {
                if(!m_buffers[m_currentBuffer].hasRemaining()) {
                    m_currentBuffer++;
                    continue;
                }
                return m_buffers[m_currentBuffer];
            }
            return null;
        }

    }
}
