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
import com.untangle.node.ftp.FtpStateMachine;
import com.untangle.node.token.ParseException;
import com.untangle.node.token.Token;
import com.untangle.node.token.TokenException;
import com.untangle.node.token.TokenResult;

/**
 * This handles FTP and inserts the necessary port forwards and rewrites the PORT/PASV commands so that the necessary connection can be made
 */
class RouterFtpHandler extends FtpStateMachine
{
    private final Logger logger = Logger.getLogger( this.getClass());
    private final RouterImpl node;
    private final RouterSessionManager sessionManager;
    private RouterSessionData sessionData;

    private boolean receivedPortCommand                = false;
    private SessionRedirect portCommandSessionRedirect = null;
    private SessionRedirectKey portCommandKey          = null;

    private static final TokenResult SYNTAX_REPLY = new TokenResult( new Token[] { FtpReply.makeReply( 501, "Syntax error in parameters or arguments") }, null );

    RouterFtpHandler( NodeTCPSession session, RouterImpl node )
    {
        super(session);

        this.node = node;
        this.sessionManager = node.getSessionManager();

        /* Lazily set session data since the other event handler can
         * run before or after this event handler */
        this.sessionData = null;
    }

    @Override
    protected TokenResult doCommand( FtpCommand command ) throws TokenException
    {
        FtpFunction function = command.getFunction();

        if ( !updateSessionData()) {
            if (logger.isDebugEnabled()) {
                logger.debug( "doCommand: Ignoring unmodified session" );
            }
            return new TokenResult( null, new Token[] { command } );
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Received command: " + function );
        }

        /* Ignore the previous port command */
        receivedPortCommand        = false;
        portCommandSessionRedirect = null;
        portCommandKey             = null;

        if ( function == FtpFunction.PASV ) {
            return pasvCommand( command );
        } else if ( function == FtpFunction.PORT ) {
            return portCommand( command );
        } else if ( function == FtpFunction.EPSV ) { 
            return epsvCommand( command );
        } else if ( function == FtpFunction.EPRT ) {
            return eprtCommand( command );
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Passing command: " + function );
        }
        return new TokenResult( null, new Token[] { command } );
    }

    @Override
    protected TokenResult doReply( FtpReply reply ) throws TokenException
    {
        int replyCode = reply.getReplyCode();

        if ( !updateSessionData()) {
            logger.debug( "doReply: Ignoring unmodified session" );
            return new TokenResult( new Token[] { reply }, null );
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Received reply: " + reply );
        }

        if ( receivedPortCommand && replyCode == 200 && portCommandKey != null &&
             portCommandSessionRedirect != null ) {
            /* Now enable the session redirect */
            sessionManager.registerSessionRedirect( sessionData, portCommandKey,
                                                    portCommandSessionRedirect );
        } else {
            switch ( replyCode ) {
            case FtpReply.PASV:
                return pasvReply( reply );

            case FtpReply.EPSV:
                return epsvReply( reply );

            default:
            }
        }

        receivedPortCommand = false;
        portCommandSessionRedirect = null;
        portCommandKey             = null;

        if (logger.isDebugEnabled()) {
            logger.debug( "Passing reply: " + reply );
        }
        return new TokenResult( new Token[] { reply }, null );
    }

    @Override
    protected void doClientDataEnd() throws TokenException
    {
        getSession().shutdownServer();
    }

    @Override
    protected void doServerDataEnd() throws TokenException
    {
        getSession().shutdownClient();
    }

    private TokenResult portCommand( FtpCommand command ) throws TokenException
    {
        return handlePortCommand( command );
    }

