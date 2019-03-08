/**
 * $Id$
 */
package com.untangle.uvm.util;

import static com.untangle.uvm.util.Ascii.CR;
import static com.untangle.uvm.util.Ascii.HTAB;
import static com.untangle.uvm.util.Ascii.LF;
import static com.untangle.uvm.util.Ascii.SP;

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
public final class AsciiUtil {

    private static final byte UPPER_LOWER_DIFF = 0x20;

    /**
     * Ensure this is only a collection of functions
     */
    private AsciiUtil() {}

    /**
     * For debugging.  Prints the character.  If the character is
     * not printable on a normal terminal, prints the decimal value
     * @param b byte to convert to String.
     * @return String of converted byte.
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
        AsciiStringBuilder sb = new AsciiStringBuilder();
        while(buf.hasRemaining()) {
            sb.append(buf.get());
        }
        buf.reset();
        return sb.toString();
    }

    /**
     * Advances the position to endIndexExclusive
     * @param buf ByteBuffer to advance.
     * @param endIndexExclusive integer to set for limit and position.
     * @return String of this value.
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
     * @return String of this value.
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
     * @return String of this value.
     */
    public static String readString(ByteBuffer buf,
                                    byte delim,
                                    boolean isEOLDelim,
                                    boolean isEOFDelim,
                                    boolean isLWSDelim,
                                    boolean returnDelim) {

        AsciiStringBuilder sb = new AsciiStringBuilder();

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
     * Determine if given byte a CR or LF
     * @param b Byte to check.
     * @return true of bute is CR or LF, false otherwise.
     */
    public static boolean isEOL(byte b) {
        return b == CR || b == LF;
    }

    /**
     * Determine if given byte a HTAB or space
     * @param b Byte to check.
     * @return true of bute is tab or space, false otherwise.
     */
    public static boolean isLWS(byte b) {
        return b == SP || b == HTAB;
    }

    /**
     * Determine if given byte a number.
     * @param b Byte to check.
     * @return true of bute is number 0-9.
     */
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
     * @param buf ByteBuffer to check.
     * @return true if entire buffer is space or tab, false if not.
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
    /**
     * Converts the byte to upper-case, if it is in the alpha range.
     *
     * @param b the byte
     * @return a uppercase byte equivilant, or the passed-in byte
     */
    public static final byte toUpper(byte b) {
        if(b >= 97 && b <= 122) {
            return (byte) (b-UPPER_LOWER_DIFF);
        }
        return b;
    }

    /**
     * Compare the ASCII bytes, using a case-insensitive compare
     * if they are alpha characters.
     * @param b1 First byte to compare.
     * @param b2 Second byte to compare.
     * @return true if the two bytes ignorin case.
     */
    public static final boolean equalsIgnoreCase(final byte b1, final byte b2) {
        return toLower(b1) == toLower(b2);
    }

    /**
     * Compare two arrays, optionally ignoring case
     * (assuming the arrays are ASCII bytes).
     * @param a1 First array of bytes to compare.
     * @param a2 Second array of bytes to compare.
     * @param ignoreCase boolean if true, compare ignoring case, otherwise considuer case.
     * @return boolean true if arrays are equal, false otherwise.
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
     * @param a1 First array of bytes to compare.
     * @param a1Start integer of starting position in first array.
     * @param a1Len integer of length in first array
     * @param a2 Second array of bytes to compare.
     * @param a2Start integer of starting position in second array
     * @param a2Len integer of length in first array
     * @param ignoreCase boolean if true, compare ignoring case, otherwise considuer case.
     * @return boolean true if arrays are equal, false otherwise.
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
     * @param b1 ByteBuffer to compare.
     * @param b2 ByteBuffer to compare.
     * @param ignoreCase boolean if true, compare ignoring case, otherwise considuer case.
     * @return boolean true if buffers are equal, false otherwise.
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
     * @param target ByteBuffer to consider.
     * @param compare ByteBuffer to test.
     * @param ignoreCase boolean if true, compare ignoring case, otherwise considuer case.
     * @return boolean target begins with bytes in compare, false otherwise.
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
     * to the AsciiStringBuilder.  The position
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
                                          AsciiStringBuilder sb,
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
