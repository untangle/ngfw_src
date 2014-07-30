/**
 * $Id$
 */
package com.untangle.node.router;

import java.net.InetSocketAddress;
import org.apache.log4j.Logger;

import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeTCPSession;
import com.untangle.node.ftp.FtpCommand;
import com.untangle.node.ftp.FtpEpsvReply;
import com.untangle.node.ftp.FtpFunction;
import com.untangle.node.ftp.FtpReply;
import com.untangle.node.ftp.FtpEventHandler;
import com.untangle.node.token.Token;

/**
 * This handles FTP and inserts the necessary port forwards and rewrites the PORT/PASV commands so that the necessary connection can be made
 */
class RouterFtpHandler extends FtpEventHandler
{
    private final Logger logger = Logger.getLogger( this.getClass());
    private final RouterImpl node;
    private final RouterSessionManager sessionManager;

    private final static int portStart = 10000 + (int)(Math.random()*3000);
    private final static int portRange = 5000;
    private static int portCurrent = portStart;
    
    private static final Token SYNTAX_REPLY = FtpReply.makeReply( 501, "Syntax error in parameters or arguments");

    RouterFtpHandler( RouterImpl node )
    {
        this.node = node;
        this.sessionManager = node.getSessionManager();
    }

    @Override
    public void handleTCPNewSession( NodeTCPSession session )
    {
        /**
         * Remove redirect if this session is the redirected session
         */
        if (node.getSessionManager().isSessionRedirect(session,Protocol.TCP,node)){
            if ( logger.isDebugEnabled()) {
                logger.debug( "Found a redirected session");
            }
        }

        /**
         * If its the control session register
         */
        if (session.getNewServerPort() == 21) {
            node.getSessionManager().registerSession( session );
        }
        
    }

    @Override
    protected void doCommand( NodeTCPSession session, FtpCommand command )
    {
        FtpFunction function = command.getFunction();

        RouterSessionData sessionData = getSessionData( session );
        if ( sessionData == null ) {
            if (logger.isDebugEnabled()) {
                logger.debug( "doCommand: Ignoring unmodified session" );
            }
            session.sendObjectToServer( command );
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Received command: " + function );
        }

        /* Ignore the previous port command */
        sessionData.receivedPortCommand        = false;
        sessionData.portCommandSessionRedirect = null;
        sessionData.portCommandKey             = null;

        if ( function == FtpFunction.PASV ) {
            pasvCommand( session, command );
            return;
        } else if ( function == FtpFunction.PORT ) {
            portCommand( session, command );
            return;
        } else if ( function == FtpFunction.EPSV ) { 
            epsvCommand( session, command );
            return;
        } else if ( function == FtpFunction.EPRT ) {
            eprtCommand( session, command );
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Passing command: " + function );
        }

        session.sendObjectToServer( command );
        return;
    }

    @Override
    protected void doReply( NodeTCPSession session, FtpReply reply )
    {
        int replyCode = reply.getReplyCode();

        RouterSessionData sessionData = getSessionData( session );
        if ( sessionData == null ) {
            logger.debug( "doReply: Ignoring unmodified session" );
            session.sendObjectToClient( reply );
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Received reply: " + reply );
        }

        if ( sessionData.receivedPortCommand &&
             replyCode == 200 &&
             sessionData.portCommandKey != null &&
             sessionData.portCommandSessionRedirect != null ) {
            /* Now enable the session redirect */
            sessionManager.registerSessionRedirect( sessionData, sessionData.portCommandKey, sessionData.portCommandSessionRedirect );
        } else {
            switch ( replyCode ) {
            case FtpReply.PASV:
                pasvReply( session, reply );
                return;

            case FtpReply.EPSV:
                epsvReply( session, reply );
                return;

            default:
            }
        }

        sessionData.receivedPortCommand = false;
        sessionData.portCommandSessionRedirect = null;
        sessionData.portCommandKey             = null;

        if (logger.isDebugEnabled()) {
            logger.debug( "Passing reply: " + reply );
        }
        session.sendObjectToClient( reply );
        return;
    }

    @Override
    protected void doClientDataEnd( NodeTCPSession session )
    {
        session.shutdownServer();
    }

    @Override
    protected void doServerDataEnd( NodeTCPSession session )
    {
        session.shutdownClient();
    }

    @Override
    public void handleTCPFinalized( NodeTCPSession session )
    {
        node.getSessionManager().releaseSession( session );
    }

    private void portCommand( NodeTCPSession session, FtpCommand command )
    {
        handlePortCommand( session, command );
    }

