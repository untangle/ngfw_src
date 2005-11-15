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


import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tran.StatisticManager;
import com.metavize.mvvm.tran.TransformContext;

class VpnStatisticManager extends StatisticManager
{
    private VpnStatisticEvent statisticEvent = new VpnStatisticEvent();

    private List<ClientDistributionEvent> clientDistributionList = new LinkedList<ClientDistributionEvent>();

    VpnStatisticManager(TransformContext tctx)
    {
        super(new EventLogger(tctx));
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

        for ( ClientDistributionEvent event : clientDistributionList ) eventLogger.log( event );

        return ( this.statisticEvent = new VpnStatisticEvent());
    }

    void addClientDistributionEvent( ClientDistributionEvent event )
    {
        this.clientDistributionList.add( event );
        this.statisticEvent.setHasStatistics();
    }
}
