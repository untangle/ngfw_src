/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: $
 */
package com.metavize.tran.spyware;

import com.metavize.mvvm.logging.EventLogger;
import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.tran.StatisticManager;
import com.metavize.mvvm.tran.TransformContext;

class SpywareStatisticManager extends StatisticManager {

    private SpywareStatisticEvent statisticEvent = new SpywareStatisticEvent();

    public SpywareStatisticManager(TransformContext tctx) {
        super(new EventLogger(tctx));
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
