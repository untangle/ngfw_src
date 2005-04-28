/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ProtoFilterSummarizer.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.tran.reporting;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import org.apache.log4j.Logger;

public class LoginSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(LoginSummarizer.class);

    public LoginSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int succeededCount = 0;
        int failedCount = 0;

        try {
            String sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and succeeded";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            succeededCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from mvvm_login_evt where time_stamp >= ? and time_stamp < ? and not local and not succeeded";
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

        addEntry("Number of successful administrative logins", succeededCount);
        addEntry("Number of failed administrative logins", failedCount);

        // XXXX
        String hdrName = "Administrative Access";

        return summarizeEntries(hdrName);
    }
}

