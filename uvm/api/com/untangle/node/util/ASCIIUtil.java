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
import static com.untangle.node.util.Ascii.HTAB;
import static com.untangle.node.util.Ascii.LF;
import static com.untangle.node.util.Ascii.SP;

import java.nio.ByteBuffer;


/**
 * Utility class to manipulating "stuff" in the ASCII character set.
 * Where Strings are being produced, it is implicit that
 * they are in the ascii characterset.
 * <p>
 * This class is tolerant of different EOL characters.  An EOL
 * can be expressed as one or two characters, and be either CR or
 * LF.  This is the standard for internet protocols.  If two "LF"
 * characters are to be considered two lines (such as reading Unix files)
 * then use of this class is inappropriate (or it needs to be
 * modified).
 * <p>
 * We use two terms to describe whitespace, loosely based on
 * RFC 822 (there are later RFC's which make whitespace more ambiguious).
 * <code>LWS</code> is defined as a horizontal tab or the space character.
 * "whitespace" is considered LWS and any EOL (see above) characters.
 * <p>
 * We also use the term "EOF" ("End of file") liberally.  When dealing
 * with A ByteBuffer, EOF means "the end of the buffer".
 */
public final class ASCIIUtil {

    private static final byte UPPER_LOWER_DIFF = 0x20;

    //Ensure this is only a collection of functions
    private ASCIIUtil() {}


    /**
     * For debugging.  Prints the character.  If the character is
     * not printable on a normal terminal, prints the decimal value
     */
    public static String asciiByteToString(byte b) {
        if(b>=33 && b <=126) {
            return new String(new byte[]{b});
        }
        else {
            return "(unprintable) " + Integer.toString(b);
        }
    }


    /**
     * Converts a ByteBuffer to a String
     * <p>
     * The buffer's position is reset to original position
     * after this method completes.
     *
     * @param buf the buffer.
     * @return the String.  If the buffer is empty, a zero-length
     *         String should be returned.
     */
    public static String bbToString(ByteBuffer buf) {
        buf.mark();
        ASCIIStringBuilder sb = new ASCIIStringBuilder();
        while(buf.hasRemaining()) {
            sb.append(buf.get());
        }
        buf.reset();
        return sb.toString();
    }

    /**
     * Advances the position to endIndexExclusive
     */
    public static String readString(ByteBuffer buf,
                                    int endIndexExclusive) {
        ByteBuffer dup = buf.duplicate();
        dup.limit(endIndexExclusive);
        buf.position(endIndexExclusive);
        return bbToString(dup);
    }

    /**
     * Read an ASCII String from the buffer.  All
     * characters up-to the <code>delim</code> are considered
     * part of the returned String.  Note that the end
     * of the buffer ("EOF") is considered a delimiter.
     * <p>
     * This method advances the position of the buffer upon
     * completion (past what was returned in the String).
     *
     * @param buf the Buffer
     * @param delim the delimiter byte
     * @param returnDelim if true, the delimiter will be returned
     *        as part of the String token.  Note that (obviously)
     *        EOF cannot be returned.
     */
    public static String readString(ByteBuffer buf,
                                    byte delim,
                                    boolean returnDelim) {
        return readString(
                          buf,
                          delim,
                          false,
                          false,
                          true,
                          returnDelim);
    }

