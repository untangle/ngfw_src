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

import java.util.TimerTask;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.Period;

/**
 * Implements <code>CronJob</code>.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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
