 /**
 * $Id$
 */
package com.untangle.app.ftp;

import java.net.InetSocketAddress;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.AppTCPSession;
import com.untangle.uvm.vnet.Token;
import com.untangle.uvm.vnet.TCPNewSessionRequest;
import com.untangle.app.ftp.FtpCommand;
import com.untangle.app.ftp.FtpEpsvReply;
import com.untangle.app.ftp.FtpFunction;
import com.untangle.app.ftp.FtpReply;
import com.untangle.app.ftp.FtpEventHandler;

/**
 * This handles FTP and inserts the necessary port forwards and rewrites the PORT/PASV commands so that the necessary connection can be made
 */
class FtpNatHandler extends FtpEventHandler
{
    protected static final Logger logger = Logger.getLogger( FtpNatHandler.class );

    private final static int portStart = 10000 + (int)(Math.random()*3000);
    private final static int portRange = 5000;
    private static int portCurrent = portStart;
    
    private static final Token SYNTAX_REPLY = FtpReply.makeReply( 501, "Syntax error in parameters or arguments");

    private static Map<SessionRedirectKey,SessionRedirect> redirectMap = new ConcurrentHashMap<SessionRedirectKey,SessionRedirect>();
    
    /**
     * FtpNatHandler creates a new FtpNatHandler
     */
    public FtpNatHandler() { }

    /**
     * handleTCPNewSessionRequest
     * @param sessionRequest
     */
    @Override
    public void handleTCPNewSessionRequest( TCPNewSessionRequest sessionRequest )
    {
        /**
         * Look to see if this is an existing TCP connection belonging to an existing FTP session
         */
        if (redirectMap.isEmpty())
            return;

        SessionRedirectKey key = new SessionRedirectKey( Protocol.TCP, sessionRequest.getOrigServerAddr(), sessionRequest.getOrigServerPort() );
        SessionRedirect redirect = redirectMap.remove( key );

        if ( redirect != null ) {
            logger.info( "FTP Data session: " +
                         sessionRequest.getOrigClientAddr().getHostAddress() + ":" + sessionRequest.getOrigClientPort() + " -> " +
                         sessionRequest.getNewServerAddr().getHostAddress() + ":" + sessionRequest.getNewServerPort() );

            /**
             * Now that the session has been created, we can remove the redirect rule
             */
            redirect.cleanup();
        }
    }

