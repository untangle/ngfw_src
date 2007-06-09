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
package com.untangle.tran.ids;

import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.StatisticEvent;
import com.untangle.mvvm.tran.StatisticManager;
import com.untangle.mvvm.tran.TransformContext;

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
