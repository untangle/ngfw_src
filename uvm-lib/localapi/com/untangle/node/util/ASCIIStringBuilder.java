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

/**
 * Modeled after the Java <code>StringBuilder</code> class,
 * yet exclusivly for ASCII characters.  Useful for creating
 * Strings from bytes in various internet protocols (SMTP, FTP, etc).
 * <p>
 * Instances are <b>not</b> threadsafe.
 * <p>
 * Note that this class always <i>should</i> work.  However, if a given
 * app is leary of the JVM running, the method {@link #testPlatform  testPlatform}
 * may be called as a test.  Note that when used within the known
 * Untangle Platform, it should never be a problem.
 */
public class ASCIIStringBuilder {

    private static final byte[] NULL_BYTES =
        new byte[] {'n', 'u', 'l', 'l'};

    private static final String ISO_LATIN_CHARSET_NAME = "ISO-8859-1";
    private static final String ASCII_CHARSET_NAME = "US-ASCII";



    //XXXXX bscott Could serialization of classes between platforms
    //      mess-up this optimization?
    private static final boolean s_isKnownASCII;

    static {
        String charsetName = java.nio.charset.Charset.defaultCharset().name();
        s_isKnownASCII = ASCII_CHARSET_NAME.equals(charsetName) ||
            ISO_LATIN_CHARSET_NAME.equals(charsetName);
    }

    private byte[] m_bytes;
    private int m_pos;


    /**
     * Construct a new instance with the default
     * initial capacity.
     */
    public ASCIIStringBuilder() {
        this(16);//Magic number 16 taken from Java sources
    }

    /**
     * Construct a new instance with the
     * given initial internal capacity.
     */
    public ASCIIStringBuilder(int initCapacity) {
        m_bytes = new byte[initCapacity];
    }


    /**
     * Construct a new instance with the
     * given String.  The caveats for the
     * String in the {@link #append(java.lang.String) append(String)}
     * method apply to this constructor.
     *
     * @param str initial string
     *
     */
    public ASCIIStringBuilder(String str) {
        m_bytes = new byte[str.length() + 16];
        append(str);
    }

    /**
     *
     * Construct a new instance with the
     * given bytes appended.
     *
     * @param bytes initial bytes
     * @param start the starting offset
     * @param len the length
     *
     */
    public ASCIIStringBuilder(byte[] bytes,
                              int start,
                              int len) {
        m_bytes = new byte[len + 16];
        append(bytes, start, len);
    }

    /**
     * Construct a new instance with the
     * given bytes appended.
     *
     * @param bytes initial bytes
     *
     */
    public ASCIIStringBuilder(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Append the given String, grabbing its
     * bytes in the default charset.  Note that
     * no checking is performed on the bytes to ensure
     * their correctness in ASCII.
     *
     * @param str the String
     *
     * @return this
     */
    public ASCIIStringBuilder append(String str) {
        byte[] bytes = null;
        if(str == null) {
            bytes = NULL_BYTES;
        }
        else {
            bytes = str.getBytes();
        }
        return append(bytes, 0, bytes.length);
    }

    /**
     * Append the given bytes
     *
     * @param bytes the bytes
     *
     * @return this
     */
    public ASCIIStringBuilder append(byte[] bytes) {
        return append(bytes, 0, bytes.length);
    }

    /**
     * Append the given bytes
     *
     * @param bytes the bytes
     * @param start start
     * @param len length
     *
     * @return this
     */
    public ASCIIStringBuilder append(byte[] bytes,
                                     int start,
                                     int len) {
        ensure(len);
        System.arraycopy(bytes, start, m_bytes, m_pos, len);
        m_pos+=len;
        return this;
    }

    /**
     * Append the given byte
     *
     * @param b the byte to append.
     *
     * @return this
     */
    public ASCIIStringBuilder append(byte b) {
        ensure(1);
        m_bytes[m_pos++] = b;
        return this;
    }

    /**
     * Append the given char (cast to a byte)
     *
     * @param c the char to append.
     *
     * @return this
     */
    public ASCIIStringBuilder append(char c) {
        return append((byte) c);
    }

    /**
     * The current size of the internal array.  Will be the size
     * of any returned String
     */
    public int size() {
        return m_pos;
    }

    /**
     * ...same as {@link #size size()}.
     */
    public int length() {
        return size();
    }

    /**
     * Converts the internal bytes to a String in the ASCII
     * characterset.
     * <p>
     * @return a new String
     */
    public String toString() {
        if(s_isKnownASCII) {
            return new String(m_bytes, 0, m_pos);
        }
        else {
            //This is nasty.  The Docs for Java declare that
            //"ISO-8859-1" must always be supported on
            //all platforms, yet I must call this method which
            //declares an exception.  I think we should take the
            //risk
            try {
                return new String(m_bytes, 0, m_pos, ISO_LATIN_CHARSET_NAME);
            }
            catch(java.io.UnsupportedEncodingException ex) {
                //Now what!  Sun lied!  However, I believe most charsets
                //use ASCII as their lower characters anyway...
                return new String(m_bytes, 0, m_pos);
            }
        }
    }

    private void ensure(final int len) {
        if(m_pos + len > m_bytes.length) {
            int newLen = (m_bytes.length + 1)*2;

            //Check edge case where the "normal" expansion
            //is not enough
            if(newLen < (m_pos + len)) {
                newLen = m_pos + len;
            }
            byte[] newBytes = new byte[newLen];
            System.arraycopy(m_bytes, 0, newBytes, 0, m_pos);
            m_bytes = newBytes;
        }
    }

}
