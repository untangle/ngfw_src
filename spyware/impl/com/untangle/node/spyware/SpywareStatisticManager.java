/*
 * $Id$
 */
package com.untangle.node.spyware;

import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.StatisticManager;
import com.untangle.uvm.node.NodeContext;

class SpywareStatisticManager extends StatisticManager
{
    private SpywareStatisticEvent statisticEvent = new SpywareStatisticEvent();

    public SpywareStatisticManager(NodeContext tctx)
    {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return (this.statisticEvent = new SpywareStatisticEvent());
    }

    void incrPass()
    {
        this.statisticEvent.incrPass();
    }

    void incrCookie()
    {
        this.statisticEvent.incrCookie();
    }

    void incrURL()
    {
        this.statisticEvent.incrURL();
    }

    void incrSubnetAccess()
    {
        this.statisticEvent.incrSubnetAccess();
    }
}
