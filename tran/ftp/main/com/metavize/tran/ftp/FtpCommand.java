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

/**
 * FTP command.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4"
 */
public class FtpCommand implements Token
{
    private final FtpFunction command;
    private final String argument;

    // constructors -----------------------------------------------------------

    public FtpCommand(FtpFunction command, String argument)
    {
        this.command = command;
        this.argument = argument;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        String cmd = command.toString();

        StringBuffer sb = new StringBuffer(cmd.length() + 2 + argument.length());
        sb.append(cmd);
        sb.append("\r\n");
        sb.append(argument);
        return ByteBuffer.wrap(sb.toString().getBytes());
    }
}