    /* Handle a port command, this is the helper for both extended and normal commands */
    private TokenResult handlePortCommand( FtpCommand command ) throws TokenException
    {
        InetSocketAddress addr;

        if ( !updateSessionData()) {
            logger.debug( "hanglePortCommand: Ignoring unmodified session" );
            return new TokenResult( null, new Token[] { command } );
        }

        try {
            addr = command.getSocketAddress();
        } catch ( ParseException e ) {
            logger.info( "Error parsing port command" + e );
            return SYNTAX_REPLY;
        }

        if ( addr == null ) {
            logger.info( "Error parsing port command(null socketaddress)" );
            return SYNTAX_REPLY;
        }

        if (addr.getAddress() == null ) {
            logger.warn( "Error parsing port command(null ip address)" );
            return SYNTAX_REPLY;
        }

        /**
         * Indicate that a port command has been received, don't create the session redirect until
         * the server accepts the port command
         */
        receivedPortCommand        = true;
        portCommandSessionRedirect = null;
        portCommandKey             = null;

        if ( sessionData.isClientRedirect()) {
            int port = node.getHandler().getNextPort( Protocol.TCP );
            /* 1. Tell the event handler to redirect the session from the server. *
             * 2. Mangle the command.                                             */
            portCommandKey = new SessionRedirectKey( Protocol.TCP, sessionData.modifiedClientAddr(), port );

            /* Queue the message for when the port command reply comes back, and make sure to free
             * the necessary port */
            portCommandSessionRedirect = new SessionRedirect( sessionData.originalServerAddr(), 0,
                                                              sessionData.originalClientAddr(), addr.getPort(),
                                                              port, sessionData.modifiedClientAddr(),
                                                              portCommandKey );

            addr = new InetSocketAddress( sessionData.modifiedClientAddr(), port );
            if (logger.isDebugEnabled()) {
                logger.debug( "Mangling PORT command to address: " + addr );
            }

            FtpFunction function = command.getFunction();

            if ( FtpFunction.EPRT.equals( function )) {
                command = FtpCommand.extendedPortCommand( addr );
            } else if ( FtpFunction.PORT.equals( function )) {
                command = FtpCommand.portCommand( addr );
            } else {
                logger.error( "Unkown port command: " + function );
                return SYNTAX_REPLY;
            }
        } else if ( sessionData.isServerRedirect()) {
            /* 1. Tell the event handler to redirect the session from the server. */
            //////////////////////////sessionManager.redirectServerSession( sessionData, session );
        }

        return new TokenResult( null, new Token[] { command } );
    }

    private TokenResult eprtCommand( FtpCommand command ) throws TokenException
    {
        logger.debug( "Handling extended port command" );
        return handlePortCommand( command );
    }
    
    private TokenResult pasvCommand( FtpCommand command ) throws TokenException
    {
        return new TokenResult( null, new Token[] { command } );
    }

    private TokenResult pasvReply( FtpReply reply ) throws TokenException
    {
        InetSocketAddress addr;

        if ( !updateSessionData()) {
            logger.debug( "Ignoring unmodified session" );
            return new TokenResult( new Token[] { reply }, null );
        }

        try {
            addr = reply.getSocketAddress();
        } catch ( ParseException e ) {
            throw new TokenException( "Error getting socket address", e );
        }

        if ( null == addr ) {
            throw new TokenException( "Error getting socket address" );
        }

        /* Verify that the server is going to the same place */
        if (addr.getAddress() == null ) {
            throw new TokenException( "wildcard address" );
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
        return new TokenResult( new Token[] { reply }, null );
    }

    private TokenResult epsvCommand( FtpCommand command ) throws TokenException
    {
        return new TokenResult( null, new Token[] { command } );
    }

    private TokenResult epsvReply( FtpReply reply ) throws TokenException
    {
        InetSocketAddress addr;
        try {
            addr = reply.getSocketAddress();
        } catch ( ParseException e ) {
            throw new TokenException( "Error getting socket address", e );
        }

        if ( null == addr ) {
            throw new TokenException( "Error getting socket address" );
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
        return new TokenResult( new Token[] { FtpEpsvReply.makeEpsvReply( addr ) }, null );
    }

    private boolean updateSessionData()
    {
        if ( sessionData == null ) {
            /* Get the information the nat node is tracking about the session */
            sessionData = sessionManager.getSessionData( getSession());
        }

        return ( sessionData != null );
    }
}

