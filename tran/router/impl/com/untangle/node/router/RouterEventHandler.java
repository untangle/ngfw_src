/*
 * Copyright (c) 2003-2006 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.node.router;

import java.net.InetAddress;
import java.net.Inet4Address;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import com.untangle.uvm.ArgonManager;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.IntfConstants;
import com.untangle.uvm.UvmContextFactory;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.localapi.ArgonInterface;

import com.untangle.uvm.networking.NetworkException;
import com.untangle.uvm.networking.IPNetwork;
import com.untangle.uvm.networking.RedirectRule;
import com.untangle.uvm.networking.internal.NetworkSpaceInternal;
import com.untangle.uvm.networking.internal.NetworkSpacesInternalSettings;
import com.untangle.uvm.networking.internal.InterfaceInternal;
import com.untangle.uvm.networking.internal.RedirectInternal;

import com.untangle.uvm.tapi.AbstractEventHandler;
import com.untangle.uvm.tapi.IPNewSessionRequest;
import com.untangle.uvm.tapi.IPSession;
import com.untangle.uvm.tapi.MPipeException;
import com.untangle.uvm.tapi.Protocol;
import com.untangle.uvm.tapi.UDPNewSessionRequest;
import com.untangle.uvm.tapi.UDPSession;
import com.untangle.uvm.tapi.event.TCPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.TCPSessionEvent;
import com.untangle.uvm.tapi.event.UDPNewSessionRequestEvent;
import com.untangle.uvm.tapi.event.UDPSessionEvent;
import com.untangle.uvm.node.IPaddr;
import com.untangle.uvm.node.Node;
import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.ParseException;

import com.untangle.uvm.node.firewall.ip.IPMatcher;
import com.untangle.uvm.node.firewall.ip.IPMatcherFactory;
import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.node.firewall.port.PortMatcher;
import com.untangle.uvm.node.firewall.port.PortMatcherFactory;
import com.untangle.uvm.node.firewall.InterfaceAddressRedirect;
import com.untangle.uvm.node.firewall.InterfaceRedirect;
import com.untangle.uvm.node.firewall.InterfaceStaticRedirect;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcher;
import com.untangle.uvm.node.firewall.protocol.ProtocolMatcherFactory;

import org.apache.log4j.Logger;

import static com.untangle.node.router.RouterConstants.*;

class RouterEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(RouterEventHandler.class);
    
    private static final String PROPERTY_BASE = "com.untangle.node.router.";
    private static final String PROPERTY_TCP_PORT_START = PROPERTY_BASE + "tcp-port-start";
    private static final String PROPERTY_TCP_PORT_END   = PROPERTY_BASE + "tcp-port-end";
    private static final String PROPERTY_UDP_PORT_START = PROPERTY_BASE + "udp-port-start";
    private static final String PROPERTY_UDP_PORT_END   = PROPERTY_BASE + "udp-port-end";
    private static final String PROPERTY_ICMP_PID_START = PROPERTY_BASE + "icmp-pid-start";
    private static final String PROPERTY_ICMP_PID_END   = PROPERTY_BASE + "icmp-pid-end";
    
    /* match to determine whether a session is natted */
    private List<NatMatcher> natMatchers     = Collections.emptyList();

    /* match to determine whether a session should pass to be passed to the DMZ
     * on a NATd interface */
    private List<DmzMatcher> dmzHostMatchers = Collections.emptyList();

    /* match to determine  whether a session is directed for the dmz */
    private RedirectMatcher dmzHost = RedirectMatcher.MATCHER_DISABLED;

    /* True if logging DMZ redirects */
    private boolean isDmzLoggingEnabled = false;

    /* All of the other rules */
    /* Use an empty list rather than null */
    private List<RedirectMatcher> redirectList = new LinkedList<RedirectMatcher>();
    
    /* A list of all of the traffic blockers */
    private List<RequestIntfMatcher> trafficBlockers = new LinkedList<RequestIntfMatcher>();

    /* A list of matchers that block traffic that would pass unfiltered */
    private List<RequestIntfMatcher> unmodifiedBlockers = new LinkedList<RequestIntfMatcher>();

    /* tracks the open TCP ports for NAT */
    private final PortList tcpPortList;

    /* Tracks the open UDP ports for NAT */
    private final PortList udpPortList;

    /* Tracks the open ICMP identifiers, Not exactly a port, but same kind of thing */
    private final PortList icmpPidList;

    /* Router Node */
    private final RouterImpl node;


    /* Setup  */
    RouterEventHandler( RouterImpl node )
    {
        super(node);

        int start = Integer.getInteger( PROPERTY_TCP_PORT_START, TCP_NAT_PORT_START );
        int end = Integer.getInteger( PROPERTY_TCP_PORT_END, TCP_NAT_PORT_END );
        tcpPortList = PortList.makePortList( start, end );

        start = Integer.getInteger( PROPERTY_UDP_PORT_START, UDP_NAT_PORT_START );
        end = Integer.getInteger( PROPERTY_UDP_PORT_END, UDP_NAT_PORT_END );
        udpPortList = PortList.makePortList( start, end );

        start = Integer.getInteger( PROPERTY_ICMP_PID_START, ICMP_PID_START );
        end = Integer.getInteger( PROPERTY_ICMP_PID_END, ICMP_PID_END );
        icmpPidList = PortList.makePortList( start, end );
        this.node = node;
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest(), Protocol.TCP );
    }

    public void handleUDPNewSessionRequest( UDPNewSessionRequestEvent event )
        throws MPipeException
    {
        handleNewSessionRequest( event.sessionRequest(), Protocol.UDP );
    }

    private void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
        throws MPipeException
    {
        InetAddress clientAddr = request.clientAddr();
        int         clientPort = request.clientPort();
        InetAddress serverAddr = request.serverAddr();
        int         serverPort = request.serverPort();

        RouterAttachment attachment = new RouterAttachment();

        request.attach( attachment );
        
        if ( matchesTrafficBlocker( request )) {
            node.incrementCount( BLOCK_COUNTER ); // BLOCK COUNTER
            
            /* XXX How should the session be rejected */
            request.rejectSilently();
            return;
        }
        
        /* Check for NAT, Redirects or DMZ */
        try {
            if (logger.isInfoEnabled()) logger.info( "Testing <" + request + ">" );
                
            if ( handleRouter( request, protocol )      ||
                 handleRedirect( request, protocol ) ||
                 handleDmzHost( request,  protocol )) {

                if (logger.isInfoEnabled()) logger.info( "<" + request + "> is nat, redirect or dmz" );

                /* Nothing left to do (sesssion is already released, and doesn't require finalization) */
                if ( request.attachment() == null ) return;

                request.release( true );

                if ( isFtp( request, protocol )) {
                    node.getSessionManager().registerSession( request, protocol,
                                                                   clientAddr, clientPort,
                                                                   serverAddr, serverPort );

                    attachment.isManagedSession( true );
                }

                return;
            }

            /* If nat is on, and this session wasn't natted,
             * redirected or dmzed, it must be rejected */
            if ( isUnmodifiedSessionBlocked( request )) {
                node.incrementCount( BLOCK_COUNTER ); // BLOCK COUNTER
                
                /* XXX How should the session be rejected */
                request.rejectSilently();
                return;
            }
        } catch ( RouterUnconfiguredException e ) {
            logger.warn( "Outside network is presently not configured, rejecting session silently" );
            request.rejectSilently();
            return;
        }

        /* Otherwise release the session, and don't care about the finalization */
        request.release(false);
    }

    @Override
    public void handleTCPComplete(TCPSessionEvent event)
        throws MPipeException
    {
        IPSession s = event.session();
        RouterAttachment na = (RouterAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                node.log(eventToLog);
                na.eventToLog(null);
            }
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        IPSession s = event.session();
        RouterAttachment na = (RouterAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                node.log(eventToLog);
                na.eventToLog(null);
            }
        }
    }

    public void handleTCPFinalized(TCPSessionEvent event)
        throws MPipeException
    {
        cleanupSession( Protocol.TCP, event.ipsession());
    }

    public void handleUDPFinalized(UDPSessionEvent event)
        throws MPipeException
    {
        /* XXX Special case ICMP */
        UDPSession udpsession = (UDPSession)event.ipsession();

        if ( udpsession.isPing()) {
            RouterAttachment attachment = (RouterAttachment)udpsession.attachment();
            int pid = udpsession.icmpId();
            int releasePid;

            if ( attachment == null ) {
                logger.error( "null attachment on Routerd session" );
                return;
            }

            releasePid = attachment.releasePort();

            if ( releasePid == 0 ) {
                if ( logger.isDebugEnabled()) logger.debug( "Ignoring non-natted PID: " + pid );
            } else if ( pid != releasePid ) {
                /* This is now an error, because the releasePid should be zero if it is not
                 * to be released */
                logger.error( "Mismatch on the attached PID and the session PID " +
                              pid + "!=" + releasePid );
            } else {
                if ( logger.isDebugEnabled()) logger.debug( "ICMP: Releasing pid: " + releasePid );
                
                icmpPidList.releasePort( releasePid );
            }
        } else {
            cleanupSession( Protocol.UDP, udpsession );
        }
    }

    void configure( NetworkSpacesInternalSettings settings )
        throws NodeException
    {        
        ArgonManager argonManager = UvmContextFactory.context().argonManager();
        NetworkManager networkManager = UvmContextFactory.context().networkManager();

        /* Create a new override */
        List<InterfaceRedirect> overrideList = new LinkedList<InterfaceRedirect>();
        
        /* Create a list of the NAT Matchers */
        List<NatMatcher> natMatchers = new LinkedList<NatMatcher>();

        /* Create a list of the DMZ host matchers */
        List<DmzMatcher> dmzHostMatchers = new LinkedList<DmzMatcher>();

        /* Create a list to block traffic from being forwarded */
        List<RequestIntfMatcher> trafficBlockers = new LinkedList<RequestIntfMatcher>();
        
        /* Create a list to block unmodified traffic that shouldn't be forwarded. */
        List<RequestIntfMatcher> unmodifiedBlockers = new LinkedList<RequestIntfMatcher>();
                
        /* First deal with all of the NATd spaces */
        for ( NetworkSpaceInternal space : settings.getNetworkSpaceList()) {
            /* When settings are disabled, ignore everything but the primary space or
             * if the space is just disabled. */
            if (( !settings.getIsEnabled() && space.getIndex() != 0 ) || !space.getIsEnabled()) continue;
            
            /* If the network space has multiple interfaces, than the traffic has to be allowed
             * between those interfaces */
            setupTrafficFlow( space, trafficBlockers, unmodifiedBlockers );
            
            if ( space.getIsDmzHostEnabled()) {
                logger.debug( "Inserting new dmz host matcher: " + space );
                dmzHostMatchers.add( DmzMatcher.makeDmzMatcher( space, settings ));
            }

            
            if ( space.getIsNatEnabled()) {
                /* Create a NAT matcher for this space */
                for ( IPNetwork networkRule : (List<IPNetwork>)space.getNetworkList()) {
                    natMatchers.add( NatMatcher.makeNatMatcher( networkRule, space ));
                }                
            }
        }
        
        /* Save all of the objects at once */

        /* Empty out the list */
        List<RedirectMatcher> redirectMatcherList = new LinkedList<RedirectMatcher>();

        /* Update all of the rules */
        for ( RedirectInternal internal : settings.getRedirectList()) {
            if ( internal.getIsEnabled()) {
                RedirectMatcher redirect = new RedirectMatcher( internal );

                logger.debug( "Adding redirect: redirect" );
                redirectMatcherList.add( redirect );
                
                overrideList.add( makeInterfaceAddressRedirect( internal ));
            }
        }
        
        /* Add the DMZ overrides first */
        for ( DmzMatcher dmzMatcher : dmzHostMatchers ) overrideList.add( dmzMatcher.getInterfaceRedirect());
        
        /* Last add the NAT overrides */
        for ( NatMatcher natMatcher : natMatchers ) overrideList.add( natMatcher.getInterfaceRedirect());
        
        /* Set the redirect list at the end(avoid concurrency issues) */
        this.redirectList        = redirectMatcherList;
        this.natMatchers         = natMatchers;
        this.dmzHostMatchers     = dmzHostMatchers;
        this.trafficBlockers     = Collections.unmodifiableList( trafficBlockers );
        this.unmodifiedBlockers  = Collections.unmodifiableList( unmodifiedBlockers );

        argonManager.setInterfaceOverrideList( overrideList );
        networkManager.subscribeLocalOutside( true );
    }

    /* Not sure if this should ever throw an exception */
    void deconfigure()
    {
        UvmContextFactory.context().networkManager().subscribeLocalOutside( false );
    }

    /**
     * Determine if a session is natted, and if necessary, rewrite its session information.
     */
    private boolean handleRouter( IPNewSessionRequest request, Protocol protocol )
        throws MPipeException, RouterUnconfiguredException
    {
        int port;
        
        boolean isRouter = false;
        NatMatcher natMatcher = null;
        
        for ( NatMatcher matcher : natMatchers ) {
            if ( matcher.isMatch( request, protocol )) {
                natMatcher = matcher;
                break;
            }
        }

        /* !!!!!!!!!!!!!!!!!! have to check for VPN */
        if (( natMatcher != null )) {
            /* !!!!!! Checking if destined to local network doesn't really work anymore because   *
             * it may be bridged, hopefully the interface check in the argon hook should fix this */
            /* Check to see if this is redirect, check before changing the source address */
            handleRedirect( request, protocol );

            /* Change the source in the request */
            /* All redirecting occurs here */

            /* !!!!!!! Need to be able to grab the new address, and have that update with DHCP */
            InetAddress localAddr = natMatcher.getNatAddress();

            // Unfortunately, as of 5/02/05, this can sometimes be null, probably due to
            // initialization order, the sleeping involved, etc.  For now, just make sure
            // not to ever change the client addr to null, which causes awful things to happen.  jdi XXX
            if (localAddr == null) throw RouterUnconfiguredException.getInstance();

            /* Update the client address */
            request.clientAddr( localAddr );

            /* Set the client port */
            /* XXX THIS IS A HACK, it really should check if the protocol is ICMP, but
             * for now there are only UDP sessions */
            /* !!!!! This actually gets worse memory wise, because now there should be 
             * a port list per space */
            if ( request.clientPort() == 0 && request.serverPort() == 0 ) {
                port = icmpPidList.getNextPort();
                ((UDPNewSessionRequest)request).icmpId( port );
                if (logger.isDebugEnabled()) {
                    logger.debug( "Redirect PING session to id: " + port );
                }
            } else {
                port = getNextPort( protocol );
                request.clientPort( port );

                if (logger.isDebugEnabled()) {
                    logger.debug( "Redirecting session to port: " + port );
                }
            }

            ((RouterAttachment)request.attachment()).releasePort( port );

            /* Increment the NAT counter */
            node.incrementCount( NAT_COUNTER ); // NAT COUNTER

            /* Log the stat */
            node.statisticManager.incrRouterSessions();

            return true;
        }

        return false;
    }

    /**
     * Determine if a session is redirected, and if necessary, rewrite its session information.
     */
    private boolean handleRedirect( IPNewSessionRequest request, Protocol protocol ) throws MPipeException
    {
        /* Check if this is a session redirect (A redirect related to a session) */
        if ( node.getSessionManager().isSessionRedirect( request, protocol )) {
            /* Increment the NAT counter */
            node.incrementCount( REDIR_COUNTER );

            return true;
        }

        for ( Iterator<RedirectMatcher> iter = redirectList.iterator(); iter.hasNext(); ) {
            RedirectMatcher matcher = iter.next();

            if ( matcher.isMatch( request, protocol )) {
                /* Redirect the session */
                matcher.redirect( request );

                /* Increment the NAT counter */
                node.incrementCount( REDIR_COUNTER );

                /* log the event if necessary */
                if ( matcher.getIsLoggingEnabled()) {
                    RouterAttachment attachment = (RouterAttachment)request.attachment();
                    if ( attachment == null ) {
                        logger.error( "null attachment to a NAT session" );
                    } else {
                        attachment.eventToLog(new RedirectEvent( request.pipelineEndpoints(), 
                                                                 matcher.ruleIndex()));
                    }
                }

                /* Log the stat */
                node.statisticManager.incrRedirect( protocol, request );

                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a session is for the DMZ, and if necessary, rewrite its session information.
     */
    private boolean handleDmzHost( IPNewSessionRequest request, Protocol protocol )
    {
        for ( DmzMatcher matcher : dmzHostMatchers ) {
            if ( logger.isDebugEnabled()) logger.debug( "testing dmz matcher" );
            
            if ( matcher.isMatch( request, protocol )) {
                if ( logger.isDebugEnabled()) logger.debug( "dmz match" ); //  DELME

                /* Increment the DMZ counter */
                node.incrementCount( DMZ_COUNTER ); // DMZ COUNTER

                matcher.redirect( request );

                if ( matcher.getIsLoggingEnabled()) {
                    RouterAttachment attachment = (RouterAttachment)request.attachment();
                    
                    if ( attachment == null ) {
                        logger.error( "null attachment to a NAT session" );
                    } else {
                        attachment.eventToLog(new RedirectEvent( request.pipelineEndpoints()));
                    }
                }
                node.statisticManager.incrDmzSessions();
                return true;
            }
        }

        return false;
    }

    /**
     * Retrieve the next port from the port list
     */
    int getNextPort( Protocol protocol )
    {
        return getPortList( protocol ).getNextPort();
    }

    /**
     * Release a port
     * Utility function for RouterSessionManager.
     */
    void releasePort( Protocol protocol, int port )
    {
        getPortList( protocol ).releasePort( port );
    }

    private void setupTrafficFlow( NetworkSpaceInternal space, 
                                   List<RequestIntfMatcher> blockers, 
                                   List<RequestIntfMatcher> unmodified )
    {
        List<InterfaceInternal> interfaceList = space.getInterfaceList();

        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();

        /* Block traffic from leaving this network space */
        if ( !space.getIsTrafficForwarded()) {
            try {
                blockers.add( RequestIntfMatcher.makeInstance( space ));
            } catch ( NodeException e ) {
                logger.error( "unable to create a traffic blocker for [" + space + "]", e );
            }
        }

        if ( space.getIsNatEnabled()) {
            /* Block traffic from entering this network space unfiltered */
            try {
                unmodified.add( RequestIntfMatcher.makeRouterInstance( space ));
            } catch ( NodeException e ) { 
                logger.error( "Unable to create a traffic passer for [" + space + "]", e );
            }
        }
    }

    /** Returns true if the request matches a traffic blocker */
    private boolean matchesTrafficBlocker( IPNewSessionRequest request )
    {
        /* Test all of the traffic blockers to determine if the traffic should be blocked */
        for ( RequestIntfMatcher blocker : trafficBlockers ) {
            if ( blocker.isMatch( request )) return true;
        }

        return false;
    }
    
    /** Returns true if the request should be allowed through unmodified (not NATd or redirected) */
    private boolean isUnmodifiedSessionBlocked( IPNewSessionRequest request )
    {
        /* Test all of the traffic  to determine if the traffic should be allowed
         * to pass unmodified */
        for ( RequestIntfMatcher blocker : this.unmodifiedBlockers ) {
            if ( blocker.isMatch( request )) return true;
        }

        return false;
    }

    /**
     * Cleanup any of the information associated with a UDP or TCP session.
     * Presently not implemented to handle ICMP sessions.
     */
    private void cleanupSession( Protocol protocol, IPSession session )
    {
        RouterAttachment attachment = (RouterAttachment)session.attachment();

        if ( attachment == null ) {
            logger.error( "null attachment on Routerd session" );
            return;
        }

        int releasePort = attachment.releasePort();

        if ( releasePort != 0 ) {
            if ( releasePort != session.clientPort() &&
                 releasePort != session.serverPort()) {
                /* This happens for all NAT ftp PORT sessions */
                logger.info( "Release port " + releasePort +" is neither client nor server port" );
            }

            if (logger.isDebugEnabled()) {
                logger.debug( "Releasing port: " + releasePort );
            }

            getPortList( protocol ).releasePort( releasePort );
        } else {
            if (logger.isDebugEnabled())
                if (logger.isDebugEnabled()) {
                    logger.debug( "Ignoring non-natted port: "
                                  + session.clientPort()
                                  + "/" + session.serverPort());
                }
        }

        if ( attachment.isManagedSession()) {
            logger.debug( "Removing session from the managed list" );

            node.getSessionManager().releaseSession( session, protocol );
        }
    }

    private PortList getPortList( Protocol protocol )
    {
        if ( protocol == Protocol.UDP ) {
            return udpPortList;
        } else if ( protocol == Protocol.TCP ) {
            return tcpPortList;
        }

        throw new IllegalArgumentException( "Unknown protocol: " + protocol );
    }

    private boolean isFtp( IPNewSessionRequest request, Protocol protocol )
    {
        if (( protocol == Protocol.TCP ) && ( request.serverPort() == FTP_SERVER_PORT )) {
            return true;
        }

        return false;
    }

    private InterfaceAddressRedirect makeInterfaceAddressRedirect( RedirectInternal internal )
    {
        IPaddr ipaddr = internal.getRedirectAddress();
        InetAddress address = ( ipaddr == null ) ? null : ipaddr.getAddr();

        return new InterfaceAddressRedirect( internal.getProtocol(),
                                             internal.getSrcIntf(), internal.getDstIntf(),
                                             internal.getSrcAddress(), internal.getDstAddress(),
                                             internal.getSrcPort(), internal.getDstPort(),
                                             address );
    }
}

class RequestIntfMatcher
{
    private final boolean isBidirectional;
    private final IntfMatcher client;
    private final IntfMatcher server;
    
    RequestIntfMatcher( IntfMatcher client, IntfMatcher server )
    {
        this( client, server, true );
    }

    RequestIntfMatcher( IntfMatcher client, IntfMatcher server, boolean isBidirectional )
    {
        this.client = client;
        this.server = server;
        this.isBidirectional = isBidirectional;
    }

    boolean isMatch( IPNewSessionRequest request )
    {
        return (( this.client.isMatch( request.clientIntf()) && this.server.isMatch( request.serverIntf())) ||
                ( this.isBidirectional && 
                  this.client.isMatch( request.serverIntf()) && this.server.isMatch( request.clientIntf())));
    }

    public String toString()
    {
        String connector = this.isBidirectional ? " <-> " : " -> ";
        return this.client.toString() + connector + this.server;
    }
    
    static RequestIntfMatcher makeInstance( NetworkSpaceInternal space ) throws NodeException
    {
        List<InterfaceInternal> interfaceList = space.getInterfaceList();
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        
        byte intfArray[] = new byte[interfaceList.size()];
        
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) intfArray[c++] = intf.getArgonIntf().getArgon();

        IntfMatcher clientIntfMatcher, serverIntfMatcher;

        try {
            clientIntfMatcher = imf.makeSetMatcher( intfArray );
            serverIntfMatcher = imf.makeInverseMatcher( intfArray );
        } catch ( ParseException e ) {
            throw new NodeException( "Unable to create the interface matchers", e );
        }
        
        return new RequestIntfMatcher( clientIntfMatcher, serverIntfMatcher );
    }

    static RequestIntfMatcher makeRouterInstance( NetworkSpaceInternal space ) throws NodeException
    {
        List<InterfaceInternal> interfaceList = space.getInterfaceList();
        IntfMatcherFactory imf = IntfMatcherFactory.getInstance();
        
        byte clientIntfArray[] = new byte[interfaceList.size()+1];
        byte serverIntfArray[] = new byte[interfaceList.size()];
        
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) {
            serverIntfArray[c] = clientIntfArray[c++] = intf.getArgonIntf().getArgon();
        }
        /* The client can be from the VPN interface (this is an inverse matcher) */
        clientIntfArray[c] = IntfConstants.VPN_INTF;

        IntfMatcher clientIntfMatcher, serverIntfMatcher;

        try {
            clientIntfMatcher = imf.makeInverseMatcher( clientIntfArray );
            serverIntfMatcher = imf.makeSetMatcher( serverIntfArray );
        } catch ( ParseException e ) {
            throw new NodeException( "Unable to create the interface matchers", e );
        }
        
        return new RequestIntfMatcher( clientIntfMatcher, serverIntfMatcher );
    }

}

