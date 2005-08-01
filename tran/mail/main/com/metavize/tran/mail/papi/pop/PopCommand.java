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

    public static PopCommand parse(ByteBuffer buf) throws ParseException
    {
        String cmd = consumeToken(buf);
        if (0 == cmd.length()) {
            throw new ParseException("no command found");
        }

        eatSpace(buf);
        String arg = consumeLine(buf); /* eat CRLF */
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
        buf.put((byte)CR);
        buf.put((byte)LF);

        buf.flip();

        return buf;
    }
}
