/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.node.ids;

import com.untangle.uvm.logging.EventLoggerFactory;
import com.untangle.uvm.logging.StatisticEvent;
import com.untangle.uvm.node.StatisticManager;
import com.untangle.uvm.node.NodeContext;

class IDSStatisticManager extends StatisticManager {

    /* Interface matcher to determine if the sessions is incoming or outgoing */
    //final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    //final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;

    private IDSStatisticEvent statisticEvent = new IDSStatisticEvent();

    public IDSStatisticManager(NodeContext tctx) {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent() {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent() {
        return ( this.statisticEvent = new IDSStatisticEvent());
    }

    void incrDNC() {
        this.statisticEvent.incrDNC();
    }

    void incrLogged() {
        this.statisticEvent.incrLogged();
    }

    void incrBlocked() {
        this.statisticEvent.incrBlocked();
    }
}
