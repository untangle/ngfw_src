/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.ids;

import com.metavize.mvvm.logging.EventLoggerFactory;
import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tran.StatisticManager;
import com.metavize.mvvm.tran.TransformContext;

class IDSStatisticManager extends StatisticManager {

    /* Interface matcher to determine if the sessions is incoming or outgoing */
    //final IntfMatcher matcherIncoming = IntfMatcher.MATCHER_IN;
    //final IntfMatcher matcherOutgoing = IntfMatcher.MATCHER_OUT;

    private IDSStatisticEvent statisticEvent = new IDSStatisticEvent();

    public IDSStatisticManager(TransformContext tctx) {
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
