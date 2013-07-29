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

import static com.untangle.node.util.Ascii.CR;
import static com.untangle.node.util.Ascii.LF;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Utility methods for parsing <code>ByteBuffer</code>s.
 *
 */
public class BufferUtil
{
    public static boolean endsWithCrLf(ByteBuffer buf)
    {
        return 2 <= buf.remaining()
            && CR == buf.get(buf.limit() - 2)
            && LF == buf.get(buf.limit() - 1);
    }

    /**
     * Find CRLF, starting from position.
     *
     * @param buf buffer to search.
     * @return the absolute index of start of CRLF, or -1 if not
     * found.
     */
    public static int findCrLf(ByteBuffer buf)
    {
        for (int i = buf.position(); i < buf.limit() - 1; i++) {
            if (CR == buf.get(i) && LF == buf.get(i + 1)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Test if buf starts with a string.
     *
     * @param buf ByteBuffer to test.
     * @param s String to match.
     * @return true if the ByteBuffer starts with the String, false
     * otherwise.
     */
    public static boolean startsWith(ByteBuffer buf, String s)
    {
        if (buf.remaining() < s.length()) {
            return false;
        }

        int pos = buf.position();

        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) != buf.get(pos + i)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Test if this ByteBuffer ends with the given String
     *
     * @param buf the buffer to test
     * @param s the String to match
     *
     * @return true if the buffer ends with the String
     */
    public static boolean endsWith(ByteBuffer buf, String s) {
        if(buf.remaining() < s.length()) {
            return false;
        }
        final int len = s.length();
        final int bufOffset = buf.limit() - len;

        for(int i = 0; i<len; i++) {
            if(((char) buf.get(i+bufOffset)) != s.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Find the start of the given pattern within the buffer.
     * Returns the index of the start of the pattern (i.e.
     * the index of pattern[offset] within buf).
     */
    public static int findPattern(final ByteBuffer buf,
                                  final byte[] pattern,
                                  final int offset,
                                  final int len) {

        //TODO bscott implement this correctly
        return findString(buf, new String(pattern, offset, len));
    }


    /**
     * Find a string in a buffer.
     *
     * @param buf buffer to search.
     * @param str string to match.
     * @return the absolute index, or -1 if not found.
     */
    public static int findString(ByteBuffer buf, String str)
    {
        ByteBuffer dup = buf.duplicate();

        while (str.length() <= dup.remaining()) {
            if (startsWith(dup, str)) {
                return dup.position();
            } else {
                dup.get();
            }
        }

        return -1;
    }

    public static int findLastString(ByteBuffer buf, String str)
    {
        ByteBuffer dup = buf.duplicate();

        for (int i = buf.limit() - str.length(); buf.position() <= i; i--) {
            dup.position(i);
            if (startsWith(dup, str)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Transfers the contents of the buffer to the given
     * OutputStream
     *
     * @param buf the source buffer
     * @param out the output stream
     */
    public static void writeBufferToStream(final ByteBuffer buf, final OutputStream out)
        throws IOException {
        if(buf.hasArray()) {
            out.write(buf.array(), buf.arrayOffset() + buf.position(), buf.remaining());
        }
        else {
            while(buf.hasRemaining()) {
                out.write(buf.get());
            }
        }
    }
}
