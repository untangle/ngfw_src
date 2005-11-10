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
package com.metavize.tran.openvpn;

import com.metavize.mvvm.MvvmContextFactory;

import java.util.List;
import java.util.LinkedList;

import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tapi.IPNewSessionRequest;
import com.metavize.mvvm.tapi.Protocol;
import com.metavize.mvvm.tran.StatisticManager;

class VpnStatisticManager extends StatisticManager
{
    private VpnStatisticEvent statisticEvent = new VpnStatisticEvent();

    private List<ClientDistributionEvent> clientDistributionList = new LinkedList<ClientDistributionEvent>();

    VpnStatisticManager()
    {
        super();
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        /* Log all of the client distribution events events */
        List<ClientDistributionEvent> clientDistributionList = this.clientDistributionList;
        this.clientDistributionList = new LinkedList<ClientDistributionEvent>();

        for ( ClientDistributionEvent event : clientDistributionList ) eventLogger.info( event );

        return ( this.statisticEvent = new VpnStatisticEvent());
    }

    void addClientDistributionEvent( ClientDistributionEvent event )
    {
        this.clientDistributionList.add( event );
        this.statisticEvent.setHasStatistics();
    }
}
