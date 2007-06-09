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

package com.untangle.node.spyware.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class SpywareSummarizer extends BaseSummarizer {

    private final Logger logger = Logger.getLogger(getClass());

    public SpywareSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long cookieBlockCount = 0l;
        long activeXBlockCount = 0l;
        long urlBlockCount = 0l;
        long subnetLogCount = 0l; // # of logged subnet access events
        long passCount = 0l;

        try {
            String sql;
            PreparedStatement ps;
            ResultSet rs;

            sql = "SELECT SUM(cookie) FROM tr_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(activeX) FROM tr_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            activeXBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(url) FROM tr_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            urlBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(subnet_access) FROM tr_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            subnetLogCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(pass) FROM tr_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            passCount = rs.getLong(1);
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
        addEntry("&nbsp;", "&nbsp;");
        addEntry("Clean communications detected", Util.trimNumber("",passCount));
        // XXXX
        String tranName = "Spyware Blocker";

        return summarizeEntries(tranName);
    }
}
