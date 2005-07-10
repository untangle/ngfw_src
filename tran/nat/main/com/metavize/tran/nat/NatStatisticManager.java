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

import org.apache.log4j.Logger;

import com.metavize.mvvm.tran.StatisticManager;
import com.metavize.mvvm.logging.StatisticEvent;

import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tapi.IPNewSessionRequest;

import com.metavize.mvvm.tran.firewall.IntfMatcher;

class NatStatisticManager extends StatisticManager
{    
    private static NatStatisticManager INSTANCE = null;
    
    /* Interface matcher to determine if the sessions is incoming or outgoing */
    final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;
        
    private NatStatisticEvent statisticEvent = new NatStatisticEvent();
    
    private NatStatisticManager()
    {
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return ( this.statisticEvent = new NatStatisticEvent());
    }
    
    void incrNatSessions()
    {
        this.statisticEvent.incrNatSessions();
    }

    void incrRedirect( Protocol protocol, IPNewSessionRequest request )
    {
        boolean isOutgoing = matcherIncoming.isMatch( request.clientIntf());

        if ( protocol == Protocol.TCP ) {
            if ( isOutgoing ) incrTcpOutgoingRedirects();
            else              incrTcpIncomingRedirects();
        } else {
            if (( request.clientPort() == 0 ) && ( request.serverPort() == 0 )) {
                /* Ping Sessions */
                if ( isOutgoing ) incrIcmpOutgoingRedirects();
                else              incrIcmpIncomingRedirects();
            } else {
                /* UDP Sessions */
                if ( isOutgoing ) incrUdpOutgoingRedirects();
                else              incrUdpIncomingRedirects();
            }
        }
    }

    void incrTcpIncomingRedirects()
    {
        this.statisticEvent.incrTcpIncomingRedirects();
    }

    void incrTcpOutgoingRedirects()
    {
        this.statisticEvent.incrTcpOutgoingRedirects();
    }

    void incrUdpIncomingRedirects()
    {
        this.statisticEvent.incrUdpIncomingRedirects();
    }

    void incrUdpOutgoingRedirects()
    {
        this.statisticEvent.incrUdpOutgoingRedirects();
    }

    void incrIcmpIncomingRedirects()
    {
        this.statisticEvent.incrIcmpIncomingRedirects();
    }

    void incrIcmpOutgoingRedirects()
    {
        this.statisticEvent.incrIcmpOutgoingRedirects();
    }

    void incrDmzSessions()
    {
        this.statisticEvent.incrDmzSessions();
    }

    static synchronized NatStatisticManager getInstance()
    {
        if ( INSTANCE == null ) 
            INSTANCE = new NatStatisticManager();

        return INSTANCE;
    }
}
