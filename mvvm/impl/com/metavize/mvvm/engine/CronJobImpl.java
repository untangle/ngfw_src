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

package com.metavize.mvvm.engine;

import java.util.TimerTask;

import com.metavize.mvvm.CronJob;
import com.metavize.mvvm.Period;

class CronJobImpl implements CronJob
{
    private final CronManager cronManager;
    private final Runnable runnable;

    private volatile Period period;
    private volatile TimerTask task;

    CronJobImpl(CronManager cronManager, Runnable runnable, Period period)
    {
        this.cronManager = cronManager;
        this.runnable = runnable;
        this.period = period;
    }

    // CronJob methods --------------------------------------------------------

    public void cancel()
    {
        cronManager.cancel(this);
    }

    public void reschedule(Period period)
    {
        this.period = period;
        cronManager.reschedule(this);
    }

    public long scheduledExecutionTime()
    {
        TimerTask t = task;
        return null == t ? -1L : t.scheduledExecutionTime();
    }

    // package private methods ------------------------------------------------

    Runnable getRunnable()
    {
        return runnable;
    }

    TimerTask getTask()
    {
        return task;
    }

    void setTask(TimerTask task)
    {
        this.task = task;
    }

    Period getPeriod()
    {
        return period;
    }
}
