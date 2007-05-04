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

import static com.untangle.tran.util.Ascii.*;

import java.io.*;
import java.nio.ByteBuffer;

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
