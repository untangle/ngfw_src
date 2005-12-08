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

package com.metavize.tran.spyware.reports;

import java.sql.*;

import com.metavize.mvvm.reporting.BaseSummarizer;
import com.metavize.mvvm.reporting.Util;
import org.apache.log4j.Logger;

public class SpywareSummarizer extends BaseSummarizer {

    private static final Logger logger = Logger.getLogger(SpywareSummarizer.class);

    public SpywareSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long cookieBlockCount = 0l;
        long activeXBlockCount = 0l;
        long urlBlockCount = 0l;
        long subnetLogCount = 0l; // # of logged subnet access events

        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;

            sql = "SELECT COUNT(*) FROM tr_spyware_evt_cookie WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spyware_evt_activex WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            activeXBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spyware_evt_blacklist WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            urlBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT COUNT(*) FROM tr_spyware_evt_access WHERE time_stamp >= ? AND time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            subnetLogCount = rs.getLong(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	long totalCount = cookieBlockCount + activeXBlockCount + urlBlockCount + subnetLogCount;
	
        addEntry("Potential spyware communications detected", Util.trimNumber("",totalCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked cookies", Util.trimNumber("",cookieBlockCount), Util.percentNumber(cookieBlockCount,totalCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked activeX", Util.trimNumber("",activeXBlockCount), Util.percentNumber(activeXBlockCount,totalCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked URLs", Util.trimNumber("",urlBlockCount), Util.percentNumber(urlBlockCount,totalCount));
        addEntry("&nbsp;&nbsp;&nbsp;Logged subnet accesses", Util.trimNumber("",subnetLogCount), Util.percentNumber(subnetLogCount,totalCount));

        // XXXX
        String tranName = "Spyware Blocker";

        return summarizeEntries(tranName);
    }
}
