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

package com.metavize.tran.mail.papi.pop;

import static com.metavize.tran.util.Ascii.*;
import static com.metavize.tran.util.Rfc822Util.*;

import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.util.AsciiCharBuffer;
import com.metavize.tran.token.Token;

public class PopCommand implements Token
{
    private final String command;
    private final String argument;

    // constructors -----------------------------------------------------------

    private PopCommand(String command, String argument)
    {
        this.command = command;
        this.argument = argument;
    }

    // static factories -------------------------------------------------------

    public static PopCommand parse(ByteBuffer buf, int iEnd) throws ParseException
    {
        ByteBuffer dup = buf.duplicate();
        String cmd = consumeToken(dup);
        if (0 == cmd.length()) {
            throw new ParseException("cannot identify command: " + AsciiCharBuffer.wrap(buf));
        }

        eatSpace(dup);
        String arg = consumeLine(dup, iEnd); /* eat CRLF */
        return new PopCommand(cmd, 0 == arg.length() ? null : arg);
    }

    // accessors --------------------------------------------------------------

    public String getCommand()
    {
        return command;
    }

    public String getArgument()
    {
        return argument;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        int len = command.length() + (null == argument ? 0 : argument.length() + 1) + 2;
        ByteBuffer buf = ByteBuffer.allocate(len);

        buf.put(command.getBytes());
        if (null != argument) {
            buf.put((byte)SP);
            buf.put(argument.getBytes());
        }

        buf.flip();

        return buf;
    }

    /* consume line in buffer (including any terminating CRLF) up to iEnd */
    private static String consumeLine(ByteBuffer zBuf, int iEnd)
    {
        ByteBuffer zDup = zBuf.duplicate();
        zBuf.position(iEnd);
        zDup.limit(iEnd);
        return consumeBuf(zDup);
    }

    /* consume rest of buffer (including any terminating CRLF) */
    private static String consumeBuf(ByteBuffer zBuf)
    {
        StringBuilder zSBuilder = new StringBuilder();
        while (true == zBuf.hasRemaining()) {
            zSBuilder.append((char) zBuf.get());
        }

        return zSBuilder.toString();
    }
}