    /**
     * Read an ASCII String from the buffer.  All
     * characters up-to the terminator are considered
     * part of the returned String.
     * <p>
     * This method advances the position of the buffer upon
     * completion (past what was returned in the String).
     *
     * @param buf the Buffer
     * @param delim the delimiter byte (set to 0 for no delimiter)
     * @param isEOLDelim is a line terminator considered a delimiter
     * @param isEOFDelim is EOF a delimiter
     * @param isLWSDelim is LWS a delimiter
     * @param returnDelim if true, the delimiter will be returned
     *        as part of the String token.  If <code>isEOFDelim</code>
     *        is false and the end of the buffer is reached without
     *        another delimiter, null is returned.
     */
    public static String readString(ByteBuffer buf,
                                    byte delim,
                                    boolean isEOLDelim,
                                    boolean isEOFDelim,
                                    boolean isLWSDelim,
                                    boolean returnDelim) {

        ASCIIStringBuilder sb = new ASCIIStringBuilder();

        buf.mark();
        byte b;
        while(buf.hasRemaining()) {
            b = buf.get();
            if(isEOL(b)) {
                if(isEOLDelim) {
                    if(returnDelim) {
                        sb.append(b);
                        transferWhitespace(buf, sb, isEOLDelim, isLWSDelim);
                    }
                    return sb.toString();
                }
            }
            if(isLWS(b)) {
                if(isLWSDelim) {
                    if(returnDelim) {
                        sb.append(b);
                        transferWhitespace(buf, sb, isEOLDelim, isLWSDelim);
                    }
                    return sb.toString();
                }
            }
            if(b == delim) {
                if(returnDelim) {
                    sb.append(b);
                }
                return sb.toString();
            }
            sb.append(b);
        }

        //If we're here, we ran out of bytes
        if(isEOFDelim) {
            return sb.toString();
        }
        buf.reset();
        return null;

    }

    /**
     * Is the given byte a CR or LF
     */
    public static boolean isEOL(byte b) {
        return b == CR || b == LF;
    }

    /**
     * Is the given byte a HTAB or space
     */
    public static boolean isLWS(byte b) {
        return b == SP || b == HTAB;
    }

    public static boolean isNumber(byte b) {
        return (b >=48 && b <=57);
    }

    /**
     * Would the next call to "get" on the buffer
     * return a {@link #isLWS LWS} character.
     *
     * @param buf the buffer
     * @return true if a LWS character, false if not
     *         or the buffer is empty.
     */
    public static boolean isNextLWS(ByteBuffer buf) {
        if(buf.hasRemaining()) {
            if(isLWS(buf.get())) {
                buf.position(buf.position() - 1);
                return true;
            }
            else {
                buf.position(buf.position() - 1);
                return false;
            }
        }
        return false;
    }

    /**
     * Checks for a "blank" line, meaning the contents of the buffer
     * is only LWS.
     */
    public static boolean isAllLWS(ByteBuffer buf) {
        buf.mark();
        while(buf.hasRemaining()) {
            if(!isLWS(buf.get())) {
                buf.reset();
                return false;
            }
        }
        buf.reset();
        return true;
    }



    /**
     * Converts the byte to lower-case, if it is in the alpha range.
     *
     * @param b the byte
     * @return a lowercase byte equivilant, or the passed-in byte
     */
    public static final byte toLower(byte b) {
        if(b >= 65 && b <= 90) {
            return (byte) (b+UPPER_LOWER_DIFF);
        }
        return b;
    }
    public static final byte toUpper(byte b) {
        if(b >= 97 && b <= 122) {
            return (byte) (b-UPPER_LOWER_DIFF);
        }
        return b;
    }

    /**
     * Compare the ASCII bytes, using a case-insensitive compare
     * if they are alpha characters.
     */
    public static final boolean equalsIgnoreCase(final byte b1, final byte b2) {
        return toLower(b1) == toLower(b2);
    }

    /**
     * Compare two arrays, optionally ignoring case
     * (assuming the arrays are ASCII bytes).
     *
     *
     */
    public static final boolean compareArrays(final byte[] a1,
                                              final byte[] a2,
                                              final boolean ignoreCase) {
        return compareArrays(a1,
                             0,
                             a1==null?0:a1.length,
                             a2,
                             0,
                             a2==null?0:a2.length,
                             ignoreCase);

    }

