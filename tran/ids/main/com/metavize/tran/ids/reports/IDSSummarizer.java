/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IDSSummarizer.java 2173 2005-08-18 02:22:16Z inieves $
 */

package com.metavize.tran.ids.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;

import org.apache.log4j.Logger;

public class IDSSummarizer extends BaseSummarizer {

    private static final Logger log = Logger.getLogger(IDSSummarizer.class);

    public IDSSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long dncEvtCount = 0;
        long loggedEvtCount = 0;
        long blockedEvtCount = 0;

        try {
            String sql = "SELECT SUM(dnc), SUM(logged), SUM(blocked) FROM tr_ids_statistic_evt WHERE time_stamp >= ? AND time_stamp < ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            ResultSet rs = ps.executeQuery();
            rs.first();
    
            dncEvtCount = rs.getLong(1);
            loggedEvtCount = rs.getLong(2);
            blockedEvtCount = rs.getLong(3);
    
            rs.close();
            ps.close();
    
        } catch (SQLException exn) {
            log.warn("could not summarize", exn);
        }

        long totalEvtCount = dncEvtCount + loggedEvtCount + blockedEvtCount;

        addEntry("Total Scan Events", Util.trimNumber("",totalEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Matched &amp; Logged", Util.trimNumber("",loggedEvtCount), Util.percentNumber(loggedEvtCount,totalEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Matched &amp; Blocked", Util.trimNumber("",blockedEvtCount), Util.percentNumber(blockedEvtCount,totalEvtCount));
        addEntry("&nbsp;&nbsp;&nbsp;Unmatched", Util.trimNumber("",dncEvtCount), Util.percentNumber(dncEvtCount,totalEvtCount));

        // XXXX
        String tranName = "Intrusion Prevention";

        return summarizeEntries(tranName);
    }
}
