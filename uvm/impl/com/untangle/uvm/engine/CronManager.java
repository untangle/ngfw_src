/**
 * $Id: CronManager.java 32073 2012-06-06 20:53:24Z dmorris $
 */
package com.untangle.uvm.engine;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

import com.untangle.uvm.CronJob;
import com.untangle.uvm.node.DayOfWeekMatcher;

/**
 * Schedules and runs tasks at a given time
 */
class CronManager
{
    private final Timer timer = new Timer(true);

    // CronManager methods ----------------------------------------------------

    CronJob makeCronJob( DayOfWeekMatcher days, int hour, int minute, final Runnable r )
    {
        final CronJobImpl cronJob = new CronJobImpl(this, r, days, hour, minute);
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

        Calendar next = nextTime(cronJob.getDays(), cronJob.getHour(), cronJob.getMinute());
        if (null != next) {
            timer.schedule(task, next.getTime());
        }

        return cronJob;
    }

    private Calendar nextTime(DayOfWeekMatcher days, int hour, int minute)
    {
        Calendar c = Calendar.getInstance();

        if (c.get(Calendar.HOUR_OF_DAY) > hour) {
            c.add(Calendar.DAY_OF_WEEK, 1);
        }

        if (c.get(Calendar.HOUR_OF_DAY) == hour && c.get(Calendar.MINUTE) >= minute) {
            c.add(Calendar.DAY_OF_WEEK, 1);
        }

        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);

        for (int i = 0; i < 7; i++) {
            if (days.isMatch(c.get(Calendar.DAY_OF_WEEK))) {
                return c;
            } else {
                c.add(Calendar.DAY_OF_WEEK, 1);
            }
        }

        return null;
    }

}
