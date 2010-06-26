/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.untangle.node.token.ParseException;
import com.untangle.uvm.node.IPaddr;

/**
 * Utilities for the FTP protocol.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class FtpUtil
{
    static final String EPRT_DELIM = "|";
    static final String EXT_IPV4   = "1";
    static final String EXT_IPV6   = "2";

    static final Pattern EXTENDED_PORT_PATTERN;
    static final Pattern EXTENDED_PASV_PATTERN;

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

        int port = 256 * Integer.parseInt(toks[4]) + Integer.parseInt(toks[5]);

        return new InetSocketAddress(addr, port);
    }

    static InetSocketAddress parseExtendedPort(String s) throws ParseException
    {
        Matcher matcher = EXTENDED_PORT_PATTERN.matcher( s );

        try {
            if (!matcher.matches()) {
                throw new ParseException( "Unable to parse extended port command" );
            }

            return new InetSocketAddress(IPaddr.parse(matcher.group(2)).getAddr(),
                                         Integer.valueOf(matcher.group(3)));
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException( "Unable to parse extended port command" );
        }
    }

    static InetSocketAddress parseExtendedPasvReply(String s) throws ParseException
    {
        Matcher matcher = EXTENDED_PASV_PATTERN.matcher(s);

        try {
            if (!matcher.matches()) {
                throw new ParseException("Unable to parse extended pasv reply: '" + s + "'");
            }

            return new InetSocketAddress(Integer.valueOf(matcher.group(2)));
        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("Unable to parse extended pasv reply", e);
        }
    }

    static String unparsePort(InetSocketAddress socketAddress)
    {
        StringBuffer sb = new StringBuffer();
        byte[] addr = socketAddress.getAddress().getAddress();
        for (byte a : addr) {
            sb.append((a) & 0xFF);
            sb.append(',');
        }

        int port = socketAddress.getPort();
        sb.append(port / 256);
        sb.append(',');
        sb.append(port % 256);

        return sb.toString();
    }

    static String unparseExtendedPort(InetSocketAddress socketAddress)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(EPRT_DELIM);
        sb.append(EXT_IPV4);
        sb.append(EPRT_DELIM);
        sb.append(socketAddress.getAddress().getHostAddress());
        sb.append(EPRT_DELIM);
        sb.append(socketAddress.getPort());
        sb.append(EPRT_DELIM);
        return sb.toString();
    }

    static String unparseExtendedPasvReply(InetSocketAddress socketAddress)
    {
        StringBuffer sb = new StringBuffer();
        sb.append(EPRT_DELIM);
        sb.append(EPRT_DELIM);
        sb.append(EPRT_DELIM);
        sb.append(socketAddress.getPort());
        sb.append(EPRT_DELIM);
        return sb.toString();
    }

    static
    {
        Pattern extended = null;
        Pattern extendedPasv = null;

        try {
            extended     = Pattern.compile("([\\x21-\\x7E])" + EXT_IPV4 + "\\1(.+)\\1(.+)\\1");
            extendedPasv = Pattern.compile("([\\x21-\\x7E])\\1\\1(.+)\\1");
        } catch ( PatternSyntaxException e ) {
            System.err.println( "Unable to initialize the extended port patterns: " + e );
            extended = null;
            extendedPasv = null;
        }

        EXTENDED_PORT_PATTERN = extended;
        EXTENDED_PASV_PATTERN = extendedPasv;
    }
}
