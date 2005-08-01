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
import java.util.regex.Matcher;
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
    //private final static Logger logger = Logger.getLogger(PopReply.class);

    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String SZVAL = DIGVAL + " octets";
    private final static String OK = "+OK ";
    private final static String OKREPLY = "^\\" + OK;
    public final static String PEOLINE = CRLF; /* protocol EOLINE */

    private final static String DATAOK = OKREPLY + SZVAL + PEOLINE;

    private final static Pattern DIGVALP = Pattern.compile(DIGVAL);
    private final static Pattern SZVALP = Pattern.compile(SZVAL, Pattern.CASE_INSENSITIVE);

    private final String reply;
    private final String argument;
    private final String zMsgDataSz;
    private final boolean bIsMsgData;

    private PopReply(String reply, String argument, String zMsgDataSz, boolean bIsMsgData)
    {
        this.reply = reply;
        this.argument = argument;
        this.zMsgDataSz = zMsgDataSz;
        this.bIsMsgData = bIsMsgData;
    }

    // static factories ------------------------------------------------------

    public static PopReply parse(ByteBuffer buf, int iEnd) throws ParseException
    {
        String zMsgDataSz;

        ByteBuffer dup = buf.duplicate();
        dup.limit(iEnd);
        String zTmp = AsciiCharBuffer.wrap(dup).toString();
        boolean bIsMsgData = zTmp.matches(DATAOK);
        //logger.debug("reply is message: " + bIsMsgData);
        if (false == bIsMsgData)
        {
            zMsgDataSz = null;
        }
        else
        {
            zMsgDataSz = parseMsgDataSz(zTmp);
        }

        String reply = consumeToken(buf);
        if (0 == reply.length()) {
            throw new ParseException("no reply found");
        }

        eatSpace(buf);

        String arg;
        if (false == bIsMsgData)
        {
            arg = consumeBuf(buf);
        }
        else
        {
            arg = consumeLine(buf, iEnd);
        }

        return new PopReply(reply, (0 == arg.length()) ? null : arg, zMsgDataSz, bIsMsgData);
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

    public String getMsgDataSz()
    {
        return zMsgDataSz;
    }

    public boolean isMsgData()
    {
        return bIsMsgData;
    }

    // Token methods ---------------------------------------------------------

    /**
     * Reassemble reply (with final CRLF).
     *
     * @return the POP reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        return getBytes(reply, argument);
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }

    // private methods --------------------------------------------------------
    private static ByteBuffer getBytes(String zReply, String zArgument)
    {
        int len = zReply.length() + (null == zArgument ? 0 : zArgument.length() + 1);
        ByteBuffer buf = ByteBuffer.allocate(len);

        buf.put(zReply.getBytes());
        if (null != zArgument)
        {
            buf.put((byte)SP); /* restore */
            buf.put(zArgument.getBytes());
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
        while (true == zBuf.hasRemaining())
        {
            zSBuilder.append((char) zBuf.get());
        }

        return zSBuilder.toString();
    }

    /* parse reply for original message size */
    private static String parseMsgDataSz(String zDataOK)
    {
        Matcher zMatcher = SZVALP.matcher(zDataOK);
        if (false == zMatcher.find())
        {
            /* should never occur
             * (if size fragment is missing, then we can not reach here)
             */
            return null;
        }

        String zSzVal = zMatcher.group();
        zMatcher = DIGVALP.matcher(zSzVal);
        if (false == zMatcher.find())
        {
            /* should never occur
             * (if size value is missing, then we can not reach here)
             */
            return null;
        }

        return zMatcher.group();
    }
}
