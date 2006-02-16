/*
 * Copyright (c) 2003, 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.net.InetAddress;
import java.net.Inet4Address;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;

import com.metavize.mvvm.ArgonManager;
import com.metavize.mvvm.NetworkManager;
import com.metavize.mvvm.IntfConstants;
import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.networking.NetworkException;
import com.metavize.mvvm.networking.IPNetwork;
import com.metavize.mvvm.networking.RedirectRule;
import com.metavize.mvvm.networking.internal.NetworkSpaceInternal;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import com.metavize.mvvm.networking.internal.InterfaceInternal;
import com.metavize.mvvm.networking.internal.RedirectInternal;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.MPipeException;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.UDPSession;
import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.tran.ParseException;

import com.metavize.mvvm.tran.firewall.ip.IPMatcher;
import com.metavize.mvvm.tran.firewall.ip.IPMatcherFactory;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcher;
import com.metavize.mvvm.tran.firewall.intf.IntfMatcherFactory;
import com.metavize.mvvm.tran.firewall.port.PortMatcher;
import com.metavize.mvvm.tran.firewall.port.PortMatcherFactory;
import com.metavize.mvvm.tran.firewall.InterfaceAddressRedirect;
import com.metavize.mvvm.tran.firewall.InterfaceRedirect;
import com.metavize.mvvm.tran.firewall.InterfaceStaticRedirect;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import org.apache.log4j.Logger;

/* Import all of the constants from NatConstants (1.5 feature) */
import static com.metavize.tran.nat.NatConstants.*;

class NatEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(NatEventHandler.class);

    /* Number of milliseconds to wait in between updating the address and updating the mvvm */
    private static final int SLEEP_TIME = 1000;

    /* match to determine whether a session is natted */
    private List<NatMatcher> natMatchers     = Collections.emptyList();

    /* !!! This is going to have to scale for more stuff */
    private boolean isNatEnabled = false;

    /* match to determine whether a session should pass to be passed to the DMZ
     * on a NATd interface */
    private List<DmzMatcher> dmzHostMatchers = Collections.emptyList();

    // private RedirectMatcher nat = RedirectMatcher.MATCHER_DISABLED;
    // private RedirectMatcher vpn = RedirectMatcher.MATCHER_DISABLED;
    // private IPMatcher natLocalNetwork;

    /* match to determine  whether a session is directed for the dmz */
    private RedirectMatcher dmzHost = RedirectMatcher.MATCHER_DISABLED;

    /* True if logging DMZ redirects */
    private boolean isDmzLoggingEnabled = false;

    /* All of the other rules */
    /* Use an empty list rather than null */
    private List<RedirectMatcher> redirectList = new LinkedList<RedirectMatcher>();

    /* tracks the open TCP ports for NAT */
    private final PortList tcpPortList;

    /* Tracks the open UDP ports for NAT */
    private final PortList udpPortList;

    /* Tracks the open ICMP identifiers, Not exactly a port, but same kind of thing */
    private final PortList icmpPidList;

    /* Nat Transform */
    private final NatImpl transform;


    /* Setup  */
    NatEventHandler( NatImpl transform )
    {
        super(transform);

        tcpPortList = PortList.makePortList( TCP_NAT_PORT_START, TCP_NAT_PORT_END );
        udpPortList = PortList.makePortList( UDP_NAT_PORT_START, UDP_NAT_PORT_END );
        icmpPidList = PortList.makePortList( ICMP_PID_START, ICMP_PID_END );
        this.transform = transform;
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

        NatAttachment attachment = new NatAttachment();

        request.attach( attachment );

        /* Check for NAT, Redirects or DMZ */
        try {
            if (logger.isInfoEnabled())
                logger.info( "Testing <" + request + ">" );
            if ( handleNat( request, protocol )      ||
                 handleRedirect( request, protocol ) ||
                 handleDmzHost( request,  protocol )) {


                if (logger.isInfoEnabled())
                    logger.info( "<" + request + "> is nat, redirect or dmz" );

                /* Nothing left to do */
                if ( request.attachment() == null ) return;

                request.release( true );

                if ( isFtp( request, protocol )) {
                    transform.getSessionManager().registerSession( request, protocol,
                                                                   clientAddr, clientPort,
                                                                   serverAddr, serverPort );

                    attachment.isManagedSession( true );
                }
                return;
            }

            /* DMZ Sessions do not require finalization */
            if ( isDmz( request, protocol )) {
                request.release( false );
                return;
            }

            /* VPN Sessions that don't require NAT. */
            if ( isVpn( request, protocol )) {
                request.release( false );
                return;
            }

            /* If nat is on, and this session wasn't natted, redirected or dmzed, it
             * must be rejected */
            if ( isNatEnabled ) {
                /* !!!!  This must get more interesting */
                /* Increment the block counter */
                transform.incrementCount( BLOCK_COUNTER ); // BLOCK COUNTER

                /* XXX How should the session be rejected */
                request.rejectSilently();
                return;
            }
        } catch ( NatUnconfiguredException e ) {
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
        NatAttachment na = (NatAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                transform.log(eventToLog);
                na.eventToLog(null);
            }
        }
    }

    @Override
    public void handleUDPComplete(UDPSessionEvent event)
        throws MPipeException
    {
        IPSession s = event.session();
        NatAttachment na = (NatAttachment)s.attachment();
        if (na != null) {
            LogEvent eventToLog = na.eventToLog();
            if (eventToLog != null) {
                transform.log(eventToLog);
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
            NatAttachment attachment = (NatAttachment)udpsession.attachment();
            int pid = udpsession.icmpId();
            int releasePid;

            if ( attachment == null ) {
                logger.error( "null attachment on Natd session" );
                return;
            }

            releasePid = attachment.releasePort();

            if ( pid != releasePid ) {
                logger.error( "Mismatch on the attached port and the session port " +
                              pid + "!=" + releasePid );
                return;
            }

            if ( releasePid != 0 ) {
                if (logger.isDebugEnabled()) {
                    logger.debug( "ICMP: Releasing pid: " + releasePid );
                }

                icmpPidList.releasePort( releasePid );
            } else if (logger.isDebugEnabled()) {
                logger.debug( "Ignoring non-natted pid: " + pid );
            }

        } else {
            cleanupSession( Protocol.UDP, udpsession );
        }
    }

    void configure( NetworkSpacesInternalSettings settings )
        throws TransformException
    {
        /* !!!! This is going to have to be updated */
        // IPMatcher localHostMatcher = IPMatcherFactory.getInstance().getLocalMatcher();
        
        ArgonManager argonManager = MvvmContextFactory.context().argonManager();
        NetworkManager networkManager = MvvmContextFactory.context().networkManager();

        /* Create a new override */
        List<InterfaceRedirect> overrideList = new LinkedList<InterfaceRedirect>();
        
        /* Create a list of the NAT Matchers */
        List<NatMatcher> natMatchers = new LinkedList<NatMatcher>();

        /* Create a list of the DMZ host matchers */
        List<DmzMatcher> dmzHostMatchers = new LinkedList<DmzMatcher>();

        /* First deal with all of the NATd spaces */
        for ( NetworkSpaceInternal space : settings.getNetworkSpaceList()) {
            if ( !space.getIsNatEnabled()) continue;

            /* Create a NAT matcher for this space */
            for ( IPNetwork networkRule : (List<IPNetwork>)space.getNetworkList()) {
                natMatchers.add( NatMatcher.makeNatMatcher( networkRule, space ));
                isNatEnabled = true;
            }
            
            if ( space.getIsDmzHostEnabled()) {
                dmzHostMatchers.add( DmzMatcher.makeDmzMatcher( space ));
            }
        }
        
        /* Save all of the objects at once */
        this.natMatchers     = natMatchers;
        this.dmzHostMatchers = dmzHostMatchers;

        /* Empty out the list */
        List<RedirectMatcher> redirectMatcherList = new LinkedList<RedirectMatcher>();

        int index = 1;
        /* Update all of the rules */
        for ( RedirectInternal internal : settings.getRedirectList()) {
            if ( internal.getIsEnabled()) {
                RedirectMatcher redirect = new RedirectMatcher( internal );
                redirectMatcherList.add( redirect );
                
                overrideList.add( makeInterfaceAddressRedirect( internal ));
            }
        }

        /* This override has to go after the rules */
//         if ( settings.getDmzEnabled() || settings.getNatEnabled()) {
//             /* Create a new redirect to redirect all traffic destined
//              * to the outside interface to the inside.  This handles sessions
//              * both sessions that are managed for FTP and sessions that are for
//              * the DMZ. */
//             InterfaceRedirect redirect =
//                 new InterfaceStaticRedirect( ProtocolMatcher.MATCHER_ALL,
//                                              IntfMatcher.getNotInside(), IntfMatcher.getAll(),
//                                              IPMatcher.MATCHER_ALL, localHostMatcher,
//                                              PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
//                                              IntfConstants.INTERNAL_INTF );

//             overrideList.add( redirect );
//         }
        
        /* Set the redirect list at the end(avoid concurrency issues) */
        this.redirectList = redirectMatcherList;
        argonManager.setInterfaceOverrideList( overrideList );
        networkManager.subscribeLocalOutside( true );
    }

    /* Not sure which this should ever throw an exception */
    void deconfigure()
    {
        MvvmContextFactory.context().networkManager().subscribeLocalOutside( false );
    }

    /**
     * Determine if a session is natted, and if necessary, rewrite its session information.
     */
    private boolean handleNat( IPNewSessionRequest request, Protocol protocol )
        throws MPipeException, NatUnconfiguredException
    {
        int port;
        
        boolean isNat = false;
        NatMatcher natMatcher = null;
        
        for ( NatMatcher matcher : natMatchers ) {
            logger.debug( "testing nat matcher" );            

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
            if (localAddr == null) throw NatUnconfiguredException.getInstance();

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

            ((NatAttachment)request.attachment()).releasePort( port );

            /* Increment the NAT counter */
            transform.incrementCount( NAT_COUNTER ); // NAT COUNTER

            /* Log the stat */
            transform.statisticManager.incrNatSessions();

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
        if ( transform.getSessionManager().isSessionRedirect( request, protocol )) {
            /* Increment the NAT counter */
            transform.incrementCount( REDIR_COUNTER );

            return true;
        }

        for ( Iterator<RedirectMatcher> iter = redirectList.iterator(); iter.hasNext(); ) {
            RedirectMatcher matcher = iter.next();

            if ( matcher.isMatch( request, protocol )) {
                /* Redirect the session */
                matcher.redirect( request );

                /* Increment the NAT counter */
                transform.incrementCount( REDIR_COUNTER );

                /* log the event if necessary */
                if ( matcher.getIsLoggingEnabled()) {
                    NatAttachment attachment = (NatAttachment)request.attachment();
                    if ( attachment == null ) {
                        logger.error( "null attachment to a NAT session" );
                    } else {
                        attachment.eventToLog(new RedirectEvent( request.pipelineEndpoints(), 
                                                                 matcher.ruleIndex()));
                    }
                }

                /* Log the stat */
                transform.statisticManager.incrRedirect( protocol, request );

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
        
        if ( dmzHost.isMatch( request, protocol )) {
            dmzHost.redirect( request );

            /* Increment the DMZ counter */
            transform.incrementCount( DMZ_COUNTER ); // DMZ COUNTER

            if ( isDmzLoggingEnabled ) {
                NatAttachment attachment = (NatAttachment)request.attachment();
                if ( attachment == null ) {
                    logger.error( "null attachment to a NAT session" );
                } else {
                    attachment.eventToLog(new RedirectEvent( request.pipelineEndpoints()));
                }
            }

            transform.statisticManager.incrDmzSessions();

            return true;
        }
        return false;
    }

    private boolean isDmz( IPNewSessionRequest request, Protocol protocol )
    {
        byte clientIntf = request.clientIntf();
        byte serverIntf = request.serverIntf();

        if (( clientIntf == IntfConstants.DMZ_INTF && serverIntf == IntfConstants.EXTERNAL_INTF ) ||
            ( clientIntf == IntfConstants.EXTERNAL_INTF && serverIntf == IntfConstants.DMZ_INTF )) {
            return true;
        }

        return false;
    }

    private boolean isVpn( IPNewSessionRequest request, Protocol protocol )
    {
        if ( request.clientIntf() == IntfConstants.VPN_INTF ||
             request.serverIntf() == IntfConstants.VPN_INTF ) {
            return true;
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
     * Utility function for NatSessionManager.
     */
    void releasePort( Protocol protocol, int port )
    {
        getPortList( protocol ).releasePort( port );
    }

    /**
     * Cleanup any of the information associated with a UDP or TCP session.
     * Presently not implemented to handle ICMP sessions.
     */
    private void cleanupSession( Protocol protocol, IPSession session )
    {
        NatAttachment attachment = (NatAttachment)session.attachment();

        if ( attachment == null ) {
            logger.error( "null attachment on Natd session" );
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

            transform.getSessionManager().releaseSession( session, protocol );
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

class NatMatcher
{
    private final RedirectMatcher matcher;
    private final NetworkSpaceInternal space;
    private InetAddress natAddress;

    NatMatcher( RedirectMatcher matcher, NetworkSpaceInternal space, InetAddress natAddress )
    {
        this.matcher    = matcher;
        this.space      = space;
        this.natAddress = natAddress;
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

    void setNatAddress( InetAddress newValue )
    {
        this.natAddress = newValue;
    }

    boolean isMatch( IPNewSessionRequest request, Protocol protocol )
    {

        boolean isMatch = this.matcher.isMatch( request, protocol );

        // System.out.println( "" + this.matcher.isEnabled() + "," + this.matcher.isMatchProtocol( protocol ));
        // System.out.println( "address" + this.matcher.isMatchAddress( request.clientAddr(), request.serverAddr()));
        // System.out.println( "port" + this.matcher.isMatchPort( request.clientPort(), request.serverPort()));
        System.out.println( "intf" + this.matcher.isMatchIntf( request.clientIntf(), request.serverIntf()));
                    
        return isMatch;
        
    }
    
    /* Create a matcher for handling traffic to be NATd */
    static NatMatcher makeNatMatcher( IPNetwork network, NetworkSpaceInternal space )
        throws TransformException
    {
        IntfMatcherFactory intfMatcherFactory = IntfMatcherFactory.getInstance();
        IPMatcherFactory imf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();

        IPMatcher clientIPMatcher = imf.makeSubnetMatcher( network.getNetwork(), network.getNetmask());
        
        List<InterfaceInternal> interfaceList = space.getInterfaceList();
        byte intfArray[] = new byte[interfaceList.size()];
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) intfArray[c++] = intf.getArgonIntf();

        
        /* XXX !!! RBS, i think the server interface should not be all */
        IntfMatcher clientIntfMatcher, serverIntfMatcher;
        try {
            clientIntfMatcher = intfMatcherFactory.makeSetMatcher( intfArray );
            
            /* All interfaces but VPN */
            serverIntfMatcher = intfMatcherFactory.makeInverseMatcher( IntfConstants.VPN_INTF );
            // serverIntfMatcher = intfMatcherFactory.getAllMatcher();
        } catch ( ParseException e ) {
            throw new TransformException( "Unable to create the client or server interface matcher " +
                                          "for a NAT matcher", e );
        }

        System.out.println( "client: " + clientIPMatcher + " server: " + serverIntfMatcher );
        RedirectMatcher matcher = new RedirectMatcher( true, false,  ProtocolMatcher.MATCHER_ALL,
                                                       clientIntfMatcher, serverIntfMatcher,
                                                       clientIPMatcher, imf.getAllMatcher(),
                                                       pmf.getAllMatcher(), pmf.getAllMatcher(),
                                                       false, null, -1 );

        return new NatMatcher( matcher, space, space.getNatAddress().getAddr());
    }
}

class DmzMatcher
{
    private final RedirectMatcher matcher;
    private final NetworkSpaceInternal space;

    DmzMatcher( RedirectMatcher matcher, NetworkSpaceInternal space )
    {
        this.matcher = matcher;
        this.space   = space;
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

    void redirect( IPNewSessionRequest request )
    {
        matcher.redirect( request );
    }

    /* Create a matcher for handling DMZ traffic */
    static DmzMatcher makeDmzMatcher( NetworkSpaceInternal space ) throws TransformException
    {
        IntfMatcherFactory intfMatcherFactory = IntfMatcherFactory.getInstance();
        IPMatcherFactory imf = IPMatcherFactory.getInstance();
        PortMatcherFactory pmf = PortMatcherFactory.getInstance();

        IPMatcher serverIPMatcher = imf.makeSingleMatcher( space.getNatAddress());

        List<InterfaceInternal> interfaceList = space.getInterfaceList();
        byte intfArray[] = new byte[interfaceList.size()];
        int c = 0;
        for ( InterfaceInternal intf : interfaceList ) intfArray[c] = intf.getArgonIntf();

        IntfMatcher clientIntfMatcher, serverIntfMatcher;

        try {
            clientIntfMatcher = intfMatcherFactory.makeInverseMatcher( intfArray );
            serverIntfMatcher = intfMatcherFactory.makeSetMatcher( intfArray );
        } catch ( ParseException e ) {
            throw new TransformException( "Unable to create the interface matchers", e );
        }

        InetAddress dmzHost = null;

        /* XXX !!!! if this is null, this shouldn't create a DMZ matcher */
        if (( space.getDmzHost() != null ) && !space.getDmzHost().isEmpty()) {
            dmzHost = space.getDmzHost().getAddr();
        }
        
        RedirectMatcher matcher = new RedirectMatcher( true, space.getIsDmzHostLoggingEnabled(),
                                                       ProtocolMatcher.MATCHER_ALL,
                                                       clientIntfMatcher, serverIntfMatcher,
                                                       imf.getAllMatcher(), serverIPMatcher,
                                                       pmf.getAllMatcher(), pmf.getAllMatcher(),
                                                       false, dmzHost, -1 );
        
        return new DmzMatcher( matcher, space );
    }
}
