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

package com.untangle.node.sigma;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class SigmaSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public SigmaSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int logCount = 0;
        int blockCount = 0;

        try {
            String sql = "select count(*) from tr_sigma_evt where time_stamp >= ? and time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            logCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_sigma_evt where time_stamp >= ? and time_stamp < ? and blocked = 't'";
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

        addEntry("Detected sigma sessions", Util.trimNumber("",logCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked sessions", Util.trimNumber("",blockCount));
        addEntry("&nbsp;&nbsp;&nbsp;Passed sessions", Util.trimNumber("",logCount - blockCount));

        // XXXX
        String tranName = "Sigma";

        return summarizeEntries(tranName);
    }
}
