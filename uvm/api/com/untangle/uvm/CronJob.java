/**
 * $Id$
 */
package com.untangle.uvm;

import com.untangle.uvm.node.DayOfWeekMatcher;

/**
 * CronJob represents a job registered with the system. This object
 * allows the job to be canceled or rescheduled.
 */
public interface CronJob
{
    /**
     * Cancel this job.
     */
    void cancel();

    /**
     * Reschedule this job to a new period.
     *
     * @param p new period to run this job.
     */
    void reschedule(DayOfWeekMatcher days, int hour, int minute);

    /**
     * Get the next scheduled execution time.
     *
     * @return next execution time, measured in milliseconds since
     * January 1, 1970.
     */
    long scheduledExecutionTime();
}
