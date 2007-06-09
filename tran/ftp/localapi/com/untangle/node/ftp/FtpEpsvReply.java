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

package com.untangle.tran.ftp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.tran.token.ParseException;
import com.untangle.tran.token.Token;
import com.untangle.tran.util.AsciiCharBuffer;

/**
 * FTP server reply to a command.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC , Section 4.2"
 */
public class FtpEpsvReply extends FtpReply
{
    private final InetSocketAddress socketAddress;
    
    private static final Pattern LINE_SPLITTER = Pattern.compile("\r\n");

    private FtpEpsvReply(String message, InetSocketAddress socketAddress)
    {
        super(FtpReply.EPSV,message);
        this.socketAddress = socketAddress;
    }
    
    public static FtpReply makeEpsvReply(InetSocketAddress socketAddress)
    {
        String message = "Entering Extended Passive Mode ("
            + FtpUtil.unparseExtendedPasvReply(socketAddress) + ").";

        String[] lines = LINE_SPLITTER.split(message);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            sb.append(FtpReply.EPSV);
            if (lines.length - 1 == i) {
                sb.append(' ');
            } else {
                sb.append('-');
            }
            sb.append(lines[i]);
            sb.append("\r\n");
        }

        return new FtpEpsvReply(sb.toString(), socketAddress);
    }

    public InetSocketAddress getSocketAddress() throws ParseException
    {
        return socketAddress;
    }
}
