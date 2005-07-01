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

package com.metavize.tran.httpblocker;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class BlockerSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(BlockerSummarizer.class);

    public BlockerSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int hitCount = 0;
        int blockCount = 0;
        long totalTraffic = 0;

        try {
            String sql = "SELECT count(*) FROM tr_http_evt_req WHERE time_stamp >= ? AND time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            hitCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT count(*) FROM tr_httpblk_evt_blk WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
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

        addEntry("Total web hits", Util.trimNumber("",hitCount));
        addEntry("Total blocked web pages", Util.trimNumber("",blockCount));
        addEntry("Total web traffic", Util.trimNumber("Bytes",totalTraffic));

        // XXXX
        String tranName = "Web Content Control";

        return summarizeEntries(tranName);
    }
}