class NatMatcher
{
    private final RedirectMatcher matcher;
    private final NetworkSpaceInternal space;
    private InetAddress natAddress;
    private final InterfaceRedirect interfaceRedirect;
    
    private final IntfMatcher vpnClientMatcher;
    private final IntfMatcher vpnServerMatcher;

    NatMatcher( RedirectMatcher matcher, NetworkSpaceInternal space, InetAddress natAddress,
                InterfaceRedirect interfaceRedirect, IntfMatcher vpnClientMatcher, 
                IntfMatcher vpnServerMatcher )
    {
        this.matcher    = matcher;
        this.space      = space;
        this.natAddress = natAddress;
        this.interfaceRedirect = interfaceRedirect;
        this.vpnClientMatcher = vpnClientMatcher;
        this.vpnServerMatcher = vpnServerMatcher;
    }

    RedirectMatcher getMatcher()
    {
        return this.matcher;
    }

    NetworkSpaceInternal getSpace()
    {
        return this.space;
    }

    InetAddress getNatAddress()
    {
        /* !!!!! This has to be updated, eg for DHCP on the NATd address */
        return this.natAddress;
    }

    void setRouterAddress( InetAddress newValue )
    {
        this.natAddress = newValue;
    }

    boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        /* If the matcher matchers, or this fits the profile of a VPN session, NAT it */
        return this.matcher.isMatch( request, protocol ) || 
            ( vpnClientMatcher.isMatch( request.clientIntf()) && 
              vpnServerMatcher.isMatch( request.serverIntf()));
    }

    InterfaceRedirect getInterfaceRedirect()
    {
        return this.interfaceRedirect;
    }
    
    /* Create a matcher for handling traffic to be NATd */
    static NatMatcher makeNatMatcher( IPNetwork network, NetworkSpaceInternal space )
        throws NodeException
    {
        IntfMatcherFactory intfMatcherFactory = IntfMatcherFactory.getInstance();
        IPMatcherFactory imf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        ProtocolMatcherFactory  prmf = ProtocolMatcherFactory.getInstance();

        IPMatcher clientIPMatcher = imf.makeSubnetMatcher( network.getNetwork(), network.getNetmask());
        
        List<InterfaceInternal> interfaceList = space.getInterfaceList();
        byte intfArray[] = new byte[interfaceList.size()];
        byte dstIntfArray[] = new byte[interfaceList.size() + 1 ];

        /* actually not the destination interface, but the inverse of the destination interfaces */
        dstIntfArray[0] = IntfConstants.VPN_INTF;

        IntfMatcher vpnClientMatcher = intfMatcherFactory.getNilMatcher();
        IntfMatcher vpnServerMatcher = intfMatcherFactory.getNilMatcher();

        /* This is used to detect vpn sessions */
        boolean hasInternal = false;
        
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) {
            byte argonIntf = intf.getArgonIntf().getArgon();
            intfArray[c] = argonIntf;
            dstIntfArray[c+1] = argonIntf;
            
            if ( argonIntf == IntfConstants.INTERNAL_INTF ) hasInternal = true;
            c++;
        }
        
        IntfMatcher clientIntfMatcher, serverIntfMatcher;

        try {
            clientIntfMatcher = intfMatcherFactory.makeSetMatcher( intfArray );            
            serverIntfMatcher = intfMatcherFactory.makeInverseMatcher( dstIntfArray );
        } catch ( ParseException e ) {
            throw new NodeException( "Unable to create the client or server interface matcher " +
                                          "for a NAT matcher", e );
        }

        if ( hasInternal ) {
            vpnClientMatcher = intfMatcherFactory.getVpnMatcher();
            vpnServerMatcher = serverIntfMatcher;
        }

        // System.out.println( "client: " + clientIPMatcher + " server: " + serverIntfMatcher );
        RedirectMatcher matcher = new RedirectMatcher( true, false, prmf.getAllMatcher(),
                                                       clientIntfMatcher, serverIntfMatcher,
                                                       clientIPMatcher, imf.getAllMatcher(),
                                                       pmf.getAllMatcher(), pmf.getAllMatcher(),
                                                       false, null, -1 );

        InetAddress natAddress = space.getNatAddress().getAddr();

        /* build the interface redirect for nat traffic that is coming back (eg for FTP */
        /* This just redirects it to the first interface in the nat space, this way it gets through */
        InterfaceRedirect redirect =
            new InterfaceStaticRedirect( prmf.getAllMatcher(),
                                         serverIntfMatcher, intfMatcherFactory.getAllMatcher(),
                                         imf.getAllMatcher(), imf.makeSingleMatcher( natAddress ),
                                         pmf.getAllMatcher(), pmf.getAllMatcher(),
                                         intfArray[0] );

        return new NatMatcher( matcher, space, natAddress, redirect, vpnClientMatcher, vpnServerMatcher );
    }
}