    /**
     * doCommand handles the FtpCommand
     * @param session
     * @param command
     */
    @Override
    protected void doCommand( AppTCPSession session, FtpCommand command )
    {
        FtpFunction function = command.getFunction();

        if (logger.isDebugEnabled()) {
            logger.debug( "Received command: " + function );
        }

        SessionState sessionState = getSessionState( session );
        if ( sessionState == null ) {
            if (logger.isDebugEnabled()) {
                logger.debug( "doCommand: Ignoring unmodified session" );
            }
            session.sendObjectToServer( command );
            return;
        }

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

    /**
     * handle the FtpReply
     * @param session
     * @param reply
     */
    @Override
    protected void doReply( AppTCPSession session, FtpReply reply )
    {
        int replyCode = reply.getReplyCode();

        SessionState sessionState = getSessionState( session );
        if ( sessionState == null ) {
            logger.debug( "doReply: Ignoring unmodified session" );
            session.sendObjectToClient( reply );
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Received reply: " + reply );
        }

        switch ( replyCode ) {
        case FtpReply.PASV:
            pasvReply( session, reply );
            return;
        case FtpReply.EPSV:
            epsvReply( session, reply );
            return;
        default:
        }

        if (logger.isDebugEnabled()) {
            logger.debug( "Passing reply: " + reply );
        }
        session.sendObjectToClient( reply );
        return;
    }

    /**
     * doClientDataEnd
     * @param session
     */
    @Override
    protected void doClientDataEnd( AppTCPSession session )
    {
        session.shutdownServer();
    }

    /**
     * doServerDataEnd
     * @param session
     */
    @Override
    protected void doServerDataEnd( AppTCPSession session )
    {
        session.shutdownClient();
    }

    /**
     * handleTCPFinalized
     * @param session
     */
    @Override
    public void handleTCPFinalized( AppTCPSession session )
    {
        SessionState sessionState = getSessionState( session );
        // cleanup all remaining redirects just in case
        if ( sessionState != null ) {
            for ( SessionRedirect redirect : sessionState.redirects ) {
                redirectMap.remove(redirect.key);
                redirect.cleanup();
            }
        }
    }

    /**
     * portCommand handles the PORT command
     * @param session
     * @param command
     */
    private void portCommand( AppTCPSession session, FtpCommand command )
    {
        handlePortCommand( session, command );
    }

    /**
     * handlePortCommand handles the PORT command
     * @param session
     * @param command
     */
    private void handlePortCommand( AppTCPSession session, FtpCommand command )
    {
        InetSocketAddress addr;

        SessionState sessionState = getSessionState( session );
        if ( sessionState == null ) {
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

        /* If the client address or port has been NATd/modified */
        if ( (session.getOrigClientPort() != session.getNewClientPort()) || (!session.getOrigClientAddr().equals(session.getNewClientAddr())) ) {
            int port = getNextPort( );

            /* Insert a new redirect for the expected session */
            SessionRedirect redirect = new SessionRedirect(session.getNewClientAddr(), port, session.getOrigClientAddr(), addr.getPort() );
            insertSessionRedirect( sessionState, redirect );
            
            addr = new InetSocketAddress( session.getNewClientAddr(), port );
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
        } 

        session.sendObjectToServer( command );
        return;
    }

    /**
     * eprtCommand handles the EPRT command
     * @param session
     * @param command
     */
    private void eprtCommand( AppTCPSession session, FtpCommand command )
    {
        logger.debug( "Handling extended port command" );
        handlePortCommand( session, command );
    }
    
    /**
     * pasvCommand handles the PASV command
     * @param session
     * @param command
     */
    private void pasvCommand( AppTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    /**
     * pasvReply handles the PASV reply
     * @param session
     * @param reply
     */
    private void pasvReply( AppTCPSession session, FtpReply reply )
    {
        InetSocketAddress origAddr;

        SessionState sessionState = getSessionState( session );
        if ( sessionState == null ) {
            logger.debug( "Ignoring unmodified session" );
            session.sendObjectToClient( reply );
            return;
        }

        try {
            origAddr = reply.getSocketAddress();
        } catch ( Exception e ) {
            throw new RuntimeException( "Error getting socket address", e );
        }

        if ( origAddr == null ) {
            throw new RuntimeException( "Error getting socket address" );
        }
        if ( origAddr.getAddress() == null ) {
            throw new RuntimeException( "wildcard address" );
        }

        /* If the server address or port has been NATd/modified */
        if ( (session.getOrigServerPort() != session.getNewServerPort()) || (!session.getOrigServerAddr().equals(session.getNewServerAddr())) ) {
            if ( ! session.getNewServerAddr().equals( origAddr.getAddress() ) ) {
                logger.warn("PASV Reply used an IP (" + origAddr.getAddress().getHostAddress() + ") different than expected (" + session.getNewServerAddr().getHostAddress() + ").");
                logger.warn("Assuming that some NAT fix-up has already been occurred and skipping NAT logic");
            } else {
                /* Modify the response, this must contain the original address the client thinks it
                 * is connecting to. */
                int port = getNextPort( );
                InetSocketAddress newAddr = new InetSocketAddress( session.getOrigServerAddr(), port );

                if (logger.isDebugEnabled()) {
                    logger.debug( "Mangling PASV reply to address: " + newAddr );
                }
            
                SessionRedirect redirect = new SessionRedirect( newAddr.getAddress(), newAddr.getPort(), origAddr.getAddress(), origAddr.getPort() );
                insertSessionRedirect( sessionState, redirect );

                /* Modify the reply to the client */
                reply = FtpReply.pasvReply( newAddr );
            }
        } 

        /**
         * Reply doesn't have to be modified, but the redirect.
         * If we ever support source redirects besides NAT. this will have to be updated
         */
        session.sendObjectToClient( reply );
        return;
    }

    /**
     * epsvCommand handles EPSV command
     * @param session
     * @param command
     */
    private void epsvCommand( AppTCPSession session, FtpCommand command )
    {
        session.sendObjectToServer( command );
    }

    /**
     * epsvReply handles EPSV reply
     * @param session
     * @param reply
     */
    private void epsvReply( AppTCPSession session, FtpReply reply )
    {
        SessionState sessionState = getSessionState( session );
        if ( sessionState == null ) {
            logger.debug( "Ignoring unmodified session" );
            throw new RuntimeException( "Missing session data");
        }
        
        InetSocketAddress origAddr;
        try {
            origAddr = reply.getSocketAddress();
        } catch ( Exception e ) {
            throw new RuntimeException( "Error getting socket address", e );
        }

        if ( origAddr == null ) {
            throw new RuntimeException( "Error getting socket address" );
        }

        if ( (session.getOrigServerPort() != session.getNewServerPort()) || (!session.getOrigServerAddr().equals(session.getNewServerAddr())) ) {
            int port = getNextPort( );
            /**
             * Create a new socket address with the original server address.
             * extended passive mode doesn't specify the address, so in order to catch
             * the data session, NAT creates a new extended passive reply which keeps a
             * separate copy of the InetSocketAddress.
             */
            InetSocketAddress newAddr = new InetSocketAddress( session.getOrigServerAddr(), port );
        
            if (logger.isDebugEnabled()) {
                logger.debug( "Mangling EPSV reply to address: " + newAddr );
            }
            
            SessionRedirect redirect = new SessionRedirect( newAddr.getAddress(), newAddr.getPort(), session.getNewServerAddr(), origAddr.getPort() );
            insertSessionRedirect( sessionState, redirect );

            session.sendObjectToClient( FtpEpsvReply.makeEpsvReply( newAddr ) );
            return;
        } else {
            session.sendObjectToClient( reply );
        }            

        return;
    }

    /**
     * getSessionState returns the session state for the provided session
     * @param session - the session
     * @return SessionState
     */
    private SessionState getSessionState( AppTCPSession session )
    {
        SessionState sessionState = (SessionState) session.attachment();
        
        if ( sessionState == null ) {
            /* Get the information the router app is tracking about the session */
            sessionState = new SessionState();
            session.attach( sessionState );
        }

        return sessionState;
    }

    /**
     * insertSessionRedirect inserts the redirect
     * @param sessionState
     * @param redirect
     */
    private void insertSessionRedirect( SessionState sessionState, SessionRedirect redirect )
    {
        redirectMap.put( redirect.key, redirect );
        sessionState.redirects.add( redirect );
    }
    
    /**
     * getNextPort provides the port to use (for the redirect)
     * @return the next port to use
     */
    private synchronized int getNextPort()
    {
        int ret = portCurrent;

        portCurrent++;
        if ( portCurrent > portStart + portRange )
            portCurrent = portStart;

        return ret;
    }
}

/**
 * SessionState stores the state associated with this session.
 * The active redirect(port forwards) for this session are stored.
 */
class SessionState
{
    protected LinkedList<SessionRedirect> redirects = new LinkedList<SessionRedirect>();
}

/**
 * The SessionRedirect key for the map
 */
class SessionRedirectKey
{
    final Protocol    protocol;
    final InetAddress serverAddr;
    final int         serverPort;
    final int         hashCode;

    /**
     * Create a new SessionRedirectKey
     * @param protocol
     * @param serverAddr
     * @param serverPort
     */
    SessionRedirectKey( Protocol protocol, InetAddress serverAddr, int serverPort )
    {
        this.protocol   = protocol;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        hashCode = calculateHashCode();
    }

    /**
     * hashCode
     * @return hashCode
     */
    public int hashCode()
    {
        return hashCode;
    }

    /**
     * equals returns true if the object equals the parameter, false otherwise
     * @param o - the object to compare
     * @return equals
     */
    public boolean equals( Object o )
    {
        if (!( o instanceof SessionRedirectKey )) return false;

        SessionRedirectKey key = (SessionRedirectKey)o;

        if ( this.protocol != key.protocol ||
             !this.serverAddr.equals( key.serverAddr ) ||
             this.serverPort != key.serverPort ) {
            return false;
        }

        return true;
    }

    /**
     * toString provides a string human readable summary of the SessionRedirectKey
     * @return the string
     */
    public String toString()
    {
        return "SessionRedirectKey| [" + protocol +"] " + "/" + serverAddr + ":" + serverPort;
    }

    /**
     * provide the hashCode of this key
     * @return the hashCode
     */
    private int calculateHashCode()
    {
        int result = 17;
        result = ( 37 * result ) + protocol.hashCode();
        result = ( 37 * result ) + serverAddr.hashCode();
        result = ( 37 * result ) + serverPort;

        return result;
    }
}

/**
 * A session redirect is a redirect or "port forward"
 * Because NAT rewrites address, we must explicitly add port forwards so that PASV/PORT commands work
 * A SessionRedirect stores an active "port forward" and all the associated information
 */
class SessionRedirect
{
    private static final Logger logger = Logger.getLogger( SessionRedirect.class );

    final InetAddress newServerAddr;
    final int         newServerPort;
    final InetAddress origServerAddr;
    final int         origServerPort;

    final SessionRedirectKey key;

    private boolean removed = false;

    private String redirectRuleFilter;
    private String redirectRuleIp;
    private int redirectRulePort;
    
    /**
     * Create a SessionRedirect from the original server addr/port to the new server addr/port
     * @param origServerAddr
     * @param origServerPort
     * @param newServerAddr
     * @param newServerPort
     */
    protected SessionRedirect( InetAddress origServerAddr, int origServerPort, InetAddress newServerAddr, int newServerPort )
    {
        insertRedirectRule( origServerAddr, origServerPort, newServerAddr, newServerPort );
        this.origServerAddr   = origServerAddr;
        this.origServerPort   = origServerPort;
        this.newServerAddr   = newServerAddr;
        this.newServerPort   = newServerPort;
        this.key = new SessionRedirectKey( Protocol.TCP, origServerAddr, origServerPort );
    }

    /**
     * toString provides a string human readable summary of the SessionRedirect
     * @return the string
     */
    public String toString()
    {
        return "SessionRedirect| " + origServerAddr + ":" + origServerAddr + " -> " + newServerAddr + ":" + newServerPort;
    }

    /**
     * If this redirect is in netfilter, cleanup removes it
     */
    synchronized void cleanup()
    {
        if ( removed == false ) {
            removeRedirectRule();
        }

        removed = true;
    }
    
    /**
     * insertRedirectRule inserts a redirect into netfilter (into the port-forward-rules chain)
     * @param origServerAddr <doc>
     * @param origServerPort <doc>
     * @param newServerAddr <doc>
     * @param newServerPort <doc>
     */
    private synchronized void insertRedirectRule( InetAddress origServerAddr, int origServerPort, InetAddress newServerAddr, int newServerPort )
    {
        if (logger.isDebugEnabled()) {
            logger.debug("newServerAddr:"+newServerAddr);
            logger.debug("newServerPort:"+newServerPort);
            logger.debug("origServerAddr:"+origServerAddr);
            logger.debug("origServerPort:"+origServerPort);
        }
        redirectRuleFilter =
            "-p tcp "
            + " -d " + origServerAddr.getHostAddress()
            + " --dport "+ origServerPort ;
        redirectRuleIp = newServerAddr.getHostAddress();
        redirectRulePort = newServerPort;
        if (logger.isDebugEnabled()) {
            logger.debug("CREATE redirect rule");
            logger.debug("rule filter: "+redirectRuleFilter);
            logger.debug("rule newServerAddr: "+redirectRuleIp);
            logger.debug("rule newServerPort: "+redirectRulePort);
        }

        String cmd = "iptables -t nat -I port-forward-rules " + redirectRuleFilter + " -m comment --comment \"FTP redirect\"  -j DNAT --to-destination " + redirectRuleIp + ":" + redirectRulePort;
        logger.warn( "FTP iptables cmd: " + cmd );
        int result = UvmContextFactory.context().execManager().execResult( cmd );
        if (result != 0) {
            logger.warn( "Command failed: " + cmd );
        }

        return;
    }

    /**
     * removeRedirectRule remove this redirect from netfilter
     */
    private synchronized void removeRedirectRule()
    {
        if (logger.isDebugEnabled()) {
            logger.debug("DESTROY redirect rule");
            logger.debug("rule filter: "+redirectRuleFilter);
            logger.debug("rule clientIp: "+redirectRuleIp);
            logger.debug("rule clientPort: "+redirectRulePort);
        }

        String cmd = "iptables -t nat -D port-forward-rules " + redirectRuleFilter + " -m comment --comment \"FTP redirect\"  -j DNAT --to-destination " + redirectRuleIp + ":" + redirectRulePort;
        logger.warn( "FTP iptables cmd: " + cmd );
        int result = UvmContextFactory.context().execManager().execResult( cmd );
        if (result != 0) {
            logger.warn( "Command failed: " + cmd );
        }
    }
}
