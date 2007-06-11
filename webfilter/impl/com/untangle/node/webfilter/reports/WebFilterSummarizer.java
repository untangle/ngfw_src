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

package com.untangle.node.webfilter.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class WebFilterSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public WebFilterSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long hitCount = 0l;
        long logCount = 0l; // logged (passed and blocked) visits
        long webTraffic = 0l; // filtered web traffic (to other nodes)

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT COUNT(*) FROM n_http_evt_req WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            hitCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_webfilter_evt_blk WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            logCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(p2s_bytes) FROM n_http_evt_req AS evt, n_http_req_line AS line, pl_stats AS stats WHERE evt.time_stamp >= ? AND evt.time_stamp < ? AND evt.request_id = line.request_id AND line.pl_endp_id = stats.pl_endp_id";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            webTraffic = rs.getLong(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Filtered web traffic", Util.trimNumber("Bytes",webTraffic));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Scanned web visits", Util.trimNumber("",hitCount));
        addEntry("&nbsp;&nbsp;&nbsp;Logged web visits", Util.trimNumber("",logCount), Util.percentNumber(logCount,hitCount));
        addEntry("&nbsp;&nbsp;&nbsp;Passed web visits", Util.trimNumber("",hitCount-logCount), Util.percentNumber(hitCount-logCount,hitCount));

        // XXXX
        String tranName = "Web Filter";

        return summarizeEntries(tranName);
    }
}
