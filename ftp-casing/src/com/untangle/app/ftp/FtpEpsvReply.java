/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetSocketAddress;
import java.util.regex.Pattern;

/**
 * FTP server reply to a command.
 * @see "RFC , Section 4.2"
 */
public class FtpEpsvReply extends FtpReply
{
    private final InetSocketAddress socketAddress;
    
    private static final Pattern LINE_SPLITTER = Pattern.compile("\r\n");

    /**
     * FtpEpsvReply - make a new EPSV reply
     * use makeEpsvReply to make a new FtpEpsvReply
     * @param message
     * @param socketAddress
     */
    private FtpEpsvReply(String message, InetSocketAddress socketAddress)
    {
        super(FtpReply.EPSV,message);
        this.socketAddress = socketAddress;
    }
    
    /**
     * makeEpsvReply - makes an EPSV reply of the provided address
     * @param socketAddress
     * @return FtpReply
     */
    public static FtpReply makeEpsvReply(InetSocketAddress socketAddress)
    {
        String message = "Entering Extended Passive Mode (" + FtpUtil.unparseExtendedPasvReply(socketAddress) + ").";

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

    /**
     * getSocketAddress for this EpsvReply
     * @return the socketAddress
     */
    public InetSocketAddress getSocketAddress()
    {
        return socketAddress;
    }
}
