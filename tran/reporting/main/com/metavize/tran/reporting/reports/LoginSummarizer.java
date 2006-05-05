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

package com.metavize.tran.reporting.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class LoginSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(LoginSummarizer.class);

    public LoginSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int succeededCount = 0;
        int failedCount = 0;

        try {
            String sql = "SELECT COUNT(*) FROM mvvm_login_evt WHERE time_stamp >= ? AND time_stamp < ? AND NOT local AND succeeded";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            succeededCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM mvvm_login_evt WHERE time_stamp >= ? AND time_stamp < ? AND NOT local AND NOT succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failedCount = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Successful administrative logins", Util.trimNumber("",(long)succeededCount));
        addEntry("Failed administrative logins", Util.trimNumber("",(long)failedCount) );

        // XXXX
        String hdrName = "Administrative Access";

        return summarizeEntries(hdrName);
    }
}
