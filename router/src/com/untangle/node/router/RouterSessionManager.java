/**
 * $Id$
 */
package com.untangle.node.router;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.vnet.IPNewSessionRequest;
import com.untangle.uvm.vnet.NodeSession;
import com.untangle.uvm.vnet.Protocol;
import com.untangle.uvm.vnet.NodeTCPSession;
import org.apache.log4j.Logger;

import org.json.JSONObject;

class RouterSessionManager
{
    Map<Long,RouterSessionData> map = new ConcurrentHashMap<Long,RouterSessionData>();

    Map<SessionRedirectKey,SessionRedirect> redirectMap = new ConcurrentHashMap<SessionRedirectKey,SessionRedirect>();

    private final Logger logger = Logger.getLogger( this.getClass());

    public RouterSessionManager( )
    {
    }

    void registerSession( IPNewSessionRequest request )
    {
        RouterSessionData data =
            new RouterSessionData( request.getOrigClientAddr(), request.getOrigClientPort(),
                                   request.getNewClientAddr(), request.getNewClientPort(),
                                   request.getOrigServerAddr(), request.getOrigServerPort(),
                                   request.getNewServerAddr(), request.getNewServerPort());

        if (logger.isDebugEnabled()) {
            logger.debug( "Registering session: " + request.id());
        }

        /* Insert the data into the map */
        RouterSessionData tmp;
        if (( tmp = map.put( request.id(), data )) != null ) {
            logger.error( "Duplicate session key: " + tmp );
        }

    }

    void registerSession( NodeTCPSession session )
    {
        RouterSessionData data =
            new RouterSessionData( session.getOrigClientAddr(), session.getOrigClientPort(),
                                   session.getNewClientAddr(), session.getNewClientPort(),
                                   session.getOrigServerAddr(), session.getOrigServerPort(),
                                   session.getNewServerAddr(), session.getNewServerPort());

        if (logger.isDebugEnabled()) {
            logger.debug( "Registering session: " + session.id());
        }

        /* Insert the data into the map */
        RouterSessionData tmp;
        if (( tmp = map.put( session.id(), data )) != null ) {
            logger.error( "Duplicate session key: " + tmp );
        }

    }
    
    void releaseSession( NodeSession session )
    {
        RouterSessionData sessionData;
        if (logger.isDebugEnabled()) {
            logger.debug( "Releasing session: " + session.id());
        }
        if (( sessionData = map.remove( session.id())) == null ) {
            logger.debug( "Released an unmanaged session: " + session );
            return;
        }

        /* Have to release all of the SessionRedirect */
        for ( Iterator<SessionRedirect> iter = sessionData.redirectList().iterator() ; iter.hasNext() ; ) {
            SessionRedirect sessionRedirect = iter.next();
            if (logger.isDebugEnabled()) {
                logger.debug( "Releasing sessionRedirect ");
            }
            /* Remove the item from the iterating list */
            iter.remove();

            SessionRedirect currentRedirect = redirectMap.remove( sessionRedirect.key );

            /* Remove the key from the redirect hash map */
            if ( currentRedirect != null && currentRedirect != sessionRedirect  ) {
                logger.error( "Redirect map mismatch" );
            }

            /* Cleanup the redirect */
            sessionRedirect.cleanup( );
        }
    }

    RouterSessionData getSessionData( NodeTCPSession session )
    {
        return map.get( session.id());
    }

    /**
     * Request to redirect a session.
     */
    void registerSessionRedirect( RouterSessionData data, SessionRedirect redirect )
    {
        /* Add the redirect to the list monitored by this session */
        data.addRedirect( redirect );

        if ( logger.isDebugEnabled()) {
            logger.debug( "Registering[" + redirect.key + "],[" + redirect + "]" );
        }

        /* Add the redict to the map of redirects */
        redirectMap.put( redirect.key, redirect );
    }

    /**
     * Check to see if this session should be redirected because of one of the
     * it is in the session redirect map
     */
    boolean isSessionRedirect( NodeSession session, Protocol protocol, RouterImpl node )
    {
        SessionRedirectKey key = new SessionRedirectKey( session, protocol );
        SessionRedirect redirect;

        if ( logger.isDebugEnabled()) {
            logger.debug( "Looking up session: " + key );
        }
        if (( redirect = redirectMap.remove( key )) == null ) {
            return false;
        }
        if ( logger.isDebugEnabled()) {
            logger.debug( "Session redirect match: " + redirect );
        }

        /* Remove the redirect rule once it is matched */
        redirect.cleanup();

        return true;
    }
}

/* For a temporary session redirect you know everything about the session except for the
 * client port */
class SessionRedirectKey
{
    final Protocol    protocol;
    final InetAddress serverAddr;
    final int         serverPort;
    final int         hashCode;

    SessionRedirectKey( Protocol protocol, InetAddress serverAddr, int serverPort )
    {
        this.protocol   = protocol;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        hashCode = calculateHashCode();
    }

    SessionRedirectKey( IPNewSessionRequest request, Protocol protocol )
    {
        this( protocol, request.getOrigServerAddr(), request.getOrigServerPort());
    }

    SessionRedirectKey( NodeTCPSession session )
    {
        this( Protocol.TCP, session.getServerAddr(), session.getServerPort());
    }

    SessionRedirectKey( NodeSession session, Protocol protocol )
    {
        this( protocol, session.getServerAddr(), session.getServerPort());
    }

    public int hashCode()
    {
        return hashCode;
    }

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

    public String toString()
    {
        return "SessionRedirectKey| [" + protocol +"] " + "/" + serverAddr + ":" + serverPort;
    }

    private int calculateHashCode()
    {
        int result = 17;
        result = ( 37 * result ) + protocol.hashCode();
        result = ( 37 * result ) + serverAddr.hashCode();
        result = ( 37 * result ) + serverPort;

        return result;
    }
}



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
    
    protected SessionRedirect( InetAddress origServerAddr, int origServerPort, InetAddress newServerAddr, int newServerPort )
    {
        createRedirectRule( origServerAddr, origServerPort, newServerAddr, newServerPort );
        this.origServerAddr   = origServerAddr;
        this.origServerPort   = origServerPort;
        this.newServerAddr   = newServerAddr;
        this.newServerPort   = newServerPort;
        this.key = new SessionRedirectKey( Protocol.TCP, origServerAddr, origServerPort );
    }

    public String toString()
    {
        return "SessionRedirect| " + origServerAddr + ":" + origServerAddr + " -> " + newServerAddr + ":" + newServerPort;
    }

    synchronized void cleanup()
    {
        if ( removed == false ) {
            removeRedirectRule();
        }

        removed = true;
    }
    
    private synchronized void createRedirectRule( InetAddress origServerAddr, int origServerPort, InetAddress newServerAddr, int newServerPort )
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
