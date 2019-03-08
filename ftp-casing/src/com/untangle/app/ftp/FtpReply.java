/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.util.AsciiCharBuffer;

/**
 * FTP server reply to a command.
 * @see "RFC 959, Section 4.2"
 */
public class FtpReply implements Token
{
    private static final Pattern LINE_SPLITTER = Pattern.compile("\r\n");

    private final int replyCode;
    private final String message;

    public final static int PASV = 227;
    public final static int EPSV = 229;

    /**
     * Create an FtpReply Token
     * @param replyCode
     * @param message
     */
    public FtpReply(int replyCode, String message)
    {
        this.replyCode = replyCode;
        this.message = message;
    }

    /**
     * Make an FtpReply
     * @param replyCode
     * @param message
     * @return the FtpReply
     */
    public static FtpReply makeReply(int replyCode, String message)
    {
        String[] lines = LINE_SPLITTER.split(message);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < lines.length; i++) {
            sb.append(replyCode);
            if (lines.length - 1 == i) {
                sb.append(' ');
            } else {
                sb.append('-');
            }
            sb.append(lines[i]);
            sb.append("\r\n");
        }

        return new FtpReply(replyCode, sb.toString());
    }

    /**
     * make a PASV FtpReply for the specified socketAddress
     * @param socketAddress
     * @return FtpReply
     */
    public static FtpReply pasvReply(InetSocketAddress socketAddress)
    {
        String msg = "Entering Passive Mode ("
            + FtpUtil.unparsePort(socketAddress) + ").";

        return makeReply(PASV, msg);
    }

    /**
     * getSocketAddress for this FtpReply (If PASV or EPSV)
     * @return the socket address for PASV or EPSV or null
     */
    public InetSocketAddress getSocketAddress()
    {
        switch(replyCode) {
        case PASV:
            /* fallthrough */
        case EPSV:
            {
                int b = message.indexOf('(');
                int e = message.indexOf(')', b);
                if ((b < 0) || (e < 0)) 
                    throw new RuntimeException("Missing parenthesis in passive command.");
                String addrStr = message.substring(b + 1, e);
                if (PASV  == replyCode) {
                    return FtpUtil.parsePort(addrStr);
                } else {
                    return FtpUtil.parseExtendedPasvReply(addrStr);
                }
            }
        default:
            return null;
        }
    }

    /**
     * getReplyCode for this FtpReply
     * @return the reply code
     */
    public int getReplyCode()
    {
        return replyCode;
    }

    /**
     * getMessage for this FtpReply
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Includes final CRLF.
     * @return the ftp reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        return ByteBuffer.wrap(message.getBytes());
    }

    /**
     * the ascii string equivalent of this FtpReply
     * @return the ascii string
     */
    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }
}
