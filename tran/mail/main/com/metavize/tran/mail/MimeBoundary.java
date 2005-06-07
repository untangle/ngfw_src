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

package com.metavize.tran.mail;

import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;

public class MimeBoundary implements Token
{
    private final String delimiter;
    private final boolean last;
    private final String delimiterStr;

    // constructors -----------------------------------------------------------

    public MimeBoundary(String delimiter, boolean last)
    {
        this.delimiter = delimiter;
        this.last = last;

        delimiterStr = "--" + delimiter + (last ? "--" : "") + CRLF;
    }

    // accessors --------------------------------------------------------------

    public String getDelimiter()
    {
        return delimiter;
    }

    public boolean isLast()
    {
        return last;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        ByteBuffer buf = ByteBuffer.allocate(delimiterStr.length());
        buf.put(delimiterStr.getBytes());
        buf.flip();
        return buf;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return delimiter + " " + (last ? "(last)" : "(separator)");
    }
}
