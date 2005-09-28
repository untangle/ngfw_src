/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.util;

public class AsciiString implements CharSequence
{
    private final byte[] val;
    private final int offset;
    private final int length;

    public AsciiString(byte[] val)
    {
        this.offset = 0;
        this.length = val.length;
        this.val = new byte[length];
        System.arraycopy(val, 0, this.val, 0, val.length);
    }

    // for subsequence
    private AsciiString(byte[] val, int offset, int length)
    {
        this.val = val;
        this.offset = offset;
        this.length = length;
    }

    public char charAt(int i)
    {
        return (char)val[offset + i];
    }

    public int length()
    {
        return length;
    }

    public CharSequence subSequence(int start, int end)
    {
        return new AsciiString(val, offset + start, end - start);
    }

    public String toString()
    {
        return new String(val, offset, length);
    }
}
