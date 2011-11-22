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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.util.AsciiCharBuffer;

/**
 * FTP command.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see "RFC 959, Section 4"
 */
public class FtpCommand implements Token
{
    private final FtpFunction function;
    private final String argument;

    // constructors -----------------------------------------------------------

    /**
     * Creates a new <code>FtpCommand</code> instance.
     *
     * @param function the FTP function. null if empty line.
     * @param argument function arguments. null if none, or empty line.
     */
    public FtpCommand(FtpFunction function, String argument)
    {
        this.function = function;
        this.argument = argument;
    }

    // business methods -------------------------------------------------------

    public InetSocketAddress getSocketAddress() throws ParseException
    {
        if (FtpFunction.PORT == function) {
            return FtpUtil.parsePort(argument);
        } else if (FtpFunction.EPRT==function) {
            return FtpUtil.parseExtendedPort(argument);
        } else {

            return null;
        }
    }

    // static factories -------------------------------------------------------

    public static FtpCommand portCommand(InetSocketAddress socketAddress)
    {
        String cmd = FtpUtil.unparsePort(socketAddress);
        return new FtpCommand(FtpFunction.PORT, cmd);
    }

    public static FtpCommand extendedPortCommand(InetSocketAddress socketAddress)
    {
        String cmd = FtpUtil.unparseExtendedPort(socketAddress);
        return new FtpCommand(FtpFunction.EPRT, cmd);
    }



    // bean methods -----------------------------------------------------------

    public FtpFunction getFunction()
    {
        return function;
    }

    public String getArgument()
    {
        return argument;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        String cmd = null == function ? "" : function.toString();

        int l = cmd.length() + 2 + (null == argument ? 0 : argument.length());

        StringBuffer sb = new StringBuffer(l);
        sb.append(cmd);
        if (null != argument) {
            sb.append(' ');
            sb.append(argument);
        }
        sb.append("\r\n");
        return ByteBuffer.wrap(sb.toString().getBytes());
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return AsciiCharBuffer.wrap(getBytes()).toString();
    }

    public int getEstimatedSize()
    {
        return (null == function ? 0 : function.toString().length())
            + (null == argument ? 0 : argument.length());
    }
}
