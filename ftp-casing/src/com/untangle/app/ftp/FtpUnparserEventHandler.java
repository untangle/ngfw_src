/**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.ReleaseToken;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Fitting;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.TCPStreamer;
import com.untangle.uvm.vnet.AbstractEventHandler;

/**
 * Unparser for FTP tokens.
 */
class FtpUnparserEventHandler extends AbstractEventHandler
{
    private static final Logger logger = Logger.getLogger(FtpUnparserEventHandler.class);

    private final boolean clientSide;

    public FtpUnparserEventHandler( boolean clientSide )
    {
        this.clientSide = clientSide;
    }

    @Override
    public void handleTCPClientChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object. ClientSide:" + clientSide);
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPServerChunk( AppTCPSession session, ByteBuffer data )
    {
        logger.warn("Received data when expect object. ClientSide:" + clientSide);
        throw new RuntimeException("Received data when expect object");
    }

    @Override
    public void handleTCPClientObject( AppTCPSession session, Object obj )
    {
        if (clientSide) {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        } else {
            unparse( session, obj, false );
            return;
        }
    }
    
    @Override
    public void handleTCPServerObject( AppTCPSession session, Object obj )
    {
        if (clientSide) {
            unparse( session, obj, true );
            return;
        } else {
            logger.warn("Received object but expected data.");
            throw new RuntimeException("Received object but expected data.");
        }
    }

    @Override
    public void handleTCPClientDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }

    @Override
    public void handleTCPServerDataEnd( AppTCPSession session, ByteBuffer data )
    {
        if ( data.hasRemaining() ) {
            logger.warn("Received data when expect object");
            throw new RuntimeException("Received data when expect object");
        }
    }
    
    @Override
    public void handleTCPClientFIN( AppTCPSession session )
    {
        if (clientSide) {
            // do nothing
        } else {
            session.shutdownServer();
        }
    }

    @Override
    public void handleTCPServerFIN( AppTCPSession session )
    {
        if (clientSide) {
            session.shutdownClient();
        } else {
            // do nothing
        }
    }

    private void unparse( AppTCPSession session, Object obj, boolean s2c )
    {
        Token token = (Token) obj;

        try {
            if (token instanceof ReleaseToken) {
                ReleaseToken release = (ReleaseToken)token;

                session.release();

                return;
            } else {
                unparse( session, token );
                return;
            }
        } catch (Exception exn) {
            logger.error("internal error, closing connection", exn);

            session.resetClient();
            session.resetServer();

            return;
        }
    }

    public void unparse( AppTCPSession session, Token token )
    {
        InetSocketAddress socketAddress = null;
        if (token instanceof FtpReply) { // XXX tacky
            FtpReply reply = (FtpReply)token;
            if (FtpReply.PASV == reply.getReplyCode()) {
                socketAddress = reply.getSocketAddress();
            }

            /* Extended pasv replies don't contain the server address, have to get that
             * from the session.  NAT/Router is the only place that has that information
             * must register the connection there. */
            else if (FtpReply.EPSV == reply.getReplyCode()) {
                socketAddress = reply.getSocketAddress();
                if (null == socketAddress)
                    throw new RuntimeException("unable to get socket address");

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
                socketAddress = cmd.getSocketAddress();
            } else if (FtpFunction.EPRT == cmd.getFunction()) {
                socketAddress = cmd.getSocketAddress();
            }
        }

        if ( socketAddress != null ) {
            /**
             * Tell the PipelineFoundry that connections from this
             * address/port is an FTP_DATA_STREAM.
             * This is so we will get the access to the stream.
             */
            logger.debug("Registering FTP data session hint/expectation: " + socketAddress);
            UvmContextFactory.context().pipelineFoundry().addConnectionFittingHint(socketAddress, Fitting.FTP_DATA_STREAM);
            /**
             * Keep the correlation between the data session and the control session that opened it
             */
            FtpEventHandler.addDataSocket(socketAddress, session.getSessionId());
        }

        if ( clientSide )
            session.sendDataToClient( token.getBytes() );
        else
            session.sendDataToServer( token.getBytes() );
    }

    public void endSession( AppTCPSession session )
    {
        FtpEventHandler.removeDataSockets(session.getSessionId());
        if ( clientSide )
            session.shutdownClient();
        else
            session.shutdownServer();
        return;
    }
}
