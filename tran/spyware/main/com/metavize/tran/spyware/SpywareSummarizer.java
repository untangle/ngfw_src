/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareSummarizer.java,v 1.2 2005/03/19 23:04:20 amread Exp $
 */

package com.metavize.tran.spyware;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import org.apache.log4j.Logger;

public class SpywareSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(SpywareSummarizer.class);

    public SpywareSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        int accessCount = 0;
        int blockCount = 0;
        int cookieCount = 0;
        int axCount = 0;

        try {
            String sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
            accessCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ? and blocked = 't'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            blockCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_cookie where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieCount = rs.getInt(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_activex where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            axCount = rs.getInt(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

        addEntry("Number of potential spyware accesses detected", accessCount);
        addEntry("Number of potential spyware accesses blocked", blockCount);
        addEntry("Number of spyware cookies blocked", cookieCount);
        addEntry("Number of spyware ActiveX controls blocked", axCount);

        // XXXX
        String tranName = "Spyware Blocker";

        return summarizeEntries(tranName);
    }
}

