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

package com.untangle.mvvm;

/**
 * CronJob represents a job registered with the system. This object
 * allows the job to be canceled or rescheduled.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 * @see MvvmLocalContext#makeCronJob(Period, Runnable)
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
    void reschedule(Period p);

    /**
     * Get the next scheduled execution time.
     *
     * @return next execution time, measured in milliseconds since
     * January 1, 1970.
     */
    long scheduledExecutionTime();
}
