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
package com.untangle.tran.spyware;

import com.untangle.mvvm.logging.EventLoggerFactory;
import com.untangle.mvvm.logging.StatisticEvent;
import com.untangle.mvvm.tran.StatisticManager;
import com.untangle.mvvm.tran.TransformContext;

class SpywareStatisticManager extends StatisticManager {

    private SpywareStatisticEvent statisticEvent = new SpywareStatisticEvent();

    public SpywareStatisticManager(TransformContext tctx) {
        super(EventLoggerFactory.factory().getEventLogger(tctx));
    }

    protected StatisticEvent getInitialStatisticEvent() {
        return this.statisticEvent;
    }

    protected StatisticEvent getNewStatisticEvent() {
        return (this.statisticEvent = new SpywareStatisticEvent());
    }

    void incrPass() {
        this.statisticEvent.incrPass();
    }

    void incrCookie() {
        this.statisticEvent.incrCookie();
    }

    void incrActiveX() {
        this.statisticEvent.incrActiveX();
    }

    void incrURL() {
        this.statisticEvent.incrURL();
    }

    void incrSubnetAccess() {
        this.statisticEvent.incrSubnetAccess();
    }
}
