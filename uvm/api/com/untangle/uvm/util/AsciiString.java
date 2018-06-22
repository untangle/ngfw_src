/**
 * $Id$
 */
package com.untangle.uvm.util;

/**
 * Wraps a byte array to create a <code>CharSequence</code>.
 */
public class AsciiString implements CharSequence
{
    private final byte[] val;

    /**
     * Initialize instance of AsciiString
     * @param  val Array of bytes to initialize.
     * @return     Instance of AsciiString.
     */
    public AsciiString(byte[] val)
    {
        this.val = new byte[val.length];
        System.arraycopy(val, 0, this.val, 0, val.length);
    }

    /**
     * Initialize instance of AsciiString via subsequence.
     * @param  val Array of bytes to initialize.
     * @param offset Begining of val position.
     * @param length Length to copy.
     * @return     Instance of AsciiString.
     */
    private AsciiString(byte[] val, int offset, int length)
    {
        this.val = new byte[length];
        System.arraycopy(val, 0, this.val, offset, length);
    }

    /**
     * Return character at position.
     * @param  i integer of position.
     * @return   char value.
     */
    public char charAt(int i)
    {
        return (char)val[i];
    }

    /**
     * Return length of string.
     * @return integer of length.
     */
    public int length()
    {
        return val.length;
    }

    /**
     * Return subsequence of string.
     * @param  start integer of start position.
     * @param  end   integer of end positon.
     * @return       New instance of AsciiString for subsequence.
     */
    public CharSequence subSequence(int start, int end)
    {
        return new AsciiString(val, start, end - start);
    }

    /**
     * Return as string.
     * @return String value.
     */
    public String toString()
    {
        return new String(val);
    }
}
