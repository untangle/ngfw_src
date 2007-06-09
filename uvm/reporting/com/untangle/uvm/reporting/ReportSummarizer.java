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

package com.untangle.uvm.reporting;

import java.sql.Connection;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Each node implements this in a class it provides for report time generation
 *
 * @author
 * @version 1.0
 */
public interface ReportSummarizer
{

    /**
     * Get the summary report for the node, from the given database starting at the
     * start time and extending to the end time.  Allows parameterization of the summarization.
     *
     * @param conn a <code>Connection</code> that should be used for queries
     * @param startDate a <code>Timestamp</code> giving the start date
     * @param endDate a <code>Timestamp</code> giving the end date
     * @param extraParams a <code>Map<String,Object></code> giving the extra params
     * @return a <code>String</code> which should contain proper HTML summarizing the situation over the given time interval
     */
    String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate, Map<String,Object> extraParams );

    /**
     * Get the summary report for the node, from the given database starting at the
     * start time and extending to the end time.
     *
     * @param conn a <code>Connection</code> that should be used for queries
     * @param startDate a <code>Timestamp</code> giving the start date
     * @param endDate a <code>Timestamp</code> giving the end date
     * @return a <code>String</code> which should contain proper HTML summarizing the situation over the given time interval
     */
    String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate);
}
