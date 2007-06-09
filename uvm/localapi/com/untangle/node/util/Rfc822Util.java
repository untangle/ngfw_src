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

import java.nio.ByteBuffer;

import com.untangle.tran.token.ParseException;

import static com.untangle.tran.util.Ascii.*;

public class Rfc822Util
{
    /**
     * Whitespace according to RFC 2822 2.2.2 (SP, HTAB).
     *
     * @param c a <code>char</code> value
     * @return a <code>boolean</code> value
     */
    public static boolean isWhitespace(char c)
    {
        return SP == c || HTAB == c;
    }

    public static boolean isWhitespace(byte b)
    {
        return isWhitespace((char)b);
    }

    public static boolean isTspecial(char c)
    {
        switch (c) {
        case '(': case ')': case '<': case '>': case '@':
        case ',': case ';': case ':': case '\\': case '"':
        case '/': case '[': case ']': case '?': case '=':
            return true;
        default:
            return false;
        }
    }

    /**
     * Consumes contiguous whitespace.
     *
     * @param buf buffer.
     * @return true if whitespace was consumed.
     */
    public static boolean eatSpace(ByteBuffer buf)
    {
        if (buf.hasRemaining() && isWhitespace(buf.get(buf.position()))) {
            while (buf.hasRemaining()) {
                if (!isWhitespace(buf.get())) {
                    buf.position(buf.position() - 1);
                    break;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Consumes the next token from the buffer.
     *
     * @param buf buffer with point on the first character of the token.
     * @return the token.
     */
    public static String consumeToken(ByteBuffer buf)
    {
        StringBuilder sb = new StringBuilder();
        while (buf.hasRemaining()) {
            char c = (char)buf.get();
            if (CR == c || isWhitespace(c)) {
                buf.position(buf.position() - 1);
                break;
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }


    /**
     * Consumes from the point, until but not including the CRLF.
     *
     * @param buf buffer with position
     * @return a <code>String</code> value
     * @exception ParseException if a CRLF was not found in the buffer's remaining data
     */
    public static String consumeLine(ByteBuffer buf)
        throws ParseException {

        int index = BufferUtil.findCrLf(buf);
        if(index < 0) {
            throw new ParseException("No Line terminator in \"" + ASCIIUtil.bbToString(buf) + "\"");
        }
        ByteBuffer dup = buf.duplicate();
        dup.limit(index);
        buf.position(index+2);
        return ASCIIUtil.bbToString(dup);
    }
}
