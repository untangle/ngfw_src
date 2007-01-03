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

package com.untangle.tran.portal.reports;

import java.sql.*;

import com.untangle.mvvm.reporting.BaseSummarizer;
import com.untangle.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class PortalSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public PortalSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long successLoginCnt = 0;
        long failureLoginCnt = 0;
        long logoutCnt = 0;

        try {
            String sql = "SELECT COUNT(*) FROM portal_login_evt WHERE time_stamp >= ? AND time_stamp < ? AND succeeded";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            successLoginCnt = (long) rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM portal_login_evt WHERE time_stamp >= ? AND time_stamp < ? AND NOT succeeded";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failureLoginCnt = (long) rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM portal_logout_evt WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            logoutCnt = (long) rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        long totalLoginCnt = successLoginCnt + failureLoginCnt;
        addEntry("User logins", Util.trimNumber("", totalLoginCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Successful", Util.trimNumber("", successLoginCnt), Util.percentNumber(successLoginCnt, totalLoginCnt));
        addEntry("&nbsp;&nbsp;&nbsp;Failed", Util.trimNumber("", failureLoginCnt), Util.percentNumber(failureLoginCnt, totalLoginCnt));

        addEntry("&nbsp;","&nbsp;");

        addEntry("User logouts", Util.trimNumber("", logoutCnt));

        // XXXX
        String hdrName = "Remote Access Portal Logins and Logouts";

        return summarizeEntries(hdrName);
    }
}