class DmzMatcher
{
    private final RedirectMatcher matcher;
    private final NetworkSpaceInternal space;
    private final InterfaceRedirect interfaceRedirect;

    DmzMatcher( RedirectMatcher matcher, NetworkSpaceInternal space, InterfaceRedirect interfaceRedirect )
    {
        this.matcher = matcher;
        this.space   = space;
        this.interfaceRedirect = interfaceRedirect;
    }

    RedirectMatcher getMatcher()
    {
        return this.matcher;
    }

    NetworkSpaceInternal getSpace()
    {
        return this.space;
    }
    
    boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {
        return matcher.isMatch( request, protocol );
    }

    boolean getIsLoggingEnabled()
    {
        return this.matcher.getIsLoggingEnabled();
    }

    void redirect( IPNewSessionRequest request )
    {
        matcher.redirect( request );
    }

    InterfaceRedirect getInterfaceRedirect()
    {
        return this.interfaceRedirect;
    }

    /* Create a matcher for handling DMZ traffic */
    static DmzMatcher makeDmzMatcher( NetworkSpaceInternal space, NetworkSpacesInternalSettings settings ) 
        throws NodeException
    {
        IntfMatcherFactory intfMatcherFactory = IntfMatcherFactory.getInstance();
        IPMatcherFactory imf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();
        ProtocolMatcherFactory prmf = ProtocolMatcherFactory.getInstance();

        IPMatcher serverIPMatcher = imf.makeSingleMatcher( space.getPrimaryAddress().getNetwork().getAddr());

        List<InterfaceInternal> interfaceList = space.getInterfaceList();

        boolean hasAllInterfaces = ( interfaceList.size() == settings.getInterfaceList().size());
        
        byte intfArray[] = new byte[interfaceList.size()];
        
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) {
            byte argonIntf = intf.getArgonIntf().getArgon();
            
            /* Don't add the internal interface if the network space has all of the interface.
             * this is special cased to handle the situtation where dmz is enabled, but nat is
             * not, unusual, but possible */
            /* This only works because presently there is only one situation where this can
             * occur, in basic mode */
            if ( hasAllInterfaces && argonIntf == IntfConstants.INTERNAL_INTF ) {
                argonIntf = IntfConstants.EXTERNAL_INTF;
            } else {
                intfArray[c++] = argonIntf;
            }
        }

