/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.TimerTask;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.node.DayOfWeekMatcher;

/**
 * Implements <code>CronJob</code>.
 */
class CronJobImpl implements CronJob
{
    private final CronManager cronManager;
    private final Runnable runnable;

    private DayOfWeekMatcher days;
    private int hour;
    private int minute;
    private volatile TimerTask task;

    public CronJobImpl( CronManager cronManager, Runnable runnable, DayOfWeekMatcher days, int hour, int minute )
    {
        this.cronManager = cronManager;
        this.runnable = runnable;
        this.days = days;
        this.hour = hour;
        this.minute = minute;
    }

    public void cancel()
    {
        cronManager.cancel(this);
    }

    public void reschedule( DayOfWeekMatcher days, int hour, int minute )
    {
        this.days = days;
        this.hour = hour;
        this.minute = minute;
        cronManager.reschedule(this);
    }

    public long scheduledExecutionTime()
    {
        TimerTask t = task;
        return null == t ? -1L : t.scheduledExecutionTime();
    }

    protected DayOfWeekMatcher getDays()
    {
        return days;
    }

    protected int getHour()
    {
        return hour;
    }

    protected int getMinute()
    {
        return minute;
    }
    
    protected Runnable getRunnable()
    {
        return runnable;
    }

    protected TimerTask getTask()
    {
        return task;
    }

    protected void setTask(TimerTask task)
    {
        this.task = task;
    }
}
