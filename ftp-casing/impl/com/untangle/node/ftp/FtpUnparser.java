/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseException;
import com.untangle.node.token.UnparseResult;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.TCPSession;
import com.untangle.uvm.vnet.event.TCPStreamer;

/**
 * Unparser for FTP tokens.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class FtpUnparser extends AbstractUnparser
{
    public FtpUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        InetSocketAddress socketAddress = null;
        if (token instanceof FtpReply) { // XXX tacky
            FtpReply reply = (FtpReply)token;
            if (FtpReply.PASV == reply.getReplyCode()) {
                try {
                    socketAddress = reply.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            }

            /* Extended pasv replies don't contain the server address, have to get that
             * from the session.  NAT/Router is the only place that has that information
             * must register the connection there. */
            else if (FtpReply.EPSV == reply.getReplyCode()) {
                try {
                    socketAddress = reply.getSocketAddress();
                    if (null == socketAddress)
                        throw new UnparseException("unable to get socket address");
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }

                /* Nat didn't already rewrite the reply, use the server address */
                InetAddress address = socketAddress.getAddress();
                if ((null == address)||
                    address.getHostAddress().equals("0.0.0.0")) {
                    TCPSession session = getSession();

                    socketAddress = new InetSocketAddress( session.serverAddr(), socketAddress.getPort());
                } /* otherwise use the data from nat */
            }
        } else if (token instanceof FtpCommand) { // XXX tacky
            FtpCommand cmd = (FtpCommand)token;
            if (FtpFunction.PORT == cmd.getFunction()) {
                try {
                    socketAddress = cmd.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            } else if (FtpFunction.EPRT == cmd.getFunction()) {
                try {
                    socketAddress = cmd.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            }
        }

        if (null != socketAddress) {
            LocalUvmContextFactory.context().pipelineFoundry()
                .registerConnection(socketAddress, Fitting.FTP_DATA_STREAM);
        }

        return new UnparseResult(new ByteBuffer[] { token.getBytes() });
    }

    public TCPStreamer endSession() { return null; }
}