        IntfMatcher clientIntfMatcher, serverIntfMatcher;

        try {
            clientIntfMatcher = intfMatcherFactory.makeSetMatcher( intfArray );
            serverIntfMatcher = intfMatcherFactory.makeInverseMatcher( intfArray );
        } catch ( ParseException e ) {
            throw new NodeException( "Unable to create the interface matchers", e );
        }

        InetAddress dmzHost = null;

        /* XXX !!!! if this is null, this shouldn't create a DMZ matcher */
        if (( space.getDmzHost() != null ) && !space.getDmzHost().isEmpty()) {
            dmzHost = space.getDmzHost().getAddr();
        } else {
            /* DMz host is not set, don't match any traffic */
            clientIntfMatcher = intfMatcherFactory.getNilMatcher();
            serverIntfMatcher = intfMatcherFactory.getNilMatcher();
        }
                
        RedirectMatcher matcher = new RedirectMatcher( true, space.getIsDmzHostLoggingEnabled(),
                                                       prmf.getAllMatcher(),
                                                       clientIntfMatcher, serverIntfMatcher,
                                                       imf.getAllMatcher(), serverIPMatcher,
                                                       pmf.getAllMatcher(), pmf.getAllMatcher(),
                                                       true, dmzHost, -1 );

        /* build the interface redirect for dmz traffic */
        InterfaceRedirect redirect =
            new InterfaceAddressRedirect( prmf.getAllMatcher(),
                                          clientIntfMatcher, serverIntfMatcher,
                                          imf.getAllMatcher(), serverIPMatcher,
                                          pmf.getAllMatcher(), pmf.getAllMatcher(),
                                          dmzHost );
        
        return new DmzMatcher( matcher, space, redirect );
    }
}
