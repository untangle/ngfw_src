/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.mail.papi.pop;

import static com.untangle.tran.util.Ascii.*;
import static com.untangle.tran.util.Rfc822Util.*;

import java.nio.ByteBuffer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.untangle.tran.token.ParseException;
import com.untangle.tran.token.Token;
import com.untangle.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

/**
 * POP server reply to a command.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see RFC1939
 */
public class PopReply implements Token
{
    private final Logger logger = Logger.getLogger(getClass());

    private final static String DIGVAL = "(\\p{Digit})++";
    private final static String LWSP = "\\p{Blank}"; /* linear-white-space */
    private final static String SZVAL = DIGVAL + "(" + LWSP + ")+(oct|byt|char).*?";
    private final static String START = "^(" + LWSP + ")*";
    private final static String OK = START + "\\+OK";
    private final static String OKLWSP = OK + "(" + LWSP + ")";
    private final static String EOLINE = CRLF; /* EOLINE */

    private final static String DATAOK = OKLWSP + "+" + SZVAL + ".*?" + EOLINE;
    private final static String PASSWDOK = START + "\\+(" + LWSP + ")+.*?" + EOLINE;
    private final static String ANYOK = OKLWSP + "*.*?" + EOLINE;

    private final static String PEOLINE = EOLINE + "$"; /* protocol EOLINE */
    private final static String OKSIMPLE = OKLWSP + "*" + PEOLINE;

    private final static Pattern DIGVALP = Pattern.compile(DIGVAL);
    private final static Pattern SZVALP = Pattern.compile(SZVAL, Pattern.CASE_INSENSITIVE);
    private final static Pattern OKSIMPLEP = Pattern.compile(OKSIMPLE, Pattern.CASE_INSENSITIVE);

    private final String reply;
    private final String argument;
    private final String zMsgDataSz;
    private final boolean isSimpleOK;
    private final boolean isOK;

    private final boolean hasSpace;

    private boolean isMsgData;
    private boolean isMsgHdrData;

    private PopReply(String reply, String argument, String zMsgDataSz, boolean isSimpleOK, boolean isOK, boolean hasSpace)
    {
        this.reply = reply;
        this.argument = argument;
        this.zMsgDataSz = zMsgDataSz;
        isMsgData = (null == zMsgDataSz) ? false : true;
        isMsgHdrData = false;
        this.isSimpleOK = isSimpleOK;
        this.isOK = isOK;

        this.hasSpace = hasSpace;
    }

    // static factories ------------------------------------------------------

    public static PopReply parse(ByteBuffer buf, int iEnd) throws ParseException
    {
        ByteBuffer zDup = buf.duplicate();
        zDup.limit(iEnd);
        String zTmp = AsciiCharBuffer.wrap(zDup).toString();
        boolean bIsMsgData = zTmp.matches(DATAOK);
        boolean bIsOK = zTmp.matches(ANYOK);
        //logger.debug("reply is message: " + bIsMsgData + ", is OK: " + bIsOK);

        String zMsgDataSz;
        boolean bIsSimpleOK;
        if (false == bIsMsgData) {
            zMsgDataSz = null;
            bIsSimpleOK = OKSIMPLEP.matcher(zTmp).find();
            //logger.debug("reply is simple ok: " + bIsSimpleOK);
        } else {
            zMsgDataSz = parseMsgDataSz(zTmp);
            bIsSimpleOK = false;
            //logger.debug("reply is message, size: " + zMsgDataSz);
        }

        buf.mark(); /* in case we need to reset later */
        String reply = consumeToken(buf);
        if (0 == reply.length()) {
            buf.reset();
            throw new ParseException("cannot identify reply: " + AsciiCharBuffer.wrap(buf));
        }

        boolean space = eatSpace(buf);

        String arg;
        if (true == bIsMsgData ||
            true == bIsOK) {
            arg = consumeLine(buf, iEnd); /* consume up to end of line */
        } else {
            arg = consumeBuf(buf); /* consume rest of buffer */
        }

        //logger.debug("reply arg: " + arg);
        return new PopReply(reply, (0 == arg.length()) ? null : arg, zMsgDataSz, bIsSimpleOK, bIsOK, space);
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

    // RETR reply can include optional octet count
    // so manually set if we get +OK without an octet count
    public void setMsgData(boolean isMsgData)
    {
        this.isMsgData = isMsgData;
        return;
    }

    public boolean isMsgData()
    {
        return isMsgData;
    }

    // TOP reply does not include any special text
    // so manually set if we get +OK
    public void setMsgHdrData(boolean isMsgHdrData)
    {
        this.isMsgHdrData = isMsgHdrData;
        return;
    }

    public boolean isMsgHdrData()
    {
        return isMsgHdrData;
    }

    public boolean isSimpleOK()
    {
        return isSimpleOK;
    }

    public boolean isOK()
    {
        return isOK;
    }

    // Token methods ---------------------------------------------------------

    /**
     * Reassemble reply (with final CRLF).
     *
     * @return the POP reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        return getBytes(reply, argument, hasSpace);
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        String zTmp = AsciiCharBuffer.wrap(getBytes()).toString();
        if (false == zTmp.matches(PASSWDOK))
            {
                return zTmp;
            }

        return "+ (encoded username/password)";
    }

    // private methods --------------------------------------------------------
    private static ByteBuffer getBytes(String zReply, String zArgument, boolean bHasSpace)
    {
        int iLen = zReply.length() + (null == zArgument ? 0 : zArgument.length() + 1);
        ByteBuffer zBuf = ByteBuffer.allocate(iLen);

        zBuf.put(zReply.getBytes());
        if (true == bHasSpace) {
            zBuf.put((byte)SP); /* restore */
        }
        if (null != zArgument) {
            zBuf.put(zArgument.getBytes());
        }

        zBuf.flip();

        return zBuf;
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

    /* parse reply for original message size */
    private static String parseMsgDataSz(String zDataOK)
    {
        Matcher zMatcher = SZVALP.matcher(zDataOK);
        if (false == zMatcher.find()) {
            /* should never occur
             * (if size fragment is missing, then we can not reach here)
             */
            return null;
        }

        String zSzVal = zMatcher.group();
        zMatcher = DIGVALP.matcher(zSzVal);
        if (false == zMatcher.find()) {
            /* should never occur
             * (if size value is missing, then we can not reach here)
             */
            return null;
        }

        return zMatcher.group();
    }

    public int getEstimatedSize()
    {
        return reply.length() + (null == argument ? 0 : argument.length() + 1);
    }
}
