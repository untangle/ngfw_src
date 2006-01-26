/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
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

    /**
     * Creates a new <code>FtpCommand</code> instance.
     *
     * @param function the FTP function. null if empty line.
     * @param argument function arguments. null if none, or empty line.
     */
    public FtpCommand(FtpFunction function, String argument)
    {
        this.function = function;
        this.argument = argument;
    }

    // business methods -------------------------------------------------------

    public InetSocketAddress getSocketAddress() throws ParseException
    {
        if (FtpFunction.PORT == function) {
            return FtpUtil.parsePort(argument);
        } else {
            return null;
        }
    }

    // static factories -------------------------------------------------------

    public static FtpCommand portCommand(InetSocketAddress socketAddress)
    {
        String cmd = FtpUtil.unparsePort(socketAddress);
        return new FtpCommand(FtpFunction.PORT, cmd);
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
        String cmd = null == function ? "" : function.toString();

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

    public int getEstimatedSize()
    {
        return function.toString().length() + argument.length();
    }
}
