/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ftp;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.Fitting;
import com.metavize.mvvm.tapi.TCPSession;
import com.metavize.tran.token.AbstractUnparser;
import com.metavize.tran.token.ParseException;
import com.metavize.tran.token.Token;
import com.metavize.tran.token.TokenStreamer;
import com.metavize.tran.token.UnparseException;
import com.metavize.tran.token.UnparseResult;

class FtpUnparser extends AbstractUnparser
{
    private final byte[] CRLF = new byte[] { 13, 10 };

    public FtpUnparser(TCPSession session, boolean clientSide)
    {
        super(session, clientSide);
    }

    public UnparseResult unparse(Token token) throws UnparseException
    {
        InetSocketAddress socketAddress = null;
        if (token instanceof FtpReply) { // XXX tacky
            FtpReply reply = (FtpReply)token;
            if (227 == reply.getReplyCode()) {
                try {
                    socketAddress = reply.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            }
        } else if (token instanceof FtpCommand) { // XXX tacky
            FtpCommand cmd = (FtpCommand)token;
            if (FtpFunction.PORT == cmd.getFunction()) {
                try {
                    socketAddress = cmd.getSocketAddress();
                } catch (ParseException exn) {
                    throw new UnparseException(exn);
                }
            }
        }

        if (null != socketAddress) {
            MvvmContextFactory.context().pipelineFoundry()
                .registerConnection(socketAddress, Fitting.FTP_DATA_STREAM);
        }

        return new UnparseResult(new ByteBuffer[] { token.getBytes() });
    }

    public TokenStreamer endSession() { return null; }
}
