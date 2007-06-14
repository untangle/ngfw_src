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

package com.untangle.node.ips.reports;

import java.sql.*;

import com.untangle.uvm.reporting.BaseSummarizer;
import com.untangle.uvm.reporting.Util;
import org.apache.log4j.Logger;

public class IPSSummarizer extends BaseSummarizer {
    private final Logger log = Logger.getLogger(getClass());

    public IPSSummarizer() { }

    public String getSummaryHtml(Connection conn, Timestamp startDate, Timestamp endDate)
    {
        long dncEvtCount = 0;
        long loggedEvtCount = 0;
        long blockedEvtCount = 0;

        try {
            String sql = "SELECT SUM(dnc), SUM(logged), SUM(blocked) FROM n_ips_statistic_evt WHERE time_stamp >= ? AND time_stamp < ?";
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
