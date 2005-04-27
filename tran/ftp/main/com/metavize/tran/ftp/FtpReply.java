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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.StringTokenizer;

import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.util.AsciiCharBuffer;

/**
 * FTP server reply to a command.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4.2"
 */
public class FtpReply implements Token
{
    private final int replyCode;
    private final String message;

    public FtpReply(int replyCode, String message)
    {
        this.replyCode = replyCode;
        this.message = message;
    }

    // business methods ------------------------------------------------------

    public InetSocketAddress getSocketAddress() throws ParseException
    {
        if (227 == replyCode) {
            int b = message.indexOf('(');
            int e = message.indexOf(')', b);
            String addrStr = message.substring(b + 1, e);
            return FtpUtil.parsePort(addrStr);
        } else {
            return null;
        }
    }

    // bean methods -----------------------------------------------------------

    public int getReplyCode()
    {
        return replyCode;
    }

    public String getMessage()
    {
        return message;
    }

    // Token methods ---------------------------------------------------------

    /**
     * Includes final CRLF.
     *
     * @return a <code>ByteBuffer</code> value
     */
    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder(message.length() + 10);
        sb.append(replyCode);
        sb.append(' ');

        StringTokenizer tok = new StringTokenizer(message, "\n");
        String line = tok.nextToken();
        if (tok.hasMoreTokens()) { /* multi-line bracketed */
            sb.append('-').append(line);
            while (tok.hasMoreTokens()) {
                line = tok.nextToken();
                if (tok.hasMoreTokens()) {
                    sb.append(line);
                    sb.append("\r\n");
                } else {  /* last line */
                    sb.append(replyCode);
                    sb.append(' ');
                    sb.append(line);
                }
            }
        }  else { /* single line */
            sb.append(line);
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
