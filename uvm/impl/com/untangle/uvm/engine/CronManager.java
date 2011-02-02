/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.Period;

/**
 * Schedules and runs tasks at a given <code>Period</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
        if (null != next) {
            timer.schedule(task, next.getTime());
        }

        return cronJob;
    }
}
