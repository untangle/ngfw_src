/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.untangle.node.token.ParseException;

/**
 * Utilities for the FTP protocol.
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

            return new InetSocketAddress( InetAddress.getByName(matcher.group(2)), Integer.valueOf(matcher.group(3)) );
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
