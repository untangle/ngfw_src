/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class HttpBlockerSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(HttpBlockerSummarizer.class);

    public HttpBlockerSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long hitCount = 0l;
        long blockCount = 0l;
        long totalTraffic = 0l;

        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;

            sql = "SELECT count(*) FROM tr_http_evt_req WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            hitCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT count(*) FROM tr_httpblk_evt_blk WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT sum(c2p_bytes), sum(s2p_bytes), sum(p2c_bytes), sum(p2s_bytes) FROM tr_http_evt_req AS h, pl_stats AS p WHERE h.time_stamp >= ? AND h.time_stamp < ? AND h.session_id = p.session_id";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            long c2p = rs.getLong(1);
            long s2p = rs.getLong(2);
            long p2c = rs.getLong(3);
            long p2s = rs.getLong(4);
            totalTraffic += s2p;
            totalTraffic += p2s;
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Total web traffic", Util.trimNumber("Bytes",totalTraffic));

        addEntry("&nbsp;", "&nbsp;");

        addEntry("Total web hits", Util.trimNumber("",hitCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked hits", Util.trimNumber("",blockCount) + " (" + Util.percentNumber(blockCount,hitCount) + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed hits", Util.trimNumber("",hitCount-blockCount) + " (" + Util.percentNumber(hitCount-blockCount,hitCount) + ")");


        // XXXX
        String tranName = "Web Content Control";

        return summarizeEntries(tranName);
    }
}

