/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
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

            sql = "SELECT SUM(cookie) FROM n_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            cookieBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(activeX) FROM n_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            activeXBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(url) FROM n_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            urlBlockCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(subnet_access) FROM n_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
            ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, startDate);
            ps.setTimestamp(2, endDate);
            rs = ps.executeQuery();
            rs.first();
            subnetLogCount = rs.getLong(1);
            rs.close();
            ps.close();

            sql = "SELECT SUM(pass) FROM n_spyware_statistic_evt WHERE time_stamp >= ? and time_stamp < ?";
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
