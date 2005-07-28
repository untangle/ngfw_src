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
        long subnetBlockCount = 0l;
        long subnetPassCount = 0l;
        long activeXBlockCount = 0l;
        long urlBlockCount = 0l;

        try {
	    String sql;
	    PreparedStatement ps;
	    ResultSet rs;

            sql = "select count(*) from tr_spyware_evt_cookie where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ? and blocked = 't'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            subnetBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_access where time_stamp >= ? and time_stamp < ? and blocked = 'f'";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            subnetPassCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_activex where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            activeXBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "select count(*) from tr_spyware_evt_blacklist where time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            urlBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

        } catch (SQLException exn) {
            logger.warn("could not summarize", exn);
        }

	long totalCount = cookieBlockCount + subnetBlockCount + subnetPassCount + activeXBlockCount + urlBlockCount;
	
        addEntry("Potential spyware communications detected", Util.trimNumber("",totalCount));
        addEntry("&nbsp;&nbsp;&nbsp;Blocked cookies", Util.trimNumber("",cookieBlockCount) + " (" + Util.percentNumber(cookieBlockCount,totalCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked subnets", Util.trimNumber("",subnetBlockCount) + " (" + Util.percentNumber(subnetBlockCount,totalCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked activeX", Util.trimNumber("",activeXBlockCount) + " (" + Util.percentNumber(activeXBlockCount,totalCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Blocked URLs", Util.trimNumber("",urlBlockCount) + " (" + Util.percentNumber(urlBlockCount,totalCount)  + ")");
        addEntry("&nbsp;&nbsp;&nbsp;Passed subnets", Util.trimNumber("",subnetPassCount) + " (" + Util.percentNumber(subnetPassCount,totalCount)  + ")");


        // XXXX
        String tranName = "Spyware Blocker";

        return summarizeEntries(tranName);
    }
}

