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

package com.metavize.tran.ftp;

import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;

/**
 * FTP command.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4"
 */
public class FtpCommand implements Token
{
    private final FtpFunction function;
    private final String argument;

    // constructors -----------------------------------------------------------

    public FtpCommand(FtpFunction function, String argument)
    {
        this.function = function;
        this.argument = argument;
    }

    // bean methods -----------------------------------------------------------

    public FtpFunction getFunction()
    {
        return function;
    }

    public String getArgument()
    {
        return argument;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        String cmd = function.toString();

        int l = cmd.length() + 2 + (null == argument ? 0 : argument.length());

        StringBuffer sb = new StringBuffer(l);
        sb.append(cmd);
        if (null != argument) {
            sb.append(' ');
            sb.append(argument);
        }
        sb.append("\r\n");
        return ByteBuffer.wrap(sb.toString().getBytes());
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }
}
