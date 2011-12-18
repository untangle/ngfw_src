/**
 * $Id$
 */
package com.untangle.node.ips;

import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.NodeContext;
import com.untangle.uvm.node.StatisticManager;

class IpsStatisticManager extends StatisticManager
{
    private IpsStatisticEvent statisticEvent = new IpsStatisticEvent();

    public IpsStatisticManager()
    {
        super();
    }

    protected StatisticEvent getInitialStatisticEvent()
    {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent()
    {
        return ( this.statisticEvent = new IpsStatisticEvent());
    }

    void incrDNC()
    {
        this.statisticEvent.incrDNC();
    }

    void incrLogged()
    {
        this.statisticEvent.incrLogged();
    }

    void incrBlocked()
    {
        this.statisticEvent.incrBlocked();
    }
}
