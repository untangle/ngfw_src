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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Pattern;

import com.metavize.tran.token.ParseException;

class FtpUtil
{
    static InetSocketAddress parsePort(String s) throws ParseException
    {
        String[] toks = Pattern.compile(",").split(s);

        byte[] bAddr = new byte[4];
        for (int j = 0; j < 4; j++) {
            bAddr[j] = new Integer(toks[j]).byteValue();
        }

        InetAddress addr = null;
        try {
            addr = InetAddress.getByAddress(bAddr);
        } catch (UnknownHostException exn) {
            throw new ParseException("bad address");
        }

        int port = 256 * Integer.parseInt(toks[4])
            + Integer.parseInt(toks[5]);

        return new InetSocketAddress(addr, port);
    }

    static String unparsePort(InetSocketAddress socketAddress)
    {
        StringBuffer sb = new StringBuffer();
        byte[] addr = socketAddress.getAddress().getAddress();
        for (byte a : addr) {
            sb.append(a);
            sb.append(',');
        }

        int port = socketAddress.getPort();
        sb.append(port / 256);
        sb.append(',');
        sb.append(port % 256);

        return sb.toString();
    }
}
