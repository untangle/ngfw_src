/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: BlockerSummarizer.java,v 1.3 2005/03/15 04:18:54 amread Exp $
 */

package com.metavize.tran.httpblocker;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
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
            String sql = "select count(*) from tr_http_evt_req where time_stamp >= ? and time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            hitCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_httpblk_evt_blk where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select sum(c2p_bytes), sum(s2p_bytes), sum(p2c_bytes), sum(p2s_bytes) from tr_http_evt_req as h, pipeline_info as p where h.time_stamp >= ? and h.time_stamp < ? and h.session_id = p.session_id";
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

        addEntry("Number of web hits", hitCount);
        addEntry("Number of blocked web pages", blockCount);
        addEntry("Total bytes of web traffic", totalTraffic);

        // XXXX
        String tranName = "Web Content Control";

        return summarizeEntries(tranName);
    }
}

