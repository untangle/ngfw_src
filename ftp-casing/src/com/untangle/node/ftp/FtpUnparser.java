/**
 * $Id$
 */
package com.untangle.node.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.node.token.AbstractUnparser;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.token.UnparseException;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;

/**
 * Unparser for FTP tokens.
 */
class FtpUnparser extends AbstractUnparser
{
    private static final Logger logger = Logger.getLogger(FtpUnparser.class);

    public FtpUnparser( boolean clientSide )
    {
        super( clientSide );
    }

    public void handleNewSession( NodeTCPSession session )
    {
    }
    
    public void unparse( NodeTCPSession session, Token token ) throws UnparseException
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

                    socketAddress = new InetSocketAddress( session.getServerAddr(), socketAddress.getPort());
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

        if ( socketAddress != null ) {
            /**
             * Tell the PipelineFoundry that connections from this
             * address/port is an FTP_DATA_STREAM.
             * This is so we will get the access to the stream.
             */
            UvmContextFactory.context().pipelineFoundry().addConnectionFittingHint(socketAddress, Fitting.FTP_DATA_STREAM);
            /**
             * Keep the correlation between the data session and the control session that opened it
             */
            FtpStateMachine.addDataSocket(socketAddress, session.getSessionId());
        }

        if ( clientSide )
            session.sendDataToClient( token.getBytes() );
        else
            session.sendDataToServer( token.getBytes() );
    }

    public void endSession( NodeTCPSession session )
    {
        FtpStateMachine.removeDataSockets(session.getSessionId());
        if ( clientSide )
            session.shutdownClient();
        else
            session.shutdownServer();
        return;
    }
}
