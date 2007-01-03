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

package com.untangle.tran.boxbackup.reports;

import java.sql.*;

import com.untangle.mvvm.reporting.BaseSummarizer;
import com.untangle.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class BoxBackupSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public BoxBackupSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int successCnt = 0;
        int failureCnt = 0;

        try {
            String sql = "SELECT COUNT(*) FROM tr_boxbackup_evt WHERE time_stamp >= ? AND time_stamp < ? AND success";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            successCnt = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_boxbackup_evt WHERE time_stamp >= ? AND time_stamp < ? AND NOT success";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            failureCnt = rs.getInt(1);
            rs.close();
            ps.close();
        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Successful backups", Util.trimNumber("",(long)successCnt));
        addEntry("Failed backups", Util.trimNumber("",(long)failureCnt) );

        // XXXX
        String hdrName = "24-Hour Replacement Backups";

        return summarizeEntries(hdrName);
    }
}
