/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm;

public interface ReportingManager  {
    /**
     * <code>isReportingEnabled</code> returns true if reporting is enabled, that is if reports
     * will be generated nightly.  Currently this is the same thing as "is the reporting transform
     * installed and turned on."
     *
     * @return a <code>boolean</code> true if reporting is enabled
     */
    boolean isReportingEnabled();

    /**
     * <code>isReportsAvailable</code> returns true if reporting is enabled and reports have been
     * generated and are ready to view.  Currently this is the same thing as "does the current symlink
     * exist and contain a valid reporting-transform/sum-daily.html file."
     *
     * @return a <code>boolean</code> true if reports are available
     */
    boolean isReportsAvailable();
}