    /* Handle a port command, this is the helper for both extended and normal commands */
    private void handlePortCommand( NodeTCPSession session, FtpCommand command )
    {
        InetSocketAddress addr;

        RouterSessionData sessionData = getSessionData( session );
        if ( sessionData == null ) {
            logger.debug( "hanglePortCommand: Ignoring unmodified session" );
            session.sendObjectToServer( command );
            return;
        }

        try {
            addr = command.getSocketAddress();
        } catch ( Exception e ) {
            logger.info( "Error parsing port command" + e );
            session.sendObjectToClient( SYNTAX_REPLY );
            return;
        }

        if ( addr == null ) {
            logger.info( "Error parsing port command(null socketaddress)" );
            session.sendObjectToClient( SYNTAX_REPLY );
            return;
        }

        if (addr.getAddress() == null ) {
            logger.warn( "Error parsing port command(null ip address)" );
            session.sendObjectToClient( SYNTAX_REPLY );
            return;
        }

        /**
         * Indicate that a port command has been received, don't create the session redirect until
         * the server accepts the port command
         */
        sessionData.receivedPortCommand        = true;
        sessionData.portCommandSessionRedirect = null;
        sessionData.portCommandKey             = null;

        if ( sessionData.isClientRedirect()) {
            int port = getNextPort( );
            /* 1. Tell the event handler to redirect the session from the server. *
             * 2. Mangle the command.                                             */
            sessionData.portCommandKey = new SessionRedirectKey( Protocol.TCP, sessionData.modifiedClientAddr(), port );

            /* Queue the message for when the port command reply comes back, and make sure to free
             * the necessary port */
            sessionData.portCommandSessionRedirect = new SessionRedirect( sessionData.originalServerAddr(), 0,
                                                              sessionData.originalClientAddr(), addr.getPort(),
                                                              port, sessionData.modifiedClientAddr(),
                                                              sessionData.portCommandKey );

            addr = new InetSocketAddress( sessionData.modifiedClientAddr(), port );
            if (logger.isDebugEnabled()) {
                logger.debug( "Mangling PORT command to address: " + addr );
            }

            FtpFunction function = command.getFunction();

            logger.info("Rewriting FTP PORT command original: " + command );

            if ( FtpFunction.EPRT.equals( function )) {
                command = FtpCommand.extendedPortCommand( addr );
            } else if ( FtpFunction.PORT.equals( function )) {
                command = FtpCommand.portCommand( addr );
            } else {
                logger.error( "Unkown port command: " + function );
                session.sendObjectToClient( SYNTAX_REPLY );
                return;
            }
            logger.info("Rewriting FTP PORT command      new: " + command );

        } else if ( sessionData.isServerRedirect()) {
            /* 1. Tell the event handler to redirect the session from the server. */
            //////////////////////////sessionManager.redirectServerSession( sessionData, session );
        }

        session.sendObjectToServer( command );
        return;
    }

    private void eprtCommand( NodeTCPSession session, FtpCommand command )
    {
        logger.debug( "Handling extended port command" );
        handlePortCommand( session, command );
    }
    
    private void pasvCommand( NodeTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    private void pasvReply( NodeTCPSession session, FtpReply reply )
    {
        InetSocketAddress addr;

        RouterSessionData sessionData = getSessionData( session );
        if ( sessionData == null ) {
            logger.debug( "Ignoring unmodified session" );
            session.sendObjectToClient( reply );
            return;
        }

        try {
            addr = reply.getSocketAddress();
        } catch ( Exception e ) {
            throw new RuntimeException( "Error getting socket address", e );
        }

        if ( null == addr ) {
            throw new RuntimeException( "Error getting socket address" );
        }

        /* Verify that the server is going to the same place */
        if (addr.getAddress() == null ) {
            throw new RuntimeException( "wildcard address" );
        }

        if ( sessionData.isServerRedirect()) {
            /* Modify the response, this must contain the original address the client thinks it
             * is connecting to. */
            addr = new InetSocketAddress( sessionData.originalServerAddr(), addr.getPort());

            if (logger.isDebugEnabled()) {
                logger.debug( "Mangling PASV reply to address: " + addr );
            }

            /* Modify the reply to the client */
            reply = FtpReply.pasvReply( addr );
        } else {
            /* nothing to do */
        }

        /**
         * Reply doesn't have to be modified, but the redirect.
         * If we ever support source redirects besides NAT. this will have to be updated
         */
        session.sendObjectToClient( reply );
        return;
    }

    private void epsvCommand( NodeTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    private void epsvReply( NodeTCPSession session, FtpReply reply )
    {
        RouterSessionData sessionData = getSessionData( session );
        if ( sessionData == null ) {
            logger.debug( "Ignoring unmodified session" );
            throw new RuntimeException( "Missing session data");
        }
        
        InetSocketAddress addr;
        try {
            addr = reply.getSocketAddress();
        } catch ( Exception e ) {
            throw new RuntimeException( "Error getting socket address", e );
        }

        if ( null == addr ) {
            throw new RuntimeException( "Error getting socket address" );
        }

        /**
         * Create a new socket address with the original server address.
         * extended passive mode doesn't specify the address, so in order to catch
         * the data session, NAT creates a new extended passive reply which keeps a
         * separate copy of the InetSocketAddress.
         */
        addr = new InetSocketAddress( sessionData.originalServerAddr(), addr.getPort());

        /**
         * Nothing has to be done here, the server address isn't sent with extended
         * passive replies, so redirects don't really matter
         */
        session.sendObjectToClient( FtpEpsvReply.makeEpsvReply( addr ) );
        return;
    }

    private RouterSessionData getSessionData( NodeTCPSession session )
    {
        RouterSessionData sessionData = (RouterSessionData) session.attachment();
        
        if ( sessionData == null ) {
            /* Get the information the router node is tracking about the session */
            sessionData = sessionManager.getSessionData( session );
            session.attach( sessionData );
        }

        return sessionData;
    }

    private synchronized int getNextPort()
    {
        int ret = portCurrent;

        portCurrent++;
        if ( portCurrent > portStart + portRange )
            portCurrent = portStart;

        return ret;
    }

}

