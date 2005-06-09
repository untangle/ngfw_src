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

import static com.metavize.tran.util.Rfc822Util.*;
import static com.metavize.tran.util.Ascii.*;

import java.nio.ByteBuffer;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;

public class SmtpCommand implements Token
{
    private final String command;
    private final String argument;

    // constructors -----------------------------------------------------------

    private SmtpCommand(String command, String argument)
    {
        this.command = command;
        this.argument = argument;
    }

    // static factories -------------------------------------------------------

    static SmtpCommand parse(ByteBuffer buf) throws ParseException
    {
        String cmd = consumeToken(buf);
        eatSpace(buf);
        String arg = consumeLine(buf);
        return new SmtpCommand(cmd, 0 == arg.length() ? null : arg);
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
        int l = command.length() + (null == argument ? 0 : argument.length())
            + 3;
        ByteBuffer buf = ByteBuffer.allocate(l);

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
