/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

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

    /**
     * Parse the PORT command
     * @param s - the command string
     * @return the addr/port pair
     */
    static InetSocketAddress parsePort(String s)
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
            throw new RuntimeException("bad address");
        }

        int portOctet1 = Integer.parseInt(toks[4]);
        int portOctet2 = Integer.parseInt(toks[5]);
        if((portOctet1 < 0) || (portOctet1 > 255) ||
           (portOctet2 < 0) || (portOctet2 > 255)){
            throw new RuntimeException("bad port");
        }
        int port = 256 * portOctet1 + portOctet2;

        return new InetSocketAddress(addr, port);
    }

    /**
     * Parse the EPRT command
     * @param s - the command string
     * @return the addr/port pair
     */
    static InetSocketAddress parseExtendedPort(String s)
    {
        Matcher matcher = EXTENDED_PORT_PATTERN.matcher( s );

        try {
            if (!matcher.matches()) {
                throw new RuntimeException( "Unable to parse extended port command" );
            }

            return new InetSocketAddress( InetAddress.getByName(matcher.group(2)), Integer.valueOf(matcher.group(3)) );
        } catch (Exception e) {
            throw new RuntimeException( "Unable to parse extended port command" );
        }
    }

    /**
     * Parse the PASV command
     * @param s - the command string
     * @return the inet address
     */
    static InetSocketAddress parseExtendedPasvReply(String s)
    {
        Matcher matcher = EXTENDED_PASV_PATTERN.matcher(s);

        try {
            if (!matcher.matches()) {
                throw new RuntimeException("Unable to parse extended pasv reply: '" + s + "'");
            }

            return new InetSocketAddress(Integer.valueOf(matcher.group(2)));
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse extended pasv reply", e);
        }
    }

    /**
     * Unparse/create a port command from a socket address
     * @param socketAddress
     * @return the command string
     */
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

    /**
     * Unparse/create a port command string from a socket address
     * @param socketAddress
     * @return the command string
     */
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

    /**
     * Unparse/create a pasv reply string from a socket address
     * @param socketAddress
     * @return the reply string
     */
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
