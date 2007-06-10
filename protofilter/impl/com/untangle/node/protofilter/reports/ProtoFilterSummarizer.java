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

package com.untangle.node.protofilter.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class ProtoFilterSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public ProtoFilterSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int logCount = 0;
        int blockCount = 0;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT COUNT(*) FROM n_protofilter_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            logCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM n_protofilter_evt WHERE time_stamp >= ? AND time_stamp < ? AND blocked";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Detected protocol sessions", Util.trimNumber("",logCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked sessions", Util.trimNumber("",blockCount), Util.percentNumber(blockCount,logCount));
        addEntry("&nbsp;&nbsp;&nbsp;Passed sessions", Util.trimNumber("",logCount - blockCount), Util.percentNumber(logCount-blockCount,logCount));

        // XXXX
        String tranName = "Protocol Control";

        return summarizeEntries(tranName);
    }
}