    /**
     * Compare two arrays, optionally ignoring case
     * (assuming the arrays are ASCII bytes).
     *
     *
     */
    public static final boolean compareArrays(final byte[] a1,
                                              final int a1Start,
                                              final int a1Len,
                                              final byte[] a2,
                                              final int a2Start,
                                              final int a2Len,
                                              final boolean ignoreCase) {

        if(a1 == null || a2 == null) {
            return a1==null && a2 == null;
        }

        if(a1Len != a2Len) {
            return false;
        }

        for(int i = 0; i<a1Len; i++) {
            if(ignoreCase) {
                if(toLower(a1[a1Start + i]) != toLower(a2[a2Start + i])) {
                    return false;
                }
            }
            else {
                if(a1[a1Start + i] != a2[a2Start + i]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Test if the buffers contain the same bytes.
     * <br><br>
     * This has been added to this class for case-insensitive compares.
     */
    public static final boolean buffersEqual(ByteBuffer b1,
                                             ByteBuffer b2,
                                             boolean ignoreCase) {
        if(b1.remaining() != b2.remaining()) {
            return false;
        }
        return startsWith(b1, b2, ignoreCase);
    }

    /**
     * Test if the target ByteBuffer starts with the bytes in compare.
     * <br><br>
     * This has been added to this class for case-insensitive compares.
     * <br><br>
     * Note that this method does <b>not</b> modify the
     * source or target buffers.
     */
    public static final boolean startsWith(final ByteBuffer target,
                                           final ByteBuffer compare,
                                           final boolean ignoreCase) {

        if(target.remaining() < compare.remaining()) {
            return false;
        }
        final int len = compare.remaining();
        final int comparePos = compare.position();
        final int targetPos = target.position();

        for(int i = 0; i<len; i++) {
            if(ignoreCase) {
                if(!equalsIgnoreCase(compare.get(comparePos+i), target.get(targetPos+i))) {
                    return false;
                }
            }
            else {
                if(compare.get(comparePos+i) != target.get(targetPos+i)) {
                    return false;
                }
            }
        }
        return true;
    }


    /**
     * Transfer whitespace from the given buffer
     * to the ASCIIStringBuilder.  The position
     * of the buffer is advanced for all bytes
     * transferred.
     *
     * @param buf the source buffer
     * @param sb the target
     * @param isEOLWhitespace are line terminators considered
     *        for transfer.  If not, they mark the end of the
     *        transfer.
     * @param isLWSWhitespace are LWS characters considered
     *        for transfer.  If not, they mark the end of the
     *        transfer.
     */
    public static void transferWhitespace(ByteBuffer buf,
                                          ASCIIStringBuilder sb,
                                          boolean isEOLWhitespace,
                                          boolean isLWSWhitespace) {

        byte b;
        while(buf.hasRemaining()) {
            b = buf.get();
            if(isEOL(b)){
                if(isEOLWhitespace) {
                    sb.append(b);
                }
                else {
                    buf.position(buf.position() - 1);
                    return;
                }
            }
            if(isLWS(b)) {
                if(isLWSWhitespace) {
                    sb.append(b);
                }
                else {
                    buf.position(buf.position() - 1);
                    return;
                }
            }
        }
    }

    /**
     * Consumes whitespace from the buffer, advancing its position.
     *
     * @param buf the buffer
     * @param isEOLWhitespace If true, EOL is considered whitespace
     *
     * @return the number of bytes consumed.
     */
    public static int eatWhitespace(ByteBuffer buf,
                                    boolean isEOLWhitespace) {

        return eatWhitespace(buf, isEOLWhitespace, Integer.MAX_VALUE);
    }

    /**
     * Consumes whitespace from the buffer, advancing its position.
     *
     * @param buf the buffer
     * @param isEOLWhitespace If true, EOL is considered whitespace
     * @param maxToConsume the max bytes to consume.
     *
     * @return the number of bytes consumed, not exceeding
     *         <code>maxToConsume</code>
     */
    public static int eatWhitespace(ByteBuffer buf,
                                    boolean isEOLWhitespace,
                                    int maxToConsume) {

        int count = 0;
        while (buf.hasRemaining() && (count < maxToConsume)) {
            byte b = buf.get();
            if(!(
                 isLWS(b) ||
                 (isEOLWhitespace && isEOL(b))
                 )) {
                buf.position(buf.position()-1);
                break;
            }
            count++;
        }
        return count;
    }

}
