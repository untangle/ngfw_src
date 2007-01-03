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

package com.untangle.mvvm.engine;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import com.untangle.mvvm.CronJob;
import com.untangle.mvvm.Period;

class CronManager
{
    private final Timer timer = new Timer(true);

    // CronManager methods ----------------------------------------------------

    CronJob makeCronJob(Period p, final Runnable r)
    {
        final CronJobImpl cronJob = new CronJobImpl(this, r, p);
        return schedule(cronJob);
    }

    // package protected methods ----------------------------------------------

    void destroy()
    {
        timer.cancel();
    }

    void cancel(CronJobImpl cj)
    {
        TimerTask task = cj.getTask();
        cj.setTask(null);
        task.cancel();
    }

    void reschedule(CronJobImpl cj)
    {
        TimerTask task = cj.getTask();
        task.cancel();
        Period p = cj.getPeriod();
        schedule(cj);
    }

    // private methods --------------------------------------------------------

    private CronJob schedule(final CronJobImpl cronJob)
    {
        TimerTask task = cronJob.getTask();
        if (null != task) {
            task.cancel();
        }

        task = new TimerTask() {
                public void run()
                {
                    if (cronJob.getTask() == this) {
                        cronJob.getRunnable().run();
                        schedule(cronJob);
                    }
                }
            };

        cronJob.setTask(task);

        Calendar next = cronJob.getPeriod().nextTime();
        timer.schedule(task, next.getTime());

        return cronJob;
    }
}
