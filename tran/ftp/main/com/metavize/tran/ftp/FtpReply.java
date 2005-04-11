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

/**
 * FTP server reply to a command. As defined in RFC 959 section 4.2.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 */
public class FtpReply implements Token
{
    private final short replyCode;
    private final String message;

    public FtpReply(int replyCode, String message)
    {
        this.replyCode = replyCode;
        this.message = message;
    }

    // accessors -------------------------------------------------------------

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
     * Does not include the final CRLF.
     *
     * @return a <code>ByteBuffer</code> value
     */
    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder(message.length() + 5);
        sb.append(replyCode);

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
                    sb.append('-');
                    sb.append(line);
                }
            }
        }  else { /* single line */
            sb.append(line);
        }

        return sb.toString().getBytes();
    }
}
