/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EventHandler.java,v 1.7 2005/03/15 02:11:52 amread Exp $
 */

package com.metavize.tran.nat;

// import java.nio.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.metavize.mvvm.tapi.Protocol;

import com.metavize.mvvm.MvvmContextFactory;

import com.metavize.mvvm.tapi.AbstractEventHandler;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.MPipeException;

import com.metavize.mvvm.tapi.event.TCPNewSessionRequestEvent;
import com.metavize.mvvm.tapi.event.UDPNewSessionRequestEvent;

import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class NatEventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(NatEventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    /* match to determine whether a session is natted */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher nat;

    /* match to determine  whether a session is directed for the dmz */
    /* XXX Probably need to initialized this with a value */
    private RedirectMatcher dmz;

    /* All of the other rules */
    /* Use an empty list rather than null */
    private List<RedirectMatcher> redirectList = new LinkedList<RedirectMatcher>();

    /* tracks the open TCP ports for NAT */
    private final PortList tcpPortList;
    
    /* Tracks the open UDP ports for NAT */
    private final PortList udpPortList;

    
    /* Setup, singleton  */
    NatEventHandler()
    {
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

    public void handleNewSessionRequest( IPNewSessionRequest request, Protocol protocol )
    {
        /* Check for NAT, Redirects or DMZ */
        if ( isNat(  request, protocol ) ||
             isRedirect(  request, protocol ) || 
             isDmz(  request,  protocol )) {
            request.release();
            return;
        }
        
        /* If nat is on, and this session wasn't natted, redirected or dmzed, it
         * must be rejected */
        if ( nat.isEnabled()) {
            /* XXX How should the session be rejected */
            request.rejectSilently();
            return;
        } 
        
        /* Otherwise release the session */
        request.release();
    }

    RedirectMatcher getNat() {
        return nat;
    }
    
    void setNat( RedirectMatcher nat ) {
        this.nat = nat;
    }

    RedirectMatcher getDmz() {
        return dmz;
    }
    
    void setDmz( RedirectMatcher dmz ) {
        this.dmz = dmz;
    }

    List <RedirectMatcher> getRedirectList() {
        return redirectList;
    }
    
    void setRedirectList( List<RedirectMatcher>redirectList ) {
        this.redirectList = redirectList;
    }

    /**
     * Determine if a session is natted, and if necessary, rewrite its session information.
     */
    private boolean isNat( IPNewSessionRequest request, Protocol protocol )
    {
        if ( nat.isMatch( request, protocol )) {
            /* Change the source in the request */
            nat.redirect( request );
            
            /* XXX What about the case where you have NAT and redirect */
            /* XXX Possibly check for redirects here */
            return true;
        }

        return false;
    }
    
    /**
     * Determine if a session is redirected, and if necessary, rewrite its session information.
     */
    private boolean isRedirect( IPNewSessionRequest request, Protocol protocol )
    {
        for ( Iterator<RedirectMatcher> iter = redirectList.iterator(); iter.hasNext(); ) {
            RedirectMatcher matcher = iter.next();
            
            if ( matcher.isMatch( request, protocol )) {
                /* Redirect the session */
                matcher.redirect( request );
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
            return true;
        }
        return false;
    }

    
    
}
