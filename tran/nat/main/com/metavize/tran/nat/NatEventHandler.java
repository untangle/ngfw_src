/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.InetAddress;

import com.metavize.mvvm.tapi.Protocol;

import com.metavize.mvvm.NetworkingConfiguration;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.ArgonManager;

import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.UDPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;

import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPSessionEvent;
import com.metavize.mvvm.tapi.event.TCPSessionEvent;

import com.metavize.mvvm.tapi.IPSession;
import com.metavize.mvvm.tapi.UDPSession;

import com.metavize.mvvm.tran.Transform;
import com.metavize.mvvm.tran.TransformException;
import com.metavize.mvvm.argon.ArgonException;

import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.IPaddr;
import com.metavize.mvvm.tran.firewall.ProtocolMatcher;
import com.metavize.mvvm.tran.firewall.IPMatcher;
import com.metavize.mvvm.tran.firewall.PortMatcher;
import com.metavize.mvvm.tran.firewall.IntfMatcher;

/* Import all of the constants from NatConstants (1.5 feature) */
import static com.metavize.tran.nat.NatConstants.*;

class NatEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(NatEventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    /* Number of milliseconds to wait in between updating the address and updating the mvvm */
    private static final int SLEEP_TIME = 1000;
    
    /* match to determine whether a session is natted */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher nat;
    private IPMatcher natLocalNetwork;

    /* match to determine  whether a session is directed for the dmz */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher dmz = RedirectMatcher.MATCHER_DISABLED;
    
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

    /* The internal address */
    private IPaddr internalAddress;
    private IPaddr internalSubnet;
    
    /* Nat Transform */
    private final NatImpl transform;

    /* NatStatisticManager */
    private final NatStatisticManager statisticManager;
    
    /* Setup  */
    NatEventHandler( NatImpl transform )
    {
        tcpPortList = PortList.makePortList( TCP_NAT_PORT_START, TCP_NAT_PORT_END );
        udpPortList = PortList.makePortList( UDP_NAT_PORT_START, UDP_NAT_PORT_END );
        icmpPidList = PortList.makePortList( ICMP_PID_START, ICMP_PID_END );
        this.transform = transform;
        this.statisticManager = NatStatisticManager.getInstance();
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
            if ( isNat(  request, protocol ) ||
                 isRedirect( request, protocol ) || 
                 isDmz(  request,  protocol )) {
                request.release( true );
                
                if ( isFtp( request, protocol )) {
                    transform.getSessionManager().registerSession( request, protocol,
                                                                   clientAddr, clientPort,
                                                                   serverAddr, serverPort );
                    
                    attachment.isManagedSession( true );
                }
                return;
            }
            
            /* If nat is on, and this session wasn't natted, redirected or dmzed, it
             * must be rejected */
            if ( nat.isEnabled()) {
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
                logger.debug( "ICMP: Releasing pid: " + releasePid );
                
                icmpPidList.releasePort( releasePid );
            } else {
                logger.debug( "Ignoring non-natted pid: " + pid );
            }

        } else {
            cleanupSession( Protocol.UDP, udpsession );
        }
    }
    
    void configure( NatSettings settings, NetworkingConfiguration netConfig ) throws TransformException
    {
        IPMatcher localHostMatcher = IPMatcher.MATCHER_LOCAL;

        if ( settings.getNatEnabled()) {
            internalAddress = settings.getNatInternalAddress();
            internalSubnet = settings.getNatInternalSubnet();

            /* Create the nat redirect */
            natLocalNetwork = new IPMatcher( internalAddress, internalSubnet, false );

            /* XXX Update to use local host */
            nat = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                       IntfMatcher.MATCHER_IN, IntfMatcher.MATCHER_ALL,
                                       natLocalNetwork, IPMatcher.MATCHER_ALL, 
                                       PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                       false, null, -1 );
            
            enableNat( netConfig );
        } else {
            nat = RedirectMatcher.MATCHER_DISABLED;
            disableNat( netConfig );
        }
        
        /* Configure the DMZ */
        if ( settings.getDmzEnabled()) {
            dmz = new RedirectMatcher( true, ProtocolMatcher.MATCHER_ALL,
                                       IntfMatcher.MATCHER_OUT, IntfMatcher.MATCHER_ALL,
                                       IPMatcher.MATCHER_ALL, localHostMatcher,
                                       PortMatcher.MATCHER_ALL, PortMatcher.MATCHER_ALL,
                                       true, settings.getDmzAddress().getAddr(), -1 );
            isDmzLoggingEnabled = settings.getDmzLoggingEnabled();
        } else {
            dmz = RedirectMatcher.MATCHER_DISABLED;
            isDmzLoggingEnabled = false;
        }
        
        /* Empty out the list */
        redirectList.clear();

        List<RedirectRule> list = (List<RedirectRule>)settings.getRedirectList();

        if ( list == null ) {
            logger.error( "Settings contain null redirect list" );
        } else {
            int index =1;
            /* Update all of the rules */
            for ( Iterator<RedirectRule> iter = list.iterator() ; iter.hasNext() ; index++ ) {
                redirectList.add( new RedirectMatcher( iter.next(), index ));
            }
        }        
    }

    void deconfigure() throws TransformException
    {
        /* Bring down NAT */
        disableNat( MvvmContextFactory.context().networkingManager().get());
    }


    RedirectMatcher getNat()
    {
        return nat;
    }
    

    RedirectMatcher getDmz()
    {
        return dmz;
    }
    
    List <RedirectMatcher> getRedirectList()
    {
        return redirectList;
    }
    
    /**
     * Determine if a session is natted, and if necessary, rewrite its session information.
     */
    private boolean isNat( IPNewSessionRequest request, Protocol protocol ) 
        throws MPipeException, NatUnconfiguredException
    {
        int port;
                
        if ( nat.isMatch( request, protocol )) {
            /* Check to see if this is destined to the NATd network, if it is drop it */
            if ( natLocalNetwork.isMatch( request.serverAddr())) {
                return false;
            }
            
            /* Check to see if this is redirect, check before changing the source address */
            isRedirect(  request, protocol );

            /* Change the source in the request */
            /* All redirecting occurs here */
            InetAddress localAddr = IPMatcher.getOutsideAddress();
            // Unfortunately, as of 5/02/05, this can sometimes be null, probably due to
            // initialization order, the sleeping involved, etc.  For now, just make sure
            // not to ever change the client addr to null, which causes awful things to happen.  jdi XXX
            if (localAddr == null) throw NatUnconfiguredException.getInstance();

            request.clientAddr(localAddr);
            
            /* Set the client port */
            /* XXX THIS IS A HACK, it really should check if the protocol is ICMP, but
             * for now there are only UDP sessions */
            if ( request.clientPort() == 0 && request.serverPort() == 0 ) {
                port = icmpPidList.getNextPort();
                ((UDPNewSessionRequest)request).icmpId( port );
                logger.debug( "Redirect PING session to id: " + port );
            } else {
                port = getNextPort( protocol );
                request.clientPort( port );
                
                logger.debug( "Redirecting session to port: " + port );
            }
            
            ((NatAttachment)request.attachment()).releasePort( port );

            /* Increment the NAT counter */
            transform.incrementCount( NAT_COUNTER ); // NAT COUNTER

            /* Log the stat */
            statisticManager.incrNatSessions();
            
            return true;
        }

        return false;
    }

    /**
     * Determine if a session is redirected, and if necessary, rewrite its session information.
     */
    private boolean isRedirect( IPNewSessionRequest request, Protocol protocol ) throws MPipeException
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
                if ( matcher.rule() == null ) {
                    logger.warn( "Null rule for a redirect matcher" );
                } else if ( matcher.rule().getLog()) {
                    eventLogger.info( new RedirectEvent( request.id(), matcher.rule(), matcher.ruleIndex()));
                }
                
                /* Log the stat */
                statisticManager.incrRedirect( protocol, request );

                return true;
            }
        }
        return false;
    }

    /**
     * Determine if a session is for the DMZ, and if necessary, rewrite its session information.
     */
    private boolean isDmz( IPNewSessionRequest request, Protocol protocol )
    {
        if ( dmz.isMatch( request, protocol )) {
            dmz.redirect( request );
            
            /* Increment the DMZ counter */
            transform.incrementCount( DMZ_COUNTER ); // DMZ COUNTER

            if ( isDmzLoggingEnabled ) {
                /* Log the event if necessary */
                eventLogger.info( new RedirectEvent( request.id()));
            }

            statisticManager.incrDmzSessions();

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

            logger.debug( "Releasing port: " + releasePort );
            
            getPortList( protocol ).releasePort( releasePort );
        } else {
            logger.debug( "Ignoring non-natted port: " + session.clientPort() + "/" + session.serverPort());
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

    private void enableNat( NetworkingConfiguration netConfig ) throws TransformException
    {
        try {
            ArgonManager argonManager = MvvmContextFactory.context().argonManager();
            /* Local antisubscribe is only used in non-production environments, 
             * in production environments it is never antisubscribed */
            argonManager.disableLocalAntisubscribe();
            argonManager.destroyBridge( netConfig, internalAddress.getAddr(), internalSubnet.getAddr());
        } catch ( Exception e ) {
            logger.error( "error while reconfiguring interface" );
            throw new TransformException( "unable to reconfiguring interface", e );
        }
    }

    private boolean isFtp( IPNewSessionRequest request, Protocol protocol )
    {
        if (( protocol == Protocol.TCP ) && ( request.serverPort() == FTP_SERVER_PORT )) {
            return true;
        }
        
        return false;
    }
   
    private void disableNat( NetworkingConfiguration netConfig ) throws TransformException
    {
        /* Wait for this to finish */
        try {
            ArgonManager argonManager = MvvmContextFactory.context().argonManager();
            /* Local antisubscribe is only used in non-production environments, 
             * in production environments it is never antisubscribed */
            argonManager.enableLocalAntisubscribe();
            argonManager.restoreBridge( netConfig );
        } catch ( Exception e ) {
            logger.error( "Interrupting while reconfiguring interface" );
            throw new TransformException( "Interrupting while reconfiguring interface", e );
        }        
    }
}

class NatAttachment
{    
    /* True if this session uses a port that must be released */
    /* Port to release, 0, if a port should not be released */
    private int releasePort = 0;
    
    /* True if this session has created a session that must be removed from the session
     * manager.  (Presently the session manager only manages ftp sessions) */
    private boolean isManagedSession = false;
    
    NatAttachment()
    {
    }

    boolean isManagedSession()
    {
        return this.isManagedSession;
    }

    void isManagedSession( boolean isManagedSession )
    {
        this.isManagedSession = isManagedSession;
    }

    int releasePort()
    {
        return this.releasePort;
    }

    void releasePort( int releasePort )
    {
        this.releasePort = releasePort;
    }
}

/* Just used to indicate that the outside interface is not configured, it happens in one very well defined
 * case, hence it doesn't need a message 
 */
class NatUnconfiguredException extends Exception {
    private static NatUnconfiguredException INSTANCE = new NatUnconfiguredException();
    
    NatUnconfiguredException()
    {
        super();
    }

    static NatUnconfiguredException getInstance() {
        return INSTANCE;
    }
}

