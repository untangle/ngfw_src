/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ReportSummarizer.java,v 1.1 2005/02/11 22:45:33 jdi Exp $
 */

package com.metavize.mvvm.reporting;

import java.sql.Connection;
import java.sql.Timestamp;

/**
 * Each transform implements this in a class it provides for report time generation
 *
 * @author
 * @version 1.0
 */
public interface ReportSummarizer
{
    /**
     * Get the summary report for the transform, from the given database starting at the
     * start time and extending to the end time.
     *
     * @param conn a <code>Connection</code> that should be used for queries
     * @param startDate a <code>Timestamp</code> giving the start date
     * @param endDate a <code>Timestamp</code> giving the end date
     * @return a <code>String</code> which should contain proper HTML summarizing the situation over the given time interval
     */
    String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate);
}
