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
import static com.metavize.tran.util.Rfc822Util.*;

import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

/**
 * POP server reply to a command.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see RFC1939
 */
public class PopReply implements Token
{
    private final static Logger logger = Logger.getLogger(PopReply.class);

    private final String reply;
    private final String argument;

    public PopReply(String reply, String argument)
    {
        this.reply = reply;
        this.argument = argument;
    }

    // static factories ------------------------------------------------------

    public static PopReply parse(ByteBuffer buf) throws ParseException
    {
        logger.debug("parse reply");

        String reply = consumeToken(buf);
        if (0 == reply.length()) {
            throw new ParseException("no reply found");
        }

        eatSpace(buf);
        String arg = consumeLine(buf); /* eat CRLF */
        return new PopReply(reply, (0 == arg.length()) ? null : arg);
    }

    // bean methods -----------------------------------------------------------

    public String getReply()
    {
        return reply;
    }

    public String getArgument()
    {
        return argument;
    }

    // Token methods ---------------------------------------------------------

    /**
     * Reassemble reply.
     * Includes final CRLF.
     *
     * @return the POP reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        int len = reply.length() + (null == argument ? 0 : argument.length() + 1) + 2;
        ByteBuffer buf = ByteBuffer.allocate(len);

        buf.put(reply.getBytes());
        if (null != argument)
        {
            buf.put((byte)SP); /* restore */
            buf.put(argument.getBytes());
        }
        buf.put((byte)CR); /* restore */
        buf.put((byte)LF); /* restore */

        buf.flip();

        return buf;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }

    // private methods --------------------------------------------------------
}
