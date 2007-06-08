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

import java.util.Date;

/**
 * Describe interface <code>ReportingManager</code> here.
 *
 * @author <a href="mailto:jdi@untangle.com">John Irwin</a>
 * @version 1.0
 */
public interface ReportingManager  {
    /**
     * Tests if reporting is enabled, that is if reports will be
     * generated nightly.  Currently this is the same thing as "is the
     * reporting transform installed and turned on."
     *
     * @return true if reporting is enabled, false otherwise.
     */
    boolean isReportingEnabled();

    /**
     * Tests if reporting is enabled and reports have been generated
     * and are ready to view.  Currently this is the same thing as
     * "does the current symlink exist and contain a valid
     * reporting-transform/sum-daily.html file."
     *
     * @return true if reports are available
     */
    boolean isReportsAvailable();

    /**
     * Kicks off the reporting process.  It creates a settings.env
     * file to help in later shell script execution.  It also updates
     * the merged address map.  Finally it purges reports other than
     * <code>daysToKeep</code> days.
     *
     * Call this each time before calling startReports.
     *
     * If this is called while a report is running, the running report
     * is automatically terminated.
     *
     * @param outputBaseDir the base output directory
     * @param midnight the day to be reported on (previous 24 hours
     * from midnight of given Date)
     * @param daysToKeep number of days of data to keep
     */
    void prepareReports(String outputBaseDir, Date midnight, int daysToKeep)
        throws MvvmException;

    /**
     * Starts the report generation.  We return since the alternative
     * is mcli with a 6 hour timeout or something stupid like that.
     *
     * Call this after calling prepareReports to set the parameters.
     */
    void startReports() throws MvvmException;

    void stopReports() throws MvvmException;

    /**
     * Tests if the reports are running -- have been started
     * and have not yet finished.  Call this after
     * <code>startReports</code> if desired to see if they're done.
     *
     * @return true if reports still running, false otherwise.
     */
    boolean isRunning();
}
