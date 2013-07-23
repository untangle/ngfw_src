/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.regex.Pattern;

import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;

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

    public FtpReply(int replyCode, String message)
    {
        this.replyCode = replyCode;
        this.message = message;
    }

    // static factories ------------------------------------------------------

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

    public static FtpReply pasvReply(InetSocketAddress socketAddress)
    {
        String msg = "Entering Passive Mode ("
            + FtpUtil.unparsePort(socketAddress) + ").";

        return makeReply(PASV, msg);
    }

    // business methods ------------------------------------------------------

    public InetSocketAddress getSocketAddress() throws ParseException
    {
        switch(replyCode) {
        case PASV:
            /* fallthrough */
        case EPSV:
            {
                int b = message.indexOf('(');
                int e = message.indexOf(')', b);
                if ((b < 0) || (e < 0)) 
                    throw new ParseException("Missing parenthesis in passive command.");
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
     * @return the ftp reply in a ByteBuffer.
     */
    public ByteBuffer getBytes()
    {
        return ByteBuffer.wrap(message.getBytes());
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }

    public int getEstimatedSize()
    {
        return 3 + message.length();
    }
}
